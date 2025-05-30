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
import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.exception.BackupException;
import org.apache.cloudstack.backup.TakeKnibBackupAnswer;
import org.apache.cloudstack.storage.to.BackupDeltaTO;
import org.apache.cloudstack.storage.to.DeltaMergeTreeTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import org.apache.cloudstack.backup.TakeKnibBackupCommand;
import org.apache.cloudstack.storage.to.KnibTO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImageOptions;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.libvirt.LibvirtException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ResourceWrapper(handles = TakeKnibBackupCommand.class)
public class LibvirtTakeKnibBackupCommandWrapper extends CommandWrapper<TakeKnibBackupCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(TakeKnibBackupCommand command, LibvirtComputingResource resource) {
        String imageStoreUrl = command.getImageStoreUrl();
        String vmName = command.getVmName();
        logger.info("Starting backup process for VM [{}].", vmName);
        List<KnibTO> knibTOs = command.getKnibTOs();
        List<VolumeObjectTO> volumeObjectTOs = knibTOs.stream().map(KnibTO::getVolumeObjectTO).collect(Collectors.toList());

        Map<String, Pair<Long, String>> mapVolumeUuidToDeltaSizeAndNewVolumePath;
        Map<String, Pair<String, Long>> mapVolumeUuidToDeltaPathOnSecondaryAndDeltaSize = new HashMap<>();
        Map<String, String> mapVolumeUuidToNewVolumePath = new HashMap<>();

        KVMStoragePoolManager storagePoolManager = resource.getStoragePoolMgr();
        boolean runningVM = command.isRunningVM();

        try {
            if (runningVM) {
                mapVolumeUuidToDeltaSizeAndNewVolumePath = resource.createDiskOnlyVmSnapshotForRunningVm(volumeObjectTOs, vmName, UUID.randomUUID().toString(), command.isQuiesceVm());
            } else {
                mapVolumeUuidToDeltaSizeAndNewVolumePath = resource.createDiskOnlyVMSnapshotOfStoppedVm(volumeObjectTOs, vmName);
            }

            for (KnibTO knibTO : knibTOs) {
                VolumeObjectTO volumeObjectTO = knibTO.getVolumeObjectTO();
                String currentVolumePath = volumeObjectTO.getPath();
                String volumeUuid = volumeObjectTO.getUuid();
                List<String> snapshotDataStoreVos = knibTO.getVmSnapshotDeltaPaths();

                try {
                    Pair<String, Long> deltaPathOnSecondaryAndSize = copyBackupDeltaToSecondary(storagePoolManager, command.getBackupParentImageStoreUrl(), imageStoreUrl,
                            command.getWait(), knibTO);

                    mapVolumeUuidToDeltaPathOnSecondaryAndDeltaSize.put(volumeUuid, deltaPathOnSecondaryAndSize);
                } catch (Exception ex) {
                    recoverPreviousVmStateAndDeletePartialBackup(resource, volumeObjectTOs, mapVolumeUuidToDeltaSizeAndNewVolumePath, vmName, runningVM,
                            mapVolumeUuidToDeltaPathOnSecondaryAndDeltaSize, storagePoolManager, imageStoreUrl);
                    throw new BackupException(String.format("There was an exception during the backup process for VM [%s], but the VM has been successfully normalized.", vmName),
                            ex, true);
                }

                DeltaMergeTreeTO deltaMergeTreeTO = knibTO.getDeltaMergeTreeTO();
                volumeObjectTO.setPath(mapVolumeUuidToDeltaSizeAndNewVolumePath.get(volumeUuid).second());

                if (deltaMergeTreeTO != null) {
                    mergeBackupDelta(resource, deltaMergeTreeTO, volumeObjectTO, vmName, runningVM, volumeUuid, snapshotDataStoreVos.isEmpty());
                }

                if (command.isEndChain()) {
                    String baseVolumePath = currentVolumePath;
                    if (deltaMergeTreeTO != null && deltaMergeTreeTO.getChild().getPath().equals(baseVolumePath)) {
                        baseVolumePath = deltaMergeTreeTO.getParent().getPath();
                    }
                    endChainForVolume(resource, volumeObjectTO, vmName, runningVM, volumeUuid, baseVolumePath);
                    mapVolumeUuidToNewVolumePath.put(volumeUuid, baseVolumePath);
                } else {
                    mapVolumeUuidToNewVolumePath.put(volumeUuid, mapVolumeUuidToDeltaSizeAndNewVolumePath.get(volumeUuid).second());
                }
            }
        } catch (BackupException ex) {
            return new TakeKnibBackupAnswer(command, ex);
        }

