//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//
package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import org.apache.cloudstack.backup.RestoreKnibBackupCommand;
import org.apache.cloudstack.storage.to.BackupDeltaTO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.libvirt.LibvirtException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@ResourceWrapper(handles = RestoreKnibBackupCommand.class)
public class LibvirtRestoreKnibBackupCommandWrapper extends CommandWrapper<RestoreKnibBackupCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(RestoreKnibBackupCommand cmd, LibvirtComputingResource resource) {
        Set<Pair<BackupDeltaTO, VolumeObjectTO>> backupToAndVolumeObjectPairs = cmd.getBackupAndVolumePairs();
        Set<BackupDeltaTO> deltasToRemove = cmd.getDeltasToRemove();
        Set<String> secondaryStorageUrls = cmd.getSecondaryStorageUrls();

        List<KVMStoragePool> parentSecondaryStorages = new ArrayList<>();
        KVMStoragePool secondaryStorage = null;
        KVMStoragePoolManager storagePoolManager = resource.getStoragePoolMgr();

        try {
            secondaryStorage = storagePoolManager.getStoragePoolByURI(backupToAndVolumeObjectPairs.stream().findFirst().get().first().getDataStore().getUrl());
            parentSecondaryStorages = secondaryStorageUrls.stream().map(storagePoolManager::getStoragePoolByURI).collect(Collectors.toList());

            restoreVolumes(backupToAndVolumeObjectPairs, secondaryStorage, storagePoolManager, cmd.getWait() * 1000);

            deleteDeltas(deltasToRemove, storagePoolManager);

        } catch (LibvirtException | QemuImgException | IOException e) {
            return new Answer(cmd, e);
        } finally {
            if (secondaryStorage != null) {
                storagePoolManager.deleteStoragePool(secondaryStorage.getType(), secondaryStorage.getUuid());
            }
            for (KVMStoragePool storagePool : parentSecondaryStorages) {
                storagePoolManager.deleteStoragePool(storagePool.getType(), storagePool.getUuid());
            }
        }
        return new Answer(cmd);
    }

    private void restoreVolumes(Set<Pair<BackupDeltaTO, VolumeObjectTO>> backupToAndVolumeObjectPairs, KVMStoragePool secondaryStorage, KVMStoragePoolManager storagePoolManager,
            int timeoutInMillis)
            throws LibvirtException, QemuImgException {
        for (Pair<BackupDeltaTO, VolumeObjectTO> backupToVolumeToPair : backupToAndVolumeObjectPairs) {
            String fullBackupPath = secondaryStorage.getLocalPathFor(backupToVolumeToPair.first().getPath());

            VolumeObjectTO volumeObjectTO = backupToVolumeToPair.second();
            PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) volumeObjectTO.getDataStore();
            KVMStoragePool primaryStoragePool = storagePoolManager.getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());
            String fullVolumePath = primaryStoragePool.getLocalPathFor(volumeObjectTO.getPath());

            QemuImgFile backup = new QemuImgFile(fullBackupPath, QemuImg.PhysicalDiskFormat.QCOW2);
            QemuImgFile volume = new QemuImgFile(fullVolumePath, QemuImg.PhysicalDiskFormat.QCOW2);

            QemuImg qemuImg = new QemuImg(timeoutInMillis);

            logger.info("Restoring volume [{}] at [{}] with backup stored at [{}].", volumeObjectTO.getUuid(), fullVolumePath, fullBackupPath);
            qemuImg.convert(backup, volume);
        }
    }

    private void deleteDeltas(Set<BackupDeltaTO> deltasToRemove, KVMStoragePoolManager storagePoolManager) throws IOException {
        for (BackupDeltaTO deltaToRemove : deltasToRemove) {
            PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) deltaToRemove.getDataStore();
            KVMStoragePool primaryStoragePool = storagePoolManager.getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());
            String fullDeltaPath = primaryStoragePool.getLocalPathFor(deltaToRemove.getPath());
            logger.debug("Deleting leftover delta [{}].", fullDeltaPath);
            Files.deleteIfExists(Path.of(fullDeltaPath));
        }
    }
}
