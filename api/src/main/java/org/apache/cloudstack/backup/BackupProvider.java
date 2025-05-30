//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.backup;

import java.util.List;
import java.util.Map;

import com.cloud.storage.Volume;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.snapshot.VMSnapshot;

public interface BackupProvider {

    String VM_WORK_JOB_HANDLER = BackupManager.class.getSimpleName();

    /**
     * Returns the unique name of the provider
     * @return returns provider name
     */
    String getName();

    /**
     * Returns description about the backup and recovery provider plugin
     * @return returns description
     */
    String getDescription();

    /**
     * Returns the list of existing backup policies on the provider
     * @return backup policies list
     */
    List<BackupOffering> listBackupOfferings(Long zoneId);

    /**
     * True if a backup offering exists on the backup provider
     */
    boolean isValidProviderOffering(Long zoneId, String uuid);

    /**
     * Assign a VM to a backup offering or policy
     * @param vm
     * @param backup
     * @param policy
     * @return
     */
    boolean assignVMToBackupOffering(VirtualMachine vm, BackupOffering backupOffering);

    /**
     * Removes a VM from a backup offering or policy
     * @param vm
     * @return
     */
    boolean removeVMFromBackupOffering(VirtualMachine vm, boolean removeBackups);

    /**
     * Whether the provide will delete backups on removal of VM from the offfering
     * @return boolean result
     */
    boolean willDeleteBackupsOnOfferingRemoval();

    /**
     * Starts and creates an adhoc backup process
     * for a previously registered VM backup
     *
     * @param vm VirtualMachine definition
     * @param quiesceVm whether to quiesce the VM or not.
     * @return
     */
    boolean takeBackup(VirtualMachine vm, boolean quiesceVm);

    /**
     * Delete an existing backup
     * @param backuo The backup to exclude
     * @param forced Indicates if backup will be force removed or not
     * @return
     */
    boolean deleteBackup(Backup backup, boolean forced);

    /**
     * Restore VM from backup
     */
    boolean restoreVMFromBackup(VirtualMachine vm, Backup backup);

    /**
     * Restore a volume from a backup
     */
    Pair<Boolean, String> restoreBackedUpVolume(Backup backup, String volumeUuid, String hostIp, String dataStoreUuid, Pair<String, VirtualMachine.State> vmNameAndState, VirtualMachine vm, Boolean startVm);

    /**
     * Returns backup metrics for a list of VMs in a zone
     * @param zoneId
     * @param vms
     * @return
     */
    Map<VirtualMachine, Backup.Metric> getBackupMetrics(Long zoneId, List<VirtualMachine> vms);

    /**
     * This method should reconcile and create backup entries for any backups created out-of-band
     * @param vm
     * @param metric
     */
    void syncBackups(VirtualMachine vm, Backup.Metric metric);

    /**
     * This method should be overwritten by any backup providers that want to schedule their backup jobs in the same queue as the VM jobs.
     * Otherwise, just use the takeBackup method.
     * */
    default Boolean orchestrateTakeBackup(Backup backup, boolean quiesceVm, boolean runningVm) {
        return null;
    }

    /**
     * This method should be overwritten by any backup providers that want to schedule their backup delete jobs in the same queue as the VM jobs.
     * Otherwise, just use the deleteBackup method.
     * */
    default Boolean orchestrateDeleteBackup(Backup backup, boolean forced) {
        return null;
    }

    /**
     * This method should be overwritten by any backup providers that want to schedule their backup restore jobs in the same queue as the VM jobs.
     * Otherwise, just use the restoreVMFromBackup method.
     * */
    default Boolean orchestrateRestoreVMFromBackup(Backup backup, VirtualMachine vm) {
        return null;
    }

    /**
     * This method should be overwritten by any backup providers that allow volume detach but need to prepare it beforehand.
     * */
    default void prepareVolumeForDetach(Volume volume, VirtualMachine virtualMachine) {
    }

    /**
     * This method should be overwritten by any backup providers that allow volume migration but need to prepare it beforehand.
     * */
    default void prepareVolumeForMigration(Volume volume, VirtualMachine virtualMachine) {
    }

    /**
     * This method should be overwritten by any backup providers that must update metadata regarding a volume after certain operations (such as after a volume migration).
     * */
    default void updateVolumeId(VirtualMachine virtualMachine, long oldVolumeId, long newVolumeId) {
    }

    /**
     * This method should be overwritten by any backup providers that are compatible with VM Snapshots but need to prepare the VM to be reverted.
     * Currently, the only strategy that calls this method is the {@code KvmFileBasedStorageVmSnapshotStrategy}.
     * */
    default void prepareVmForSnapshotRevert(VMSnapshot vmSnapshot, VirtualMachine virtualMachine) {
    }
}
