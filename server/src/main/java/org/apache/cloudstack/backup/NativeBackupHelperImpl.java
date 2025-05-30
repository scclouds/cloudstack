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
package org.apache.cloudstack.backup;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.DataTO;
import org.apache.cloudstack.backup.dao.NativeBackupStoragePoolDao;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.RevertSnapshotCommand;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;

public class NativeBackupHelperImpl implements BackupHelper {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private NativeBackupStoragePoolDao nativeBackupStoragePoolDao;

    @Override
    public void configureChainInfo(DataTO volumeTo, Command cmd) {
        if (!(volumeTo instanceof VolumeObjectTO)) {
            return;
        }
        VolumeObjectTO volumeObjectTO = (VolumeObjectTO) volumeTo;
        NativeBackupStoragePoolVO backupDelta = nativeBackupStoragePoolDao.findOneByVolumeId(volumeObjectTO.getVolumeId());
        if (backupDelta == null) {
            return;
        }
        volumeObjectTO.setChainInfo(backupDelta.getBackupDeltaParentPath());
        if (cmd instanceof DeleteCommand) {
            ((DeleteCommand) cmd).setDeleteChain(true);
        }
        if (cmd instanceof RevertSnapshotCommand) {
            ((RevertSnapshotCommand) cmd).setDeleteChain(true);
        }
        logger.debug("Configured chain info for volume [{}]. Set it as [{}].", volumeObjectTO.getUuid(), volumeObjectTO.getChainInfo());
    }

    @Override
    public void cleanupBackupMetadata(long volumeId) {
        logger.debug("Cleaning up backup metadata for volume [{}].", volumeId);
        nativeBackupStoragePoolDao.expungeByVolumeId(volumeId);
    }

}