        return new TakeKnibBackupAnswer(command, true, mapVolumeUuidToNewVolumePath, mapVolumeUuidToDeltaPathOnSecondaryAndDeltaSize);
    }

    /**
     * Copy the backup delta to the secondary storage. Since we created a snapshot on top of the volume, the volume is now the backup delta.
     * If there were snapshots created after the last backup, they'll be copied alongside and merged in the secondary storage.
     * */
    private Pair<String, Long> copyBackupDeltaToSecondary(KVMStoragePoolManager storagePoolManager, String backupParentImageStoreUrl, String imageStoreUrl, int wait, KnibTO knibTO) {
        VolumeObjectTO delta = knibTO.getVolumeObjectTO();
        String parentDeltaPathOnSecondary = knibTO.getPathBackupParentOnSecondary();
        List<String> deltaPathsToCopy = knibTO.getVmSnapshotDeltaPaths();
        deltaPathsToCopy.add(delta.getPath());

        KVMStoragePool parentImagePool = null;
        KVMStoragePool imagePool = null;
        long backupSize;
        final String backupOnSecondary = getRelativePathOnSecondaryForBackup(delta.getAccountId(), delta.getVolumeId(), UUID.randomUUID().toString());
        try {
            imagePool = storagePoolManager.getStoragePoolByURI(imageStoreUrl);
            if (backupParentImageStoreUrl != null) {
                parentImagePool = storagePoolManager.getStoragePoolByURI(backupParentImageStoreUrl);
            }

            PrimaryDataStoreTO primaryDataStoreTO = (PrimaryDataStoreTO) delta.getDataStore();
            KVMStoragePool primaryPool = storagePoolManager.getStoragePool(primaryDataStoreTO.getPoolType(), primaryDataStoreTO.getUuid());

            String topDelta = backupOnSecondary;
            ArrayList<String> temporaryDeltasToRemove = new ArrayList<>();
            while (!deltaPathsToCopy.isEmpty()) {
                String backupDeltaFullPathOnSecondary = imagePool.getLocalPathFor(topDelta);
                temporaryDeltasToRemove.add(backupDeltaFullPathOnSecondary);
                String parentBackupFullPath = null;

                if (parentDeltaPathOnSecondary != null) {
                    parentBackupFullPath = parentImagePool.getLocalPathFor(parentDeltaPathOnSecondary);
                }

                String backupDeltaFullPathOnPrimary = primaryPool.getLocalPathFor(deltaPathsToCopy.remove(0));
                convertDeltaToSecondary(backupDeltaFullPathOnPrimary, backupDeltaFullPathOnSecondary, parentBackupFullPath, delta.getUuid(), wait);

                if (!deltaPathsToCopy.isEmpty()) {
                    parentDeltaPathOnSecondary = topDelta;
                    topDelta = getRelativePathOnSecondaryForBackup(delta.getAccountId(), delta.getVolumeId(), UUID.randomUUID().toString());
                    parentImagePool = imagePool;
                }
            }
            // The base should not be removed
            temporaryDeltasToRemove.remove(0);

            String backupOnSecondaryFullPath = imagePool.getLocalPathFor(backupOnSecondary);

            commitTopDeltaOnBaseBackupOnSecondaryIfNeeded(wait, topDelta, backupOnSecondary, imagePool, backupOnSecondaryFullPath, temporaryDeltasToRemove);

            backupSize = Files.size(Path.of(backupOnSecondaryFullPath));
        } catch (LibvirtException | QemuImgException | IOException e) {
            logger.error("Exception while converting backup [{}] to secondary storage [{}] due to: [{}].", delta.getPath(), imagePool, e.getMessage(), e);
            throw new BackupException("Exception while converting backup to secondary storage.", e, true);
        } finally {
            if (parentImagePool != null) {
                storagePoolManager.deleteStoragePool(parentImagePool.getType(), parentImagePool.getUuid());
            }
            if (imagePool != null) {
                storagePoolManager.deleteStoragePool(imagePool.getType(), imagePool.getUuid());
            }
        }
        return new Pair<>(backupOnSecondary, backupSize);
    }

    private void commitTopDeltaOnBaseBackupOnSecondaryIfNeeded(int wait, String topDelta, String backupOnSecondary, KVMStoragePool imagePool, String backupOnSecondaryFullPath,
            List<String> temporaryDeltasToRemove)
            throws LibvirtException, QemuImgException {
        if (topDelta.equals(backupOnSecondary)) {
            return;
        }

        QemuImg qemuImg = new QemuImg(wait);
        QemuImgFile topDeltaImg = new QemuImgFile(imagePool.getLocalPathFor(topDelta), QemuImg.PhysicalDiskFormat.QCOW2);
        QemuImgFile baseDeltaImg = new QemuImgFile(backupOnSecondaryFullPath, QemuImg.PhysicalDiskFormat.QCOW2);

        logger.debug("Committing top delta [{}] on base delta [{}].", topDeltaImg, baseDeltaImg);
        qemuImg.commit(topDeltaImg, baseDeltaImg, true);

        logger.debug("Removing temporary deltas [{}].", temporaryDeltasToRemove);
        for (String delta : temporaryDeltasToRemove) {
            try {
                Files.deleteIfExists(Path.of(delta));
            } catch (IOException ex) {
                logger.error("Failed to remove temporary delta [{}]. Will not stop the backup process, but this should be investigated.", delta, ex);
            }
        }
    }

    private void convertDeltaToSecondary(String pathDeltaOnPrimary, String pathDeltaOnSecondary, String pathParentOnSecondary, String volumeUuid, int wait)
            throws QemuImgException, LibvirtException {
        QemuImgFile backupDestination = new QemuImgFile(pathDeltaOnSecondary, QemuImg.PhysicalDiskFormat.QCOW2);
        QemuImgFile backupOrigin = new QemuImgFile(pathDeltaOnPrimary, QemuImg.PhysicalDiskFormat.QCOW2);
        QemuImgFile parentBackup = null;

        if (pathParentOnSecondary != null) {
            parentBackup = new QemuImgFile(pathParentOnSecondary, QemuImg.PhysicalDiskFormat.QCOW2);
        }

        logger.debug("Converting delta [{}] to [{}] with {}", backupOrigin, backupDestination, parentBackup == null ? "no parent." : String.format("parent [%s].", parentBackup));

        createDirsIfNeeded(pathDeltaOnSecondary, volumeUuid);

        QemuImg qemuImg = new QemuImg(wait);
        qemuImg.convert(backupOrigin, backupDestination, parentBackup, null, null,  new QemuImageOptions(backupOrigin.getFormat(), backupOrigin.getFileName(), null), null,
                true, false);
    }


    private void endChainForVolume(LibvirtComputingResource resource, VolumeObjectTO volumeObjectTO, String vmName, boolean isVmRunning, String volumeUuid, String baseVolumePath)
            throws BackupException {

        BackupDeltaTO baseVolume = new BackupDeltaTO(volumeObjectTO.getDataStore(), Hypervisor.HypervisorType.KVM, baseVolumePath);
        DeltaMergeTreeTO deltaMergeTreeTO = new DeltaMergeTreeTO(volumeObjectTO, baseVolume, volumeObjectTO, new ArrayList<>());

        logger.debug("Ending backup chain for volume [{}], the next backup will be a full backup.", volumeObjectTO.getUuid());

        mergeBackupDelta(resource, deltaMergeTreeTO, volumeObjectTO, vmName, isVmRunning, volumeUuid, false);
    }

    /**
     * Tries to recover the previous state of the VM. Should only be called if an exception in the backup creation process happened.<br/>
     * For each volume, will:<br/>
     *  - Merge back any backup deltas created;
     *  - Remove the data backed up to the secondary storage;
     * */
    private void recoverPreviousVmStateAndDeletePartialBackup(LibvirtComputingResource resource, List<VolumeObjectTO> volumeObjectTos,
            Map<String, Pair<Long, String>> mapVolumeUuidToDeltaSizeAndNewVolumePath, String vmName, boolean runningVm,
            Map<String, Pair<String, Long>> mapVolumeUuidToDeltaPathOnSecondaryAndSize, KVMStoragePoolManager storagePoolManager, String imageStoreUrl) {
        logger.error("There has been an exception during the backup creation process. We will try to revert the VM [{}] to its previous state.", vmName);

        for (VolumeObjectTO volumeObjectTO : volumeObjectTos) {
            String volumeUuid = volumeObjectTO.getUuid();

            BackupDeltaTO oldDelta = new BackupDeltaTO(volumeObjectTO.getDataStore(), Hypervisor.HypervisorType.KVM, volumeObjectTO.getPath());
            volumeObjectTO.setPath(mapVolumeUuidToDeltaSizeAndNewVolumePath.get(volumeUuid).second());
            DeltaMergeTreeTO deltaMergeTreeTO = new DeltaMergeTreeTO(volumeObjectTO, oldDelta, volumeObjectTO, new ArrayList<>());

            mergeBackupDelta(resource, deltaMergeTreeTO, volumeObjectTO, vmName, runningVm, volumeUuid, false);

            Pair<String, Long> deltaPathOnSecondaryAndSize = mapVolumeUuidToDeltaPathOnSecondaryAndSize.get(volumeUuid);
            if (deltaPathOnSecondaryAndSize == null) {
                continue;
            }

            cleanupDeltaOnSecondary(storagePoolManager, imageStoreUrl, deltaPathOnSecondaryAndSize.first());
        }
    }

    private void cleanupDeltaOnSecondary(KVMStoragePoolManager storagePoolManager, String imageStoreUrl, String deltaPath) {
        KVMStoragePool imagePool = null;

        try {
            imagePool = storagePoolManager.getStoragePoolByURI(imageStoreUrl);
            String fullDeltaPath = imagePool.getLocalPathFor(deltaPath);

            logger.debug("Cleaning up delta at [{}] as part of the post backup error normalization effort.", fullDeltaPath);

            Files.deleteIfExists(Path.of(fullDeltaPath));
        } catch (IOException e) {
            logger.error("Exception while trying to cleanup delta at [{}].", deltaPath, e);
        } finally {
            if (imagePool != null) {
                storagePoolManager.deleteStoragePool(imagePool.getType(), imagePool.getUuid());
            }
        }
    }


    private void mergeBackupDelta(LibvirtComputingResource resource, DeltaMergeTreeTO deltaMergeTreeTO, VolumeObjectTO volumeObjectTO, String vmName, boolean isVmRunning,
            String volumeUuid, boolean countNewestDeltaAsGrandchild) throws BackupException {
        try {
            if (isVmRunning) {
                resource.mergeDeltaForRunningVm(deltaMergeTreeTO, vmName, volumeObjectTO);
            } else {
                if (countNewestDeltaAsGrandchild) {
                    deltaMergeTreeTO.addGrandChild(volumeObjectTO);
                }
                resource.mergeDeltaForStoppedVm(deltaMergeTreeTO);
            }
        } catch (LibvirtException | QemuImgException | IOException e) {
            logger.error("Exception while merging the last backup delta using delta merge tree [{}] for VM [{}] and volume [{}].", deltaMergeTreeTO, vmName, volumeUuid, e);
            throw new BackupException(String.format("Exception during backup wrap-up phase for VM [%s].", vmName), e, false);
        }
    }

    private String getRelativePathOnSecondaryForBackup(long accountId, long volumeId, String backupPath) {
        return String.format("%s%s%s%s%s%s%s", "backups", File.separator, accountId, File.separator, volumeId, File.separator, backupPath);
    }

    private void createDirsIfNeeded(String deltaFullPath, String volumeUuid) {
        String dirs = deltaFullPath.substring(0, deltaFullPath.lastIndexOf(File.separator));
        try {
            Files.createDirectories(Path.of(dirs));
        } catch (IOException e) {
            throw new BackupException(String.format("Error while creating directories for backup of volume [%s].", volumeUuid), e, true);
        }
    }

}
