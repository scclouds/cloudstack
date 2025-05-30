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

package org.apache.cloudstack.backup;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.storage.MergeDiskOnlyVmSnapshotCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.manager.Commands;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.UsageEventVO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeApiServiceImpl;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.Pair;
import com.cloud.utils.Predicate;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineManagerImpl;
import com.cloud.vm.VmWork;
import com.cloud.vm.VmWorkConstants;
import com.cloud.vm.VmWorkDeleteBackup;
import com.cloud.vm.VmWorkRestoreBackup;
import com.cloud.vm.VmWorkSerializer;
import com.cloud.vm.VmWorkTakeBackup;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotDetailsVO;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDetailsDao;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupDetailDao;
import org.apache.cloudstack.backup.dao.NativeBackupDataStoreDao;
import org.apache.cloudstack.backup.dao.NativeBackupJoinDao;
import org.apache.cloudstack.backup.dao.NativeBackupStoragePoolDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobExecutionContext;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.Outcome;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.framework.jobs.impl.OutcomeImpl;
import org.apache.cloudstack.framework.jobs.impl.VmWorkJobVO;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.secstorage.heuristics.HeuristicType;
import org.apache.cloudstack.storage.command.BackupDeleteAnswer;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.heuristics.HeuristicRuleHelper;
import org.apache.cloudstack.storage.to.BackupDeltaTO;
import org.apache.cloudstack.storage.to.DeltaMergeTreeTO;
import org.apache.cloudstack.storage.to.KnibTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.vmsnapshot.VMSnapshotHelper;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.cloudstack.backup.dao.BackupDetailDao.CURRENT;
import static org.apache.cloudstack.backup.dao.BackupDetailDao.END_OF_CHAIN;
import static org.apache.cloudstack.backup.dao.BackupDetailDao.IMAGE_STORE_ID;
import static org.apache.cloudstack.backup.dao.BackupDetailDao.PARENT_ID;

public class KnibBackupProvider extends AdapterBase implements BackupProvider, Configurable {

    protected ConfigKey<Integer> backupChainSize = new ConfigKey<>("Advanced", Integer.class, "backup.chain.size", "8", "Determines the max size of a backup chain." +
            " Currently only used by the KNIB provider. If cloud admins set it to 1 , all the backups will be full backups. With values lower than 1, the backup chain will be " +
            "unlimited, unless it is stopped by another process. Please note that unlimited backup chains have a higher chance of getting corrupted, as new backups will be" +
            " dependant on all of the older ones.", true, ConfigKey.Scope.Zone);

    @Inject
    private AsyncJobManager jobManager;
    @Inject
    private EntityManager entityManager;

    @Inject
    private VirtualMachineManager virtualMachineManager;

    @Inject
    private UserVmDao userVmDao;

    @Inject
    private VMSnapshotHelper vmSnapshotHelper;

    @Inject
    private SnapshotDataStoreDao snapshotDataStoreDao;

    @Inject
    private VMSnapshotDao vmSnapshotDao;

    @Inject
    private VMSnapshotDetailsDao vmSnapshotDetailsDao;

    @Inject
    private BackupDao backupDao;

    @Inject
    private NativeBackupJoinDao nativeBackupJoinDao;

    @Inject
    private BackupDetailDao backupDetailDao;

    @Inject
    private NativeBackupStoragePoolDao nativeBackupStoragePoolDao;

    @Inject
    private NativeBackupDataStoreDao nativeBackupDataStoreDao;

    @Inject
    private HeuristicRuleHelper heuristicRuleHelper;

    @Inject
    private DataStoreManager dataStoreManager;

    @Inject
    private ImageStoreDao dataStoreDao;

    @Inject
    private AgentManager agentManager;

    @Inject
    private EndPointSelector endPointSelector;

    @Inject
    private VolumeDao volumeDao;

    @Inject
    private ImageStoreDao imageStoreDao;

    @Inject
    private VolumeApiService volumeApiService;

    @Inject
    private PrimaryDataStoreDao storagePoolDao;

    protected static final String STANDARD_BACKUP_UUID = "SB";
    protected static final String COMPRESSED_BACKUP_UUID = "CB";
    protected static final String VALIDATING_BACKUP_UUID = "VB";
    protected static final String COMPRESSED_VALIDATING_BACKUP_UUID = "CVB";

    private static final String KNIB_PROVIDER_NAME = "knib";

    // Return only the standard until the others are implemented
    List<BackupOffering> backupOfferings = Arrays.asList(
            new KnibBackupOffering("Standard", STANDARD_BACKUP_UUID, "Standard backup offering.")
            //new KnibBackupOffering("Compressed", COMPRESSED_BACKUP_UUID, "Compressed backup offering."),
            //new KnibBackupOffering("Validating", VALIDATING_BACKUP_UUID, "Validating backup offering."),
            //new KnibBackupOffering("Compressed Validating", COMPRESSED_VALIDATING_BACKUP_UUID, "Compressed and validating backup offering.")
    );

    private final List<Storage.StoragePoolType> supportedStoragePoolTypes = List.of(Storage.StoragePoolType.Filesystem, Storage.StoragePoolType.NetworkFilesystem,
            Storage.StoragePoolType.SharedMountPoint);

    private final List<Backup.Status> allowedBackupStatesToRemove = List.of(Backup.Status.BackedUp, Backup.Status.Failed, Backup.Status.Error);

    private final List<VirtualMachine.State> allowedVmStates = Arrays.asList(VirtualMachine.State.Running, VirtualMachine.State.Stopped);
    @Override
    public String getDescription() {
        return "Native Incremental KVM Backup Plugin";
    }

    @Override
    public List<BackupOffering> listBackupOfferings(Long zoneId) {
        return backupOfferings;
    }

    @Override
    public boolean isValidProviderOffering(Long zoneId, String uuid) {
        return backupOfferings.stream().anyMatch(backupOffering -> backupOffering.getExternalId().equals(uuid));
    }

    @Override
    public boolean assignVMToBackupOffering(VirtualMachine vm, BackupOffering backupOffering) {
        logger.debug("Assigning VM [{}] to KNIB backup offering with name:[{}], uuid: [{}].", vm.getUuid(), backupOffering.getName(), backupOffering.getUuid());
        if (!Hypervisor.HypervisorType.KVM.equals(vm.getHypervisorType())) {
            logger.error("KVM Native Incremental Backup provider is only supported for KVM.");
            return false;
        }

        for (VolumeVO volume : volumeDao.findByInstance(vm.getId())) {
            if (CollectionUtils.isNotEmpty(snapshotDataStoreDao.listReadyByVolumeIdAndCheckpointPathNotNull(volume.getId()))) {
                logger.error("KNIB is not compatible with incremental volume snapshots.");
                return false;
            }
        }

        for (VMSnapshotVO vmSnapshotVO : vmSnapshotDao.findByVmAndByType(vm.getId(), VMSnapshot.Type.Disk)) {
            List<VMSnapshotDetailsVO> vmSnapshotDetails = vmSnapshotDetailsDao.listDetails(vmSnapshotVO.getId());
            if (!vmSnapshotDetails.stream().allMatch(vmSnapshotDetailsVO -> vmSnapshotDetailsVO.getName().equals(VolumeApiServiceImpl.KVM_FILE_BASED_STORAGE_SNAPSHOT))) {
                logger.error("KNIB is only supported with disk-only VM snapshots using [{}] strategy. Found a disk-only VM snapshot using another strategy for the VM.",
                        VolumeApiServiceImpl.KVM_FILE_BASED_STORAGE_SNAPSHOT);
                logger.debug("Found VM snapshot details [{}].", () -> vmSnapshotDetails.stream().map(VMSnapshotDetailsVO::getName).collect(Collectors.toList()));
                return false;
            }
        }

        return CollectionUtils.isEmpty(vmSnapshotDao.findByVmAndByType(vm.getId(), VMSnapshot.Type.DiskAndMemory));
    }

    @Override
    public boolean removeVMFromBackupOffering(VirtualMachine vm, boolean removeBackups) {
        logger.info("Removing VM [{}] from KNIB backup offering.", vm.getUuid());

        NativeBackupJoinVO current = nativeBackupJoinDao.findCurrent(vm.getId());
        if (current == null) {
            logger.debug("There is no current active chain, no need to do anything.");
            return true;
        }

        BackupVO backupVO = backupDao.findById(current.getId());
        validateVmState(vm, "remove backup offering", VirtualMachine.State.Expunging, VirtualMachine.State.Destroyed);
        if (mergeCurrentBackupDeltas(current, backupVO)) {
            backupDetailDao.removeDetail(current.getId(), CURRENT);
            return true;
        }
        return false;
    }

    @Override
    public boolean willDeleteBackupsOnOfferingRemoval() {
        return false;
    }

    @Override
    public boolean takeBackup(VirtualMachine vm, boolean quiesceVm) {
        logger.debug("Queueing backup on VM [{}].", vm.getUuid());
        Outcome<Boolean> outcome = createBackupThroughJobQueue(vm, quiesceVm);

        try {
            outcome.get();
        } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
            throw new CloudRuntimeException(String.format("Unable to retrieve result from job takeBackup due to [%s]. VM [%s].", e.getMessage(), vm.getUuid()), e);
        }

        Object jobResult = jobManager.unmarshallResultObject(outcome.getJob());

        if (jobResult instanceof Throwable) {
            throw new CloudRuntimeException(String.format("Exception while taking KVM native incremental backup for VM [%s]. Check the logs for more information.", vm.getUuid()));
        }

        return BooleanUtils.isTrue((Boolean) jobResult);
    }

    @Override
    public Boolean orchestrateTakeBackup(Backup backup, boolean quiesceVm, boolean runningVm) {
        BackupVO backupVO = (BackupVO) backup;
        long vmId = backup.getVmId();
        VirtualMachine userVm = virtualMachineManager.findById(vmId);

        validateVmState(userVm, "take backup");
        List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(userVm.getId());
        validateStorages(volumeTOs, userVm.getUuid());

        logger.info("Starting VM backup process for VM [{}].", userVm.getUuid());

        Long hostId = vmSnapshotHelper.pickRunningHost(vmId);

        List<NativeBackupJoinVO> backupChain = getBackupJoinParents(backupVO, true);
        NativeBackupJoinVO parentBackup = getParentAndSetEndOfChain(backupVO, backupChain);
        NativeBackupJoinVO newBackupJoin = nativeBackupJoinDao.findById(backup.getId());
        boolean fullBackup = parentBackup == null;
        List<NativeBackupStoragePoolVO> parentBackupDeltasOnPrimary = new ArrayList<>();
        List<NativeBackupDataStoreVO> parentBackupDeltasOnSecondary = new ArrayList<>();
        String parentImageStoreUrl = null;
        List<KnibTO> knibTOs = new ArrayList<>();
        HashMap<String, NativeBackupStoragePoolVO> volumeUuidToDeltaPrimaryRef = new HashMap<>();
        HashMap<String, NativeBackupDataStoreVO> volumeUuidToDeltaSecondaryRef = new HashMap<>();

        if (!fullBackup) {
            parentBackupDeltasOnPrimary = nativeBackupStoragePoolDao.listByBackupId(parentBackup.getId());
            parentBackupDeltasOnSecondary = nativeBackupDataStoreDao.listByBackupId(parentBackup.getId());
            parentImageStoreUrl = dataStoreDao.findById(parentBackup.getImageStoreId()).getUrl();
        }

        transitStateWithoutThrow(userVm, VirtualMachine.Event.BackupRequested, hostId);
        updateBackupStatusToBackingUp(volumeTOs, backupVO);

        DataStore imageStore = getImageStoreForBackup(userVm.getDataCenterId(), backupVO);
        createDetails(imageStore.getId(), fullBackup ? 0L : parentBackup.getId(), backupVO);

        List<VMSnapshotVO> succeedingVmSnapshotList = getSucceedingVmSnapshotList(parentBackup);
        VMSnapshotVO succeedingVmSnapshot = succeedingVmSnapshotList.isEmpty() ? null : succeedingVmSnapshotList.get(0);

        Map<Long, List<SnapshotDataStoreVO>> volumeIdToSnapshotDataStoreList = mapVolumesToVmSnapshotReferences(volumeTOs, succeedingVmSnapshotList);

        for (VolumeObjectTO volumeObjectTO : volumeTOs) {
            KnibTO knibTO = new KnibTO(volumeObjectTO, volumeIdToSnapshotDataStoreList.getOrDefault(volumeObjectTO.getId(), new ArrayList<>()));
            knibTOs.add(knibTO);
            createDeltaReferences(fullBackup, newBackupJoin.getEndOfChain(), !succeedingVmSnapshotList.isEmpty(), runningVm, backup, parentBackupDeltasOnSecondary,
                    parentBackupDeltasOnPrimary, volumeUuidToDeltaPrimaryRef, volumeUuidToDeltaSecondaryRef, succeedingVmSnapshot, knibTO);
        }

        TakeKnibBackupCommand command = new TakeKnibBackupCommand(quiesceVm, runningVm, newBackupJoin.getEndOfChain(), userVm.getInstanceName(), imageStore.getUri(),
                parentImageStoreUrl, knibTOs);

        Answer answer = agentManager.easySend(hostId, command);

        if (!answer.getResult()) {
            processBackupFailure(answer, userVm, hostId, runningVm, backupVO);
            return false;
        }

        processBackupSuccess(runningVm, volumeTOs, volumeUuidToDeltaPrimaryRef, volumeUuidToDeltaSecondaryRef, (TakeKnibBackupAnswer)answer, parentBackupDeltasOnPrimary,
                succeedingVmSnapshotList, backupVO, fullBackup, userVm, hostId);

        updateCurrentBackup(newBackupJoin);

        return true;
    }

    @Override
    public boolean deleteBackup(Backup backup, boolean forced) {
        logger.debug("Queueing backup [{}] deletion.", backup.getUuid());
        Outcome<Boolean> outcome = deleteBackupThroughJobQueue(backup, forced);

        try {
            outcome.get();
        } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
            throw new CloudRuntimeException(String.format("Unable to retrieve result from job deleteBackup due to [%s]. Backup [%s].", e.getMessage(), backup.getUuid()), e);
        }

        Object jobResult = jobManager.unmarshallResultObject(outcome.getJob());

        if (jobResult instanceof Throwable) {
            throw new CloudRuntimeException(String.format("Exception while deleting KVM native incremental backup [%s]. Check the logs for more information.", backup.getUuid()));
        }

        return BooleanUtils.isTrue((Boolean) jobResult);
    }

    @Override
    public Boolean orchestrateDeleteBackup(Backup backup, boolean forced) {
        BackupVO backupVO = (BackupVO) backup;

        VirtualMachine virtualMachine = virtualMachineManager.findById(backup.getVmId());

        if (virtualMachine != null) {
            validateVmState(virtualMachine, "delete backup");
        }

        logger.info("Starting delete process for backup [{}].", backupVO);

        if (!validateBackupState(backupVO)) {
            return false;
        }

        checkErrorBackup(backupVO, virtualMachine);
        if (deleteFailedBackup(backupVO)) {
            return true;
        }

        NativeBackupJoinVO childBackup = nativeBackupJoinDao.findByParentId(backup.getId());

        if (childBackup != null && !Backup.Status.Expunged.equals(childBackup.getStatus())) {
            logger.debug("Backup [{}] has children that are not expunged, will mark it as removed on the database but the files will not be deleted from secondary storage " +
                    "until the children are also expunged.");
            backupVO.setStatus(Backup.Status.Removed);
            backupDao.update(backupVO.getId(), backupVO);
            return true;
        }

        NativeBackupJoinVO backupJoinVO = nativeBackupJoinDao.findById(backup.getId());
        if (backupJoinVO.getCurrent()) {
            if (!mergeCurrentBackupDeltas(backupJoinVO, backupVO)) {
                return false;
            }
        }

        Commands deleteCommands = new Commands(Command.OnError.Continue);

        DataStore dataStore = addBackupDeltasToDeleteCommand(backup.getId(), deleteCommands);
        List<NativeBackupJoinVO> backupParentsToBeRemoved = getParentsToBeExpungedWithBackupAndAddThemToListOfDeleteCommands(backupVO, deleteCommands);

        EndPoint endPoint = endPointSelector.select(dataStore);
        if (endPoint == null) {
            logger.error("Unable to find SSVM to delete backup [{}]. Check if SSVM is up for the zone.", backup);
            throw new CloudRuntimeException(String.format("Unable to delete backup [%s]. Please check the logs.", backup.getUuid()));
        }
        Answer[] deleteAnswers;
        try {
            deleteAnswers = agentManager.send(endPoint.getId(), deleteCommands);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException(e);
        }

        List<Long> removedBackupIds = backupParentsToBeRemoved.stream().map(NativeBackupJoinVO::getId).collect(Collectors.toList());
        removedBackupIds.add(backup.getId());

        boolean isFailedSetEmpty = processRemoveBackupFailures(forced, deleteAnswers, removedBackupIds, backupJoinVO);

        processRemovedBackups(removedBackupIds);

        return isFailedSetEmpty;
    }

    private boolean validateBackupState(BackupVO backupVO) {
        if (!allowedBackupStatesToRemove.contains(backupVO.getStatus())) {
            logger.error("Backup [{}] is not in a state allowed to be removed. Current state is [{}]; allowed states are [{}]", backupVO, backupVO.getStatus(),
                    allowedBackupStatesToRemove);
            return false;
        }
        return true;
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, Backup backup) {
        logger.debug("Queueing backup [{}] restore for VM [{}].", backup.getUuid(), vm.getUuid());
        Outcome<Boolean> outcome = restoreVMFromBackupThroughJobQueue(vm, backup);

        try {
            outcome.get();
        } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
            throw new CloudRuntimeException(String.format("Unable to retrieve result from job restoreVMFromBackup due to [%s]. Backup [%s].", e.getMessage(), backup.getUuid()), e);
        }

        Object jobResult = jobManager.unmarshallResultObject(outcome.getJob());

        if (jobResult instanceof Throwable) {
            throw new CloudRuntimeException(String.format("Exception while restoring KVM native incremental backup [%s]. Check the logs for more information.", backup.getUuid()));
        }

        return BooleanUtils.isTrue((Boolean) jobResult);
    }

    @Override
    public Boolean orchestrateRestoreVMFromBackup(Backup backup, VirtualMachine vm) {
        logger.info("Starting restore backup process for VM [{}] and backup [{}].", vm.getUuid(), backup);
        validateNoVmSnapshots(vm);

        BackupVO backupVO = (BackupVO) backup;
        NativeBackupJoinVO backupJoinVO = nativeBackupJoinDao.findById(backup.getId());
        NativeBackupJoinVO currentBackup = nativeBackupJoinDao.findCurrent(vm.getId());
        List<NativeBackupStoragePoolVO> deltasOnPrimary = new ArrayList<>();
        if (currentBackup != null) {
            deltasOnPrimary = nativeBackupStoragePoolDao.listByBackupId(currentBackup.getId());
        }
        List<NativeBackupDataStoreVO> deltasOnSecondary = nativeBackupDataStoreDao.listByBackupId(backup.getId());
        List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(vm.getId());

        Set<BackupDeltaTO> deltasToRemove = new HashSet<>();

        List<NativeBackupDataStoreVO> backupsWithoutVolumes = getBackupsWithoutVolumes(deltasOnSecondary, volumeTOs);
        createAndAttachVolumes(backupsWithoutVolumes, vm);
        // Get new volume references
        volumeTOs = vmSnapshotHelper.getVolumeTOList(vm.getId());

        Set<Pair<BackupDeltaTO, VolumeObjectTO>> backupAndVolumePairs = generateBackupAndVolumePairsToRestore(deltasOnSecondary, volumeTOs, backupJoinVO);
        List<VolumeObjectTO> volumesWithoutBackups = getVolumesWithoutBackups(volumeTOs, deltasOnSecondary);
        List<DeltaMergeTreeTO> deltasToBeMerged = populateDeltasToRemoveAndToMergeAndUpdateVolumePaths(deltasOnPrimary, deltasToRemove, volumeTOs, volumesWithoutBackups, vm.getUuid());

        List<NativeBackupJoinVO> parentBackups = getBackupJoinParents(backupVO, true);
        Set<Long> secondaryStorageIds = parentBackups.stream().map(NativeBackupJoinVO::getImageStoreId).collect(Collectors.toSet());
        Set<String> secondaryStorageUrls = secondaryStorageIds.stream().map(id -> imageStoreDao.findById(id).getUrl()).collect(Collectors.toSet());

        Commands commands = new Commands(Command.OnError.Stop);
        commands.addCommand(new RestoreKnibBackupCommand(deltasToRemove, backupAndVolumePairs, secondaryStorageUrls));
        commands.addCommand(new MergeDiskOnlyVmSnapshotCommand(deltasToBeMerged, vm.getState().equals(VirtualMachine.State.Running), vm.getInstanceName()));

        Long hostId = vmSnapshotHelper.pickRunningHost(vm.getId());
        Answer[] answers = null;

        try {
            answers = agentManager.send(hostId, commands);
        } catch (OperationTimedoutException | AgentUnavailableException e) {
            throw new RuntimeException(e);
        }

        if (answers == null) {
            logger.error("Failed to restore backup [{}] due to no answer from host.", backup);
            return false;
        }

        Optional<Answer> failed = Arrays.stream(answers).filter(answer -> !answer.getResult()).findFirst();
        if (failed.isPresent()) {
            logger.error("Failed to restore backup [{}] due to [{}].", backup, failed.get().getDetails());
            return false;
        }

        updateVolumePathsAndSizeIfNeeded(vm, volumeTOs, deltasToBeMerged, deltasOnSecondary);

        if (currentBackup != null) {
            nativeBackupStoragePoolDao.expungeByBackupId(currentBackup.getId());
            setEndOfChainAndRemoveCurrentForBackup(currentBackup);
        }

        return true;
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, String volumeUuid, String hostIp, String dataStoreUuid, Pair<String, VirtualMachine.State> vmNameAndState,
            VirtualMachine vm, Boolean startVm) {
        throw new UnsupportedServiceException("This provider still does not support volume restore, only full backup restore.");
    }

    @Override
    public void prepareVolumeForDetach(Volume volume, VirtualMachine virtualMachine) {
        logger.info("Preparing volume [{}] for detach.", volume.getUuid());
        mergeCurrentDeltaIntoVolume(volume, virtualMachine, "detach", virtualMachine.getState().equals(VirtualMachine.State.Running));
    }

    @Override
    public void prepareVolumeForMigration(Volume volume, VirtualMachine vm) {
        if (VirtualMachine.State.Migrating.equals(vm.getState())) {
            logger.info("Preparing volume [{}] for live migration.", volume.getUuid());
            mergeCurrentDeltaIntoVolume(volume, vm, "live migration", true);
        }
    }

    @Override
    public void updateVolumeId(VirtualMachine virtualMachine, long oldVolumeId, long newVolumeId) {
        nativeBackupDataStoreDao.updateVolumeId(oldVolumeId, newVolumeId);
    }

    @Override
    public void prepareVmForSnapshotRevert(VMSnapshot vmSnapshot, VirtualMachine virtualMachine) {
        NativeBackupJoinVO currentBackup = nativeBackupJoinDao.findCurrent(virtualMachine.getId());

        if (currentBackup == null) {
            logger.debug("There is no current backup delta, the VM [{}] is already prepared for VM snapshot revert.", virtualMachine.getUuid());
            return;
        }
        if (currentBackup.getDate().before(vmSnapshot.getCreated())) {
            logger.debug("The current backup delta was taken before [{}] the VM snapshot being reverted [{}], no need to prepare the VM.", currentBackup.getDate(),
                    vmSnapshot.getCreated());
            return;
        }

        logger.debug("Preparing VM [{}] for VM snapshot reversion.", virtualMachine.getUuid());

        List<VolumeObjectTO> volumeObjectTOs = vmSnapshotHelper.getVolumeTOList(virtualMachine.getId());

        VMSnapshotVO vmSnapshotSucceedingCurrentBackup = getSucceedingVmSnapshot(currentBackup);

        List<DeltaMergeTreeTO> deltaMergeTreeTOList = new ArrayList<>();
        Commands commands = new Commands(Command.OnError.Stop);
        List<NativeBackupStoragePoolVO> deletedDeltas = new ArrayList<>();

        createDeleteCommandsAndMergeTrees(volumeObjectTOs, commands, deletedDeltas, vmSnapshotSucceedingCurrentBackup, deltaMergeTreeTOList);

        if (!deltaMergeTreeTOList.isEmpty()) {
            commands.addCommand(new MergeDiskOnlyVmSnapshotCommand(deltaMergeTreeTOList, false, virtualMachine.getInstanceName()));
        }

        Long hostId = vmSnapshotHelper.pickRunningHost(virtualMachine.getId());

        Answer[] answers;
        try {
            answers = agentManager.send(hostId, commands);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException(e);
        }

        if (answers == null || Arrays.stream(answers).anyMatch(answer -> !answer.getResult())) {
            logger.error("Error while trying to prepare VM [{}] for VM snapshot reversion. Got [{}] as answers from host.", virtualMachine.getUuid(),
                    answers != null ? Arrays.stream(answers).filter(answer -> !answer.getResult()).map(Answer::getDetails) : null);
            throw new CloudRuntimeException(String.format("Unable to prepare VM [%s] for VM snapshot reversion.", virtualMachine.getUuid()));
        }

        List<SnapshotDataStoreVO> snapRefsSucceedingCurrentBackup = new ArrayList<>();

        if (vmSnapshotSucceedingCurrentBackup != null) {
            snapRefsSucceedingCurrentBackup = vmSnapshotHelper.getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(vmSnapshotSucceedingCurrentBackup.getId());
        }

        updateReferencesAfterPrepareForSnapshotRevert(deltaMergeTreeTOList, snapRefsSucceedingCurrentBackup, deletedDeltas, currentBackup);
    }

    @Override
    public Map<VirtualMachine, Backup.Metric> getBackupMetrics(Long zoneId, List<VirtualMachine> vms) {
        return new HashMap<>();
    }

    @Override
    public void syncBackups(VirtualMachine vm, Backup.Metric metric) {
    }

    @Override
    public String getConfigComponentName() {
        return BackupService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {backupChainSize};
    }

    private Outcome<Boolean> createBackupThroughJobQueue(VirtualMachine vm, boolean quiesceVm) {
        final CallContext context = CallContext.current();
        long userId = context.getCallingUser().getId();
        long accountId = context.getCallingAccount().getAccountId();
        long vmId = vm.getId();
        VirtualMachine userVm = virtualMachineManager.findById(vmId);

        BackupVO backup = new BackupVO(vmId, vm.getBackupOfferingId(), accountId, vm.getDomainId(), vm.getDataCenterId(), 0, Backup.Status.Queued);

        VmWorkJobVO workJob = new VmWorkJobVO(AsyncJobExecutionContext.getOriginJobId(), userId, accountId, VmWorkTakeBackup.class.getName(), vmId, VirtualMachine.Type.Instance,
                VmWorkJobVO.Step.Starting);
        VmWorkTakeBackup workInfo = new VmWorkTakeBackup(userId, accountId, vmId, backupDao.persist(backup).getId(), VM_WORK_JOB_HANDLER, KNIB_PROVIDER_NAME, quiesceVm,
                userVm.getState().equals(VirtualMachine.State.Running));

        return submitWorkJob(workJob, workInfo, vmId);
    }

    private Outcome<Boolean> deleteBackupThroughJobQueue(Backup backup, boolean forced) {
        final CallContext context = CallContext.current();
        long userId = context.getCallingUser().getId();
        long accountId = context.getCallingAccount().getAccountId();
        VirtualMachine userVm = userVmDao.findByIdIncludingRemoved(backup.getVmId());
        long vmId = userVm.getId();

        VmWorkJobVO workJob = new VmWorkJobVO(AsyncJobExecutionContext.getOriginJobId(), userId, accountId, VmWorkDeleteBackup.class.getName(), vmId, VirtualMachine.Type.Instance,
                VmWorkJobVO.Step.Starting);
        VmWorkDeleteBackup workInfo = new VmWorkDeleteBackup(userId, accountId, vmId, VM_WORK_JOB_HANDLER, KNIB_PROVIDER_NAME, backup.getId(), forced);

        return submitWorkJob(workJob, workInfo, vmId);
    }

    private Outcome<Boolean> restoreVMFromBackupThroughJobQueue(VirtualMachine vm, Backup backup) {
        final CallContext context = CallContext.current();
        long userId = context.getCallingUser().getId();
        long accountId = context.getCallingAccount().getAccountId();
        long vmId = vm.getId();

        VmWorkJobVO workJob = new VmWorkJobVO(AsyncJobExecutionContext.getOriginJobId(), userId, accountId, VmWorkDeleteBackup.class.getName(), vmId, VirtualMachine.Type.Instance,
                VmWorkJobVO.Step.Starting);
        VmWorkRestoreBackup workInfo = new VmWorkRestoreBackup(userId, accountId, vmId, VM_WORK_JOB_HANDLER, KNIB_PROVIDER_NAME, backup.getId());

        return submitWorkJob(workJob, workInfo, vmId);
    }

    private OutcomeImpl<Boolean> submitWorkJob(VmWorkJobVO workJob, VmWork workInfo, long vmId) {
        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
        workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

        jobManager.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vmId);
        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new OutcomeImpl<>(Boolean.class, workJob, VirtualMachineManagerImpl.VmJobCheckInterval.value(), new Predicate() {
            @Override
            public boolean checkCondition() {
                AsyncJobVO jobVo = entityManager.findById(AsyncJobVO.class, workJob.getId());
                return jobVo == null || jobVo.getStatus() != JobInfo.Status.IN_PROGRESS;
            }
        }, AsyncJob.Topics.JOB_STATE);
    }

    /**
     * Validates the Backup status:<br/>
     * - If it is Error and The VM is in BackupError, will throw an exception;<br/>
     * - If it is in Error but the VM is not in BackupError, will set the backup as Failed so that it may be removed with {@code deleteFailedBackup(BackupVO backupVO)};<br/>
     * - If it is not in Error, does nothing.
     * */
    private void checkErrorBackup(BackupVO backupVO, VirtualMachine virtualMachine) {
        if (backupVO.getStatus() != Backup.Status.Error) {
            return;
        }
        if (virtualMachine != null && virtualMachine.getState() == VirtualMachine.State.BackupError) {
            logger.error("Unable to delete backup [{}] as it is in Error state and the associated VM [{}] is in BackupError state. You must read the backup creation logs," +
                    " normalize the VM's volumes in the hypervisor/storage and update the VM state in the database before trying to delete the backup. Try again when the VM is not " +
                    "in this state.", backupVO, virtualMachine.getUuid());
            throw new InvalidParameterValueException(String.format("Unable to delete backup [%s]. Please check the logs.", backupVO.getUuid()));
        }
        logger.debug("Assuming VM and storage are normalized and setting backup [{}] as failed so its metadata is deleted.");
        backupVO.setStatus(Backup.Status.Failed);
    }

    /**
     * Deletes a Failed backup metadata and sets the backup as Expunged.
     * */
    private boolean deleteFailedBackup(BackupVO backupVO) {
        if (backupVO.getStatus() == Backup.Status.Failed) {
            long backupId = backupVO.getId();

            backupVO.setStatus(Backup.Status.Expunged);
            backupDao.update(backupId, backupVO);
            nativeBackupStoragePoolDao.expungeByBackupId(backupId);
            nativeBackupDataStoreDao.expungeByBackupId(backupId);
            backupDetailDao.removeDetails(backupId);
            return true;
        }
        return false;
    }

    protected void mergeCurrentDeltaIntoVolume(Volume volume, VirtualMachine virtualMachine, String operation, boolean isVmRunning) {
        NativeBackupStoragePoolVO delta = nativeBackupStoragePoolDao.findOneByVolumeId(volume.getId());
        if (delta == null) {
            logger.debug("Volume [{}] has no deltas to merge, doing nothing.", volume.getUuid());
            return;
        }
        NativeBackupJoinVO nativeBackupJoinVO = nativeBackupJoinDao.findById(delta.getBackupId());
        VMSnapshotVO succeedingVmSnapshotVO = getSucceedingVmSnapshot(nativeBackupJoinVO);

        DataStore store = dataStoreManager.getDataStore(volume.getPoolId(), DataStoreRole.Primary);
        VolumeObject volumeObject = VolumeObject.getVolumeObject(store, (VolumeVO)volume);

        DeltaMergeTreeTO deltaMergeTreeTO = createDeltaMergeTree(succeedingVmSnapshotVO == null, isVmRunning, delta, (VolumeObjectTO)volumeObject.getTO(), succeedingVmSnapshotVO);
        MergeDiskOnlyVmSnapshotCommand cmd = new MergeDiskOnlyVmSnapshotCommand(List.of(deltaMergeTreeTO), isVmRunning, virtualMachine.getInstanceName());

        Answer answer = agentManager.easySend(vmSnapshotHelper.pickRunningHost(virtualMachine.getId()), cmd);

        if (answer == null || !answer.getResult()) {
            logger.error("Error while trying to prepare volume [{}] for {}. Got [{}] as answer from host.", volume.getUuid(), operation, answer != null ? answer.getDetails() : null);
            throw new CloudRuntimeException(String.format("Unable to prepare volume [%s] for [%s].", volume.getUuid(), operation));
        }

        if (succeedingVmSnapshotVO == null) {
            VolumeVO volumeVO = volumeDao.findById(volumeObject.getId());
            volumeVO.setPath(deltaMergeTreeTO.getParent().getPath());
            volumeDao.update(volumeVO.getId(), volumeVO);
        }

        expungeOldDeltasAndUpdateVmSnapshotIfNeeded(List.of(delta), succeedingVmSnapshotVO);

        List<NativeBackupStoragePoolVO> backupDeltas = nativeBackupStoragePoolDao.listByBackupId(delta.getBackupId());
        if (backupDeltas.isEmpty()) {
            backupDetailDao.removeDetail(delta.getBackupId(), CURRENT);
        }
    }

    /**
     * Creates the necessary delta references on both primary and secondary storage. Also maps the volume to the parent delta backup and create the delta merge tree.
     * */
    protected void createDeltaReferences(boolean fullBackup, boolean endOfChain, boolean hasVmSnapshotSucceedingLastBackup, boolean runningVm, Backup backup,
            List<NativeBackupDataStoreVO> parentBackupDeltasOnSecondary, List<NativeBackupStoragePoolVO> parentBackupDeltasOnPrimary,
            HashMap<String, NativeBackupStoragePoolVO> volumeUuidToDeltaPrimaryRef, HashMap<String, NativeBackupDataStoreVO> volumeUuidToDeltaSecondaryRef,
            VMSnapshotVO succeedingVmSnapshot, KnibTO knibTO) {
        VolumeObjectTO volumeObjectTO = knibTO.getVolumeObjectTO();
        logger.debug("Creating delta references for backup [{}] of volume [{}].", backup.getUuid(), volumeObjectTO.getUuid());

        NativeBackupDataStoreVO deltaSecondaryRef = new NativeBackupDataStoreVO(backup.getId(), volumeObjectTO.getVolumeId(), volumeObjectTO.getSize(), null);

        if (!fullBackup) {
            NativeBackupStoragePoolVO parentDeltaOnPrimary = createDeltaMergeTreeForVolume(false, runningVm, parentBackupDeltasOnPrimary, succeedingVmSnapshot, knibTO);
            findAndSetParentBackupPath(parentBackupDeltasOnSecondary, parentDeltaOnPrimary, knibTO);
        }

        NativeBackupDataStoreVO referenceOnSecondary = nativeBackupDataStoreDao.persist(deltaSecondaryRef);
        logger.trace("Created reference [{}] for backup [{}] of volume [{}].", referenceOnSecondary, backup, volumeObjectTO);
        volumeUuidToDeltaSecondaryRef.put(volumeObjectTO.getUuid(), referenceOnSecondary);

        if (endOfChain) {
            return;
        }

        NativeBackupStoragePoolVO deltaPrimaryRef = new NativeBackupStoragePoolVO(backup.getId(), volumeObjectTO.getPoolId(), volumeObjectTO.getVolumeId(), null,
                volumeObjectTO.getPath());

        if (knibTO.getDeltaMergeTreeTO() != null && !hasVmSnapshotSucceedingLastBackup) {
            deltaPrimaryRef.setBackupDeltaParentPath(knibTO.getDeltaMergeTreeTO().getParent().getPath());
        } else if (hasVmSnapshotSucceedingLastBackup) {
            deltaPrimaryRef.setBackupDeltaParentPath(volumeObjectTO.getPath());
        }

        NativeBackupStoragePoolVO referenceOnPrimary = nativeBackupStoragePoolDao.persist(deltaPrimaryRef);
        logger.trace("Created reference [{}] for backup [{}] of volume [{}].", referenceOnPrimary, backup, volumeObjectTO);
        volumeUuidToDeltaPrimaryRef.put(volumeObjectTO.getUuid(), referenceOnPrimary);
    }

    /**
     * Returns ordered list of disk-only VM snapshots taken after the last backup. The list is ordered from oldest to newest.
     * */
    protected List<VMSnapshotVO> getSucceedingVmSnapshotList(NativeBackupJoinVO backup) {
        List<VMSnapshotVO> vmSnapshotVOs = new ArrayList<>();
        if (backup == null) {
            return vmSnapshotVOs;
        }

        VMSnapshotVO currentSnapshotVO = vmSnapshotDao.findCurrentSnapshotByVmId(backup.getVmId());
        if (currentSnapshotVO == null || currentSnapshotVO.getCreated().before(backup.getDate())) {
            return vmSnapshotVOs;
        }
        vmSnapshotVOs.add(0, currentSnapshotVO);

        while (currentSnapshotVO.getParent() != null && currentSnapshotVO.getParent() != 0) {
            VMSnapshotVO parentSnap = vmSnapshotDao.findById(currentSnapshotVO.getParent());
            if (parentSnap.getCreated().before(backup.getDate())){
                break;
            }
            currentSnapshotVO = parentSnap;
            vmSnapshotVOs.add(0, currentSnapshotVO);
        }

        logger.debug("Found the following VM snapshots that succeed the backup [{}]: [{}].", backup.getUuid(), vmSnapshotVOs);

        return vmSnapshotVOs;
    }

    /**
     * Returns the disk-only VM snapshot taken after the last backup, if any.
     * */
    private VMSnapshotVO getSucceedingVmSnapshot(NativeBackupJoinVO backup) {
        List<VMSnapshotVO> snaps = getSucceedingVmSnapshotList(backup);
        if (snaps.isEmpty()) {
            return null;
        }
        return snaps.get(0);
    }

    /**
     * Given a VM snapshot, returns a map of volume id to list of snapshot references of the children of the VM snapshot.
     * */
    private Map<Long, List<SnapshotDataStoreVO>> gatherSnapshotReferencesOfChildrenSnapshot(List<VolumeObjectTO> volumeObjectTOs, VMSnapshot vmSnapshotVO) {
        Map<Long, List<SnapshotDataStoreVO>> volumeToSnapshotRefs = new HashMap<>();

        if (vmSnapshotVO == null) {
            return volumeToSnapshotRefs;
        }

        List<VMSnapshotVO> snapshotChildren = vmSnapshotDao.listByParent(vmSnapshotVO.getId());

        if (CollectionUtils.isEmpty(snapshotChildren)) {
            return volumeToSnapshotRefs;
        }

        List<SnapshotDataStoreVO> snapshotDataStoreVOS = new ArrayList<>();
        snapshotChildren.stream()
                .map(snapshotVo -> vmSnapshotHelper.getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(snapshotVo.getId()))
                .forEach(snapshotDataStoreVOS::addAll);

        mapVolumesToSnapshotReferences(volumeObjectTOs, snapshotDataStoreVOS, volumeToSnapshotRefs);

        if (logger.isDebugEnabled()) {
            StringBuilder log = new StringBuilder(String.format("Found the following snapshot references that succeed the VM snapshot [%s].", vmSnapshotVO.getUuid()));
            for (VolumeObjectTO volumeObjectTO : volumeObjectTOs) {
                log.append(String.format(" Volume [%s]; Snapshot references [%s].", volumeObjectTO.getUuid(), volumeToSnapshotRefs.get(volumeObjectTO.getId())));
            }
            logger.debug(log.toString());
        }

        return volumeToSnapshotRefs;
    }

    /**
     * Given a list of volumes and VM snapshots, maps the volumes to the snapshot references of the VM snapshots.
     * */
    protected Map<Long, List<SnapshotDataStoreVO>> mapVolumesToVmSnapshotReferences(List<VolumeObjectTO> volumeObjectTOs, List<VMSnapshotVO> vmSnapshotVOList) {
        Map<Long, List<SnapshotDataStoreVO>> volumeToSnapshotRefs = new HashMap<>();
        if (vmSnapshotVOList.isEmpty()) {
            logger.trace("No VM snapshot to map to any volume, returning.");
            return volumeToSnapshotRefs;
        }

        ArrayList<SnapshotDataStoreVO> allRefs = new ArrayList<>();
        for (VMSnapshotVO vmSnapshotVO : vmSnapshotVOList) {
            allRefs.addAll(vmSnapshotHelper.getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(vmSnapshotVO.getId()));
        }
        mapVolumesToSnapshotReferences(volumeObjectTOs, allRefs, volumeToSnapshotRefs);
        logger.trace("Given volume objects [{}] and VM snapshots [{}], created the following map [{}].", volumeObjectTOs, vmSnapshotVOList, volumeToSnapshotRefs);
        return volumeToSnapshotRefs;
    }

    protected void mapVolumesToSnapshotReferences(List<VolumeObjectTO> volumeObjectTOs, List<SnapshotDataStoreVO> snapshotDataStoreVOS, Map<Long, List<SnapshotDataStoreVO>> volumeToSnapshotRefs) {
        for (VolumeObjectTO volumeObjectTO : volumeObjectTOs) {
            List<SnapshotDataStoreVO> associatedSnapshots = snapshotDataStoreVOS.stream()
                    .filter(snapRef -> Objects.equals(snapRef.getVolumeId(), volumeObjectTO.getVolumeId()))
                    .collect(Collectors.toList());
            volumeToSnapshotRefs.put(volumeObjectTO.getId(), associatedSnapshots);
        }
    }

    /**
     * Updates the necessary references on the database. Also calculates the backup's physical size.
     * */
    private long updateDeltaReferencesAndCalculateBackupPhysicalSize(VolumeObjectTO volumeObjectTO, HashMap<String, NativeBackupStoragePoolVO> volumeUuidToDeltaPrimaryRef,
            HashMap<String, NativeBackupDataStoreVO> volumeUuidToDeltaSecondaryRef, TakeKnibBackupAnswer answer, long physicalBackupSize) {
        String volumeUuid = volumeObjectTO.getUuid();
        NativeBackupStoragePoolVO deltaPrimaryRef = volumeUuidToDeltaPrimaryRef.get(volumeUuid);
        NativeBackupDataStoreVO deltaSecondaryRef = volumeUuidToDeltaSecondaryRef.get(volumeUuid);

        String newVolumePath = answer.getMapVolumeUuidToNewVolumePath().get(volumeUuid);

        if (deltaPrimaryRef != null) {
            logger.trace("Updating delta reference on primary [{}] path to [{}].", deltaPrimaryRef, newVolumePath);
            deltaPrimaryRef.setBackupDeltaPath(newVolumePath);
            nativeBackupStoragePoolDao.update(deltaPrimaryRef.getId(), deltaPrimaryRef);
        }

        VolumeVO volumeVO = volumeDao.findById(volumeObjectTO.getId());
        volumeVO.setPath(newVolumePath);
        logger.trace("Updating volume [{}] path to [{}].", volumeVO.getUuid());
        volumeDao.update(volumeVO.getId(), volumeVO);

        Pair<String, Long> deltaPathOnSecondaryAndSize = answer.getMapVolumeUuidToDeltaPathOnSecondaryAndSize().get(volumeUuid);
        logger.trace("Updating delta reference on secondary [{}] path to [{}].", deltaSecondaryRef, deltaPathOnSecondaryAndSize.first());
        deltaSecondaryRef.setBackupPath(deltaPathOnSecondaryAndSize.first());
        nativeBackupDataStoreDao.update(deltaSecondaryRef.getId(), deltaSecondaryRef);

        physicalBackupSize += deltaPathOnSecondaryAndSize.second();
        return physicalBackupSize;
    }

    /**
     * Expunge the old backup deltas and if there were disk-only VM snapshot deltas after the last backup, update their paths.
     * */
    private void expungeOldDeltasAndUpdateVmSnapshotIfNeeded(List<NativeBackupStoragePoolVO> oldDeltasOnPrimary, VMSnapshot vmSnapshot) {
        List<SnapshotDataStoreVO> snapshotRefs = vmSnapshot == null ? List.of() : vmSnapshotHelper.getVolumeSnapshotsAssociatedWithKvmDiskOnlyVmSnapshot(vmSnapshot.getId());
        for (NativeBackupStoragePoolVO oldBackupDelta : oldDeltasOnPrimary) {
            logger.trace("Expunging old backup delta [{}].", oldBackupDelta);
            nativeBackupStoragePoolDao.expunge(oldBackupDelta.getId());
            SnapshotDataStoreVO snapshotDataStoreVO = snapshotRefs.stream().filter(ref -> ref.getVolumeId() == oldBackupDelta.getVolumeId()).findFirst().orElse(null);
            if (snapshotDataStoreVO == null) {
                return;
            }
            snapshotDataStoreVO.setInstallPath(oldBackupDelta.getBackupDeltaParentPath());
            logger.debug("Updating snapshot delta [{}] path to [{}].", snapshotDataStoreVO.getId(), oldBackupDelta.getBackupDeltaParentPath());
            snapshotDataStoreDao.update(snapshotDataStoreVO.getId(), snapshotDataStoreVO);
        }
    }

    /**
     * Create a {@link DeltaMergeTreeTO} for the volume if it has a delta on primary and add it to the list.
     *
     * @return the delta on primary of the volume. Null if no delta.
     * */
    protected NativeBackupStoragePoolVO createDeltaMergeTreeForVolume(boolean childIsVolume, boolean runningVm, List<NativeBackupStoragePoolVO> deltasOnPrimary, VMSnapshotVO succeedingVmSnapshot,
            KnibTO knibTO) {
        VolumeObjectTO volumeObjectTO = knibTO.getVolumeObjectTO();

        NativeBackupStoragePoolVO deltaOnPrimary = deltasOnPrimary.stream()
                .filter(delta -> delta.getVolumeId() == volumeObjectTO.getVolumeId())
                .findFirst()
                .orElse(null);
        if (deltaOnPrimary == null) {
            return null;
        }

        logger.debug("Volume [{}] has a backup delta on primary storage [{}].", volumeObjectTO.getUuid(), deltaOnPrimary);

        knibTO.setDeltaMergeTreeTO(createDeltaMergeTree(childIsVolume, runningVm, deltaOnPrimary, volumeObjectTO, succeedingVmSnapshot));
        return deltaOnPrimary;
    }

    private DeltaMergeTreeTO createDeltaMergeTree(boolean childIsVolume, boolean runningVm, NativeBackupStoragePoolVO deltaOnPrimary,
            VolumeObjectTO volumeObjectTO, VMSnapshotVO succeedingVmSnapshot) {
        DataStore store = dataStoreManager.getDataStore(deltaOnPrimary.getStoragePoolId(), DataStoreRole.Primary);
        DataTO deltaChild;
        if (childIsVolume) {
            deltaChild = volumeObjectTO;
        } else {
            deltaChild = new BackupDeltaTO(store.getTO(), Hypervisor.HypervisorType.KVM, deltaOnPrimary.getBackupDeltaPath());
        }

        BackupDeltaTO deltaParent = new BackupDeltaTO(store.getTO(), Hypervisor.HypervisorType.KVM, deltaOnPrimary.getBackupDeltaParentPath());

        List<String> succeedingDeltaPaths = new ArrayList<>();
        if (succeedingVmSnapshot != null) {
            succeedingDeltaPaths = gatherSnapshotReferencesOfChildrenSnapshot(List.of(volumeObjectTO), succeedingVmSnapshot).getOrDefault(volumeObjectTO.getVolumeId(), List.of())
                    .stream().map(SnapshotDataStoreVO::getInstallPath).collect(Collectors.toList());

            if (!childIsVolume && !runningVm && succeedingDeltaPaths.isEmpty()) {
                succeedingDeltaPaths = List.of(volumeObjectTO.getPath());
                logger.debug("Since the last backup delta of volume [{}] is succeeded by a snapshot and the delta created by this snapshot is also the volume, it will have to be" +
                        " rebased. Setting it as the grand-child.", volumeObjectTO.getUuid());
            }
        }



        List<DataTO> deltaGrandchildren = succeedingDeltaPaths.stream()
                    .map(deltaPath -> new BackupDeltaTO(store.getTO(), Hypervisor.HypervisorType.KVM, deltaPath))
                    .collect(Collectors.toList());

        DeltaMergeTreeTO deltaMergeTreeTO = new DeltaMergeTreeTO(volumeObjectTO, deltaParent, deltaChild, deltaGrandchildren);

        logger.debug("Mapped the following delta merge tree for volume [{}]: [{}].", volumeObjectTO.getUuid(), deltaMergeTreeTO);
        return deltaMergeTreeTO;
    }

    /**
     * Sets on the {@code knibTO} the backupParentOnSecondary path based on the list of NativeBackupDataStoreVO.
     *
     * @param parentBackupDeltasOnSecondary
     *         List of deltas on secondary;
     * @param parentDeltaOnPrimary
     * @param knibTO
     *         KnibTO to be configured;
     */
    protected void findAndSetParentBackupPath(List<NativeBackupDataStoreVO> parentBackupDeltasOnSecondary, NativeBackupStoragePoolVO parentDeltaOnPrimary, KnibTO knibTO) {
        VolumeObjectTO volumeObjectTO = knibTO.getVolumeObjectTO();
        if (parentDeltaOnPrimary == null) {
            logger.debug("Volume [{}] has no parent on primary, thus its backup cannot be incremental.", volumeObjectTO);
            return;
        }

        NativeBackupDataStoreVO parentOnSecondary = parentBackupDeltasOnSecondary.stream()
                .filter(backupDataStoreVo -> volumeObjectTO.getVolumeId() == backupDataStoreVo.getVolumeId())
                .findFirst()
                .orElse(null);

        if (parentOnSecondary == null) {
            return;
        }

        logger.debug("Volume [{}] already has a backup [{}].", volumeObjectTO.getUuid(), parentOnSecondary.getBackupId());

        knibTO.setPathBackupParentOnSecondary(parentOnSecondary.getBackupPath());
    }

    /**
     * Verify if the data center has heuristic rules for allocating backups; if there is then returns the {@link DataStore} returned by the JS script.
     * Otherwise, returns a {@link DataStore} with free capacity.
     */
    protected DataStore getImageStoreForBackup(Long dataCenterId, BackupVO backupVO) {
        DataStore imageStore = heuristicRuleHelper.getImageStoreIfThereIsHeuristicRule(dataCenterId, HeuristicType.BACKUP, backupVO);

        if (imageStore == null) {
            imageStore = dataStoreManager.getImageStoreWithFreeCapacity(dataCenterId);
        }

        if (imageStore == null) {
            backupVO.setStatus(Backup.Status.Failed);
            backupDao.update(backupVO.getId(), backupVO);
            throw new CloudRuntimeException(String.format("Unable to find secondary storage for backup [%s].", backupVO));
        }

        logger.debug("Backup [{}] will use secondary storage [{}].", backupVO.getUuid(), imageStore.getUuid());
        return imageStore;
    }

    /**
     * Gets the parent for newBackup <br/>
     * - If no backups are found, returns null. <br/>
     * - If the last backup was the end of the chain, returns null. <br/>
     *
     * @param newBackup the new backup being created.
     * @param backupChain newBackup's ancestors.
     * */
    protected NativeBackupJoinVO getParentAndSetEndOfChain(BackupVO newBackup, List<NativeBackupJoinVO> backupChain) {
        int remainingChainSize = backupChainSize.valueIn(newBackup.getZoneId());
        if (CollectionUtils.isEmpty(backupChain)) {
            setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(remainingChainSize, newBackup.getZoneId(), newBackup.getId(), newBackup.getUuid());
            return null;
        }

        remainingChainSize -= backupChain.size();

        NativeBackupJoinVO parent = backupChain.get(0);
        if (remainingChainSize < 1) {
            setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(remainingChainSize, newBackup.getZoneId(), parent.getId(), parent.getUuid());
            return null;
        }

        setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(remainingChainSize, newBackup.getZoneId(), newBackup.getId(), newBackup.getUuid());

        return parent.getStatus().equals(Backup.Status.BackedUp) ? parent : null;
    }

    /**
     * For every restore point, maps a volume to it.
     * @throws CloudRuntimeException If cannot map restore point to any volume.
     * */
    private Set<Pair<BackupDeltaTO, VolumeObjectTO>> generateBackupAndVolumePairsToRestore(List<NativeBackupDataStoreVO> backupVOs, List<VolumeObjectTO> volumeTOs, NativeBackupJoinVO backupJoinVO) {
        Set<Pair<BackupDeltaTO, VolumeObjectTO>> backupAndVolumePairs = new HashSet<>();
        for (NativeBackupDataStoreVO backupDataStoreVO : backupVOs) {
            VolumeObjectTO volumeObjectTO = volumeTOs.stream().filter(volumeTO -> volumeTO.getVolumeId() == backupDataStoreVO.getVolumeId())
                    .findFirst()
                    .orElse(null);

            if (volumeObjectTO == null) {
                logger.error("All backups should have a corresponding volume at this point, however, backup delta [{}] does not.", backupDataStoreVO.getId());
                throw new CloudRuntimeException("Error while restoring backup. Please check the logs.");
            }

            DataStore dataStore = dataStoreManager.getDataStore(backupJoinVO.getImageStoreId(), DataStoreRole.Image);
            backupAndVolumePairs.add(new Pair<>(new BackupDeltaTO(dataStore.getTO(), Hypervisor.HypervisorType.KVM, backupDataStoreVO.getBackupPath()), volumeObjectTO));
        }
        logger.debug("Generated the following list of pairs of backup deltas and volumes: [{}].", backupAndVolumePairs);
        return backupAndVolumePairs;
    }

    /**
     * For every volume, maps deltas that should be deleted, if there are any. If a volume has a delta but no backup, it will be mapped to be merged.
     *
     * @return List of deltas to be merged.
     * */
    private List<DeltaMergeTreeTO> populateDeltasToRemoveAndToMergeAndUpdateVolumePaths(List<NativeBackupStoragePoolVO> deltasOnPrimary, Set<BackupDeltaTO> deltasToRemove, List<VolumeObjectTO> volumeTOs,
            List<VolumeObjectTO> volumesWithoutBackups, String vmUuid) {
        List<DeltaMergeTreeTO> deltasToBeMerged = new ArrayList<>();
        for (NativeBackupStoragePoolVO deltaOnPrimary : deltasOnPrimary) {
            Optional<VolumeObjectTO> optional = volumeTOs.stream().filter(volumeTO -> volumeTO.getVolumeId() == deltaOnPrimary.getVolumeId()).findFirst();
            if (optional.isEmpty()) {
                logger.error("Failed to find volume that matches delta [{}] with path [{}]. Please check for inconsistencies on the database or if there are leftover" +
                        " deltas on storage.", deltaOnPrimary.getId(), deltaOnPrimary.getBackupDeltaPath());
                throw new CloudRuntimeException(String.format("Failed to restore VM [%s]. Please check the logs.", vmUuid));
            }
            VolumeObjectTO volumeObjectTO = optional.get();

            if (volumesWithoutBackups.contains(volumeObjectTO)) {
                deltasToBeMerged.add(createDeltaMergeTree(true, false, deltaOnPrimary, volumeObjectTO, null));
                continue;
            }

            DataStore dataStore = dataStoreManager.getDataStore(deltaOnPrimary.getStoragePoolId(), DataStoreRole.Primary);
            BackupDeltaTO backupDeltaTO = new BackupDeltaTO(dataStore.getTO(), Hypervisor.HypervisorType.KVM, deltaOnPrimary.getBackupDeltaPath());
            logger.debug("Mapped the following backup delta on primary to be removed since the volume [{}] is not part of the backup being restored [{}].",
                    volumeObjectTO.getUuid(), backupDeltaTO);
            deltasToRemove.add(backupDeltaTO);
            volumeObjectTO.setPath(deltaOnPrimary.getBackupDeltaParentPath());
        }
        if (!deltasToBeMerged.isEmpty()) {
            logger.debug("The following deltaMergeTrees [{}] were created to merge volumes [{}] that have no backups.", deltasToBeMerged, volumesWithoutBackups);
        }
        return deltasToBeMerged;
    }

    private void updateVolumePathsAndSizeIfNeeded(VirtualMachine vm, List<VolumeObjectTO> volumeTOs, List<DeltaMergeTreeTO> deltaMergeTreeTOList, List<NativeBackupDataStoreVO> deltasOnSecondary) {
        List<VolumeVO> volumeVOs = volumeDao.findByInstance(vm.getId());

        for (VolumeVO volumeVO : volumeVOs) {
            VolumeObjectTO volumeTO = volumeTOs.stream().filter(volumeObjectTO -> volumeObjectTO.getVolumeId() == volumeVO.getId()).findFirst().get();
            String log = "Volume [%s] path was updated as part of the backup restore process. New path: [%s].";

            DeltaMergeTreeTO deltaMergeTreeTO = deltaMergeTreeTOList.stream().filter(delta -> delta.getChild().getId() == volumeTO.getId()).findFirst().orElse(null);
            if (!volumeVO.getPath().equals(volumeTO.getPath())) {
                volumeVO.setPath(volumeTO.getPath());
                logger.debug(() -> String.format(log, volumeVO.getUuid(), volumeVO.getPath()));
            } else if (deltaMergeTreeTO != null) {
                volumeVO.setPath(deltaMergeTreeTO.getParent().getPath());
                logger.debug(() -> String.format(log, volumeVO.getUuid(), volumeVO.getPath()));
            }

            NativeBackupDataStoreVO deltaOnSec = deltasOnSecondary.stream().filter(delta -> delta.getVolumeId() == volumeVO.getId()).findFirst().orElse(null);
            if (deltaOnSec != null && deltaOnSec.getVolumeSize() != volumeVO.getSize()) {
                logger.debug("Volume [{}] size was restored as part of the backup restore process. Old size is [{}] new size is [{}].", volumeVO.getUuid(),
                        volumeVO.getSize(), deltaOnSec.getVolumeSize());
                volumeVO.setSize(deltaOnSec.getVolumeSize());
            }

            volumeDao.update(volumeVO.getId(), volumeVO);
        }
    }

    protected void createAndAttachVolumes(List<NativeBackupDataStoreVO> backups, VirtualMachine vm) {
        logger.info("Found the following backup deltas that have no volume correspondence [{}]. Will create new volumes and attach them to VM [{}].", backups.stream()
                .map(NativeBackupDataStoreVO::getId).collect(Collectors.toList()), vm.getUuid());
        for (NativeBackupDataStoreVO backup : backups) {
            VolumeVO volumeVO = duplicateVolume(backup);
            Volume volume = volumeApiService.attachVolumeToVM(vm.getId(), volumeVO.getId(), null, false, true);
            try {
                volumeApiService.stateTransitTo(volume, Volume.Event.RestoreRequested);
            } catch (NoTransitionException e) {
                throw new CloudRuntimeException(e);
            }
            backup.setVolumeId(volumeVO.getId());
            nativeBackupDataStoreDao.update(backup.getId(), backup);
        }
    }

    private VolumeVO duplicateVolume(NativeBackupDataStoreVO backup) {
        VolumeVO volumeVO = volumeDao.findByIdIncludingRemoved(backup.getVolumeId());
        VolumeVO duplicateVO = new VolumeVO(volumeVO);
        duplicateVO.setAttached(null);
        duplicateVO.setVolumeType(Volume.Type.DATADISK);
        duplicateVO.setInstanceId(null);
        duplicateVO.setPoolId(null);
        duplicateVO.setPath(null);
        return volumeDao.persist(duplicateVO);
    }

    protected List<NativeBackupDataStoreVO> getBackupsWithoutVolumes(List<NativeBackupDataStoreVO> backups, List<VolumeObjectTO> volumes) {
        List<NativeBackupDataStoreVO> deltasOnSecondaryWithNoVolumes = new ArrayList<>();
        for (NativeBackupDataStoreVO backup : backups) {
            VolumeObjectTO volumeObjectTO = volumes.stream().filter(volumeTO -> volumeTO.getVolumeId() == backup.getVolumeId())
                    .findFirst()
                    .orElse(null);

            if (volumeObjectTO == null) {
                deltasOnSecondaryWithNoVolumes.add(backup);
            }
        }
        return deltasOnSecondaryWithNoVolumes;
    }

    protected List<VolumeObjectTO> getVolumesWithoutBackups(List<VolumeObjectTO> volumeObjectTOS, List<NativeBackupDataStoreVO> deltasOnSecondary) {
        List<VolumeObjectTO> volumesWithNoBackups = new ArrayList<>();
        for (VolumeObjectTO volume : volumeObjectTOS) {
            if (deltasOnSecondary.stream().noneMatch(delta -> delta.getVolumeId() == volume.getVolumeId())) {
                volumesWithNoBackups.add(volume);
            }
        }
        logger.debug("Found the following volumes that are not part of the backup being restored [{}].", volumesWithNoBackups);
        return volumesWithNoBackups;
    }

    private void processBackupSuccess(boolean runningVm, List<VolumeObjectTO> volumeTOs, HashMap<String, NativeBackupStoragePoolVO> volumeUuidToDeltaPrimaryRef,
            HashMap<String, NativeBackupDataStoreVO> volumeUuidToDeltaSecondaryRef, TakeKnibBackupAnswer answer, List<NativeBackupStoragePoolVO> parentBackupDeltasOnPrimary,
            List<VMSnapshotVO> succeedingVmSnapshots, BackupVO backupVO, boolean fullBackup, VirtualMachine userVm, Long hostId) {
        long physicalBackupSize = 0;
        logger.debug("Processing backup [{}] success.", backupVO.getUuid());
        for (VolumeObjectTO volumeObjectTO : volumeTOs) {
            physicalBackupSize = updateDeltaReferencesAndCalculateBackupPhysicalSize(volumeObjectTO, volumeUuidToDeltaPrimaryRef, volumeUuidToDeltaSecondaryRef, answer, physicalBackupSize);
        }

        expungeOldDeltasAndUpdateVmSnapshotIfNeeded(parentBackupDeltasOnPrimary, succeedingVmSnapshots.isEmpty() ? null : succeedingVmSnapshots.get(0));

        backupVO.setSize(physicalBackupSize);
        backupVO.setStatus(Backup.Status.BackedUp);
        backupVO.setType(fullBackup ? "FULL" : "INCREMENTAL");
        backupDao.update(backupVO.getId(), backupVO);

        transitStateWithoutThrow(userVm, runningVm ? VirtualMachine.Event.BackupSucceededRunning : VirtualMachine.Event.BackupSucceededStopped, hostId);

        Map<String, String> details = new HashMap<>();
        details.put(UsageEventVO.DynamicParameters.vmId.name(), String.valueOf(userVm.getId()));
        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_CREATE, backupVO.getAccountId(), backupVO.getZoneId(), backupVO.getId(),
                String.format("Backup %s - VM %s", backupVO.getUuid(), userVm.getUuid()), backupVO.getBackupOfferingId(), null, backupVO.getSize(),
                backupVO.getProtectedSize(), Backup.class.getName(), backupVO.getUuid(), details);
    }

    private void processBackupFailure(Answer answer, VirtualMachine vm, long hostId, boolean runningVm, BackupVO backupVO) {
        if (answer instanceof TakeKnibBackupAnswer && ((TakeKnibBackupAnswer) answer).isVmConsistent()) {
            logger.info("Backup [{}] of VM [{}] failed. However, the VM is still consistent, so we will roll back its state.", backupVO.getUuid(), vm.getUuid());
            backupVO.setStatus(Backup.Status.Failed);

            transitStateWithoutThrow(vm, runningVm ? VirtualMachine.Event.OperationFailedToRunning : VirtualMachine.Event.OperationFailedToStopped, hostId);
        } else {
            logger.info("Backup [{}] of VM [{}] ended in error. We are not sure if the VM is consistent; thus, we will set it as BackupError.", backupVO.getUuid(), vm.getUuid());
            transitStateWithoutThrow(vm, VirtualMachine.Event.OperationFailedToError, hostId);
            backupVO.setStatus(Backup.Status.Error);
        }

        backupDao.update(backupVO.getId(), backupVO);
    }

    private void processRemovedBackups(List<Long> removedBackupIds) {
        for (Long removedBackupId : removedBackupIds) {
            BackupVO removedBackupVO = backupDao.findByIdIncludingRemoved(removedBackupId);
            removedBackupVO.setStatus(Backup.Status.Expunged);
            backupDao.update(removedBackupId, removedBackupVO);
            nativeBackupDataStoreDao.expungeByBackupId(removedBackupId);
            backupDetailDao.removeDetailsExcept(removedBackupId, END_OF_CHAIN);
        }
    }

    /**
     * For every backup, except for the one which the command was issued, will set them as Expunged regardless and hope operators will look
     * at the logs. For the current one, if forced=false, will set it as error, otherwise, will set it as Expunged as well.
     * */
    private boolean processRemoveBackupFailures(boolean forced, Answer[] deleteAnswers, List<Long> removedBackupIds, NativeBackupJoinVO backupJoinVO) {
        List<Answer> failures = Arrays.stream(deleteAnswers).filter(answer -> !answer.getResult()).collect(Collectors.toList());
        Set<Long> failedToRemoveBackupIdSet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(failures)) {
            StringBuilder failureStringBuilder = new StringBuilder("Encountered the following failures during backup removal, all will be marked as Expunged and need to be" +
                    " manually deleted from storage. ");
            for (Answer answer : failures) {
                failedToRemoveBackupIdSet.add(((BackupDeleteAnswer)answer).getBackupId());
                failureStringBuilder.append(answer.getDetails());
            }
            logger.error(failureStringBuilder.toString());
        }

        removedBackupIds.removeAll(failedToRemoveBackupIdSet);

        if (!forced && failedToRemoveBackupIdSet.remove(backupJoinVO.getId())) {
            BackupVO failedVO = backupDao.findByIdIncludingRemoved(backupJoinVO.getId());
            logger.info("Since backup delete command was not forced, will not set the main backup [{}] as Expunged, will set it as error instead.", failedVO.getUuid());
            failedVO.setStatus(Backup.Status.Error);
            backupDao.update(failedVO.getId(), failedVO);
        }

        for (Long failedToRemove : failedToRemoveBackupIdSet) {
            BackupVO failedVO = backupDao.findByIdIncludingRemoved(failedToRemove);
            failedVO.setStatus(Backup.Status.Expunged);
            logger.error("Setting backup [{}] as expunged, even though there was an error when deleting it from storage. Please look at the logs and check if it was deleted from" +
                    " storage.", failedVO.getUuid());
            backupDao.update(failedToRemove, failedVO);
        }

        return failedToRemoveBackupIdSet.isEmpty();
    }

    /**
     * Merges the backup deltas related to the passed {@code NativeBackupJoinVO}. Will set the parent, if any, as end_of_chain.
     *
     * @return true if the merge was successful and false otherwise.
     * */
    protected boolean mergeCurrentBackupDeltas(NativeBackupJoinVO backupJoinVO, BackupVO backupVO) {
        VirtualMachine userVm = userVmDao.findById(backupVO.getVmId());

        VMSnapshotVO succeedingVmSnapshot = getSucceedingVmSnapshot(backupJoinVO);
        MergeDiskOnlyVmSnapshotCommand cmd = buildMergeDiskOnlyVmSnapshotCommandForCurrentBackup(backupJoinVO, userVm, succeedingVmSnapshot);
        Long hostId = vmSnapshotHelper.pickRunningHost(backupVO.getVmId());

        Answer answer = agentManager.easySend(hostId, cmd);
        if (answer == null || !answer.getResult()) {
            logger.error("Failed to remove backup [{}]. Tried to merge the current deltas to cleanup the VM but failed due to [{}].",
                    backupVO, answer != null ? answer.getDetails() : "no answer");
            return false;
        }

        backupDetailDao.persist(new BackupDetailVO(backupVO.getId(), END_OF_CHAIN, Boolean.TRUE.toString()));

        expungeOldDeltasAndUpdateVmSnapshotIfNeeded(nativeBackupStoragePoolDao.listByBackupId(backupJoinVO.getId()), succeedingVmSnapshot);

        if (succeedingVmSnapshot != null) {
            return true;
        }

        for (DeltaMergeTreeTO deltaMergeTreeTO : cmd.getDeltaMergeTreeToList()) {
            VolumeVO volumeVO = volumeDao.findById(deltaMergeTreeTO.getVolumeObjectTO().getVolumeId());
            volumeVO.setPath(deltaMergeTreeTO.getParent().getPath());
            logger.debug("Updating volume [{}] path to [{}] as part of the backup delete cleanup process.", volumeVO.getUuid(), volumeVO.getPath());
            volumeDao.update(volumeVO.getId(), volumeVO);
        }

        return true;
    }

    private void createDeleteCommandsAndMergeTrees(List<VolumeObjectTO> volumeObjectTOs, Commands commands, List<NativeBackupStoragePoolVO> deletedDeltas,
            VMSnapshotVO vmSnapshotSucceedingCurrentBackup, List<DeltaMergeTreeTO> deltaMergeTreeTOList) {
        for (VolumeObjectTO volumeObjectTO : volumeObjectTOs) {
            NativeBackupStoragePoolVO delta = nativeBackupStoragePoolDao.findOneByVolumeId(volumeObjectTO.getVolumeId());
            if (delta == null) {
                continue;
            }
            if (delta.getBackupDeltaPath().equals(volumeObjectTO.getPath())) {
                commands.addCommand(new DeleteCommand(new BackupDeltaTO(volumeObjectTO.getDataStore(), Hypervisor.HypervisorType.KVM, delta.getBackupDeltaParentPath())));
                deletedDeltas.add(delta);
                logger.debug("Volume [{}] has a backup delta that will be deleted as part of the preparation to revert a VM snapshot.", volumeObjectTO.getUuid());
            } else {
                deltaMergeTreeTOList.add(createDeltaMergeTree(false, false, delta, volumeObjectTO, vmSnapshotSucceedingCurrentBackup));
            }
        }
    }

    private List<NativeBackupJoinVO> getParentsToBeExpungedWithBackupAndAddThemToListOfDeleteCommands(BackupVO backupVO, Commands deleteCommands) {
        logger.debug("Searching for removed parents of [{}] that should be expunged.", backupVO);
        List<NativeBackupJoinVO> backupParents = getBackupJoinParents(backupVO, true);
        List<NativeBackupJoinVO> backupParentsToBeExpunged = null;
        for (int i = 0; i < backupParents.size(); i++) {
            NativeBackupJoinVO backupParent = backupParents.get(i);
            if (Backup.Status.Removed.equals(backupParent.getStatus())) {
                addBackupDeltasToDeleteCommand(backupParent.getId(), deleteCommands);
            } else {
                backupParentsToBeExpunged = backupParents.subList(0, i);
                break;
            }
        }
        if (backupParentsToBeExpunged == null) {
            backupParentsToBeExpunged = backupParents;
        }
        logger.debug("Found [{}] removed parents of [{}] that should be expunged: [{}].", backupParentsToBeExpunged.size(), backupVO, backupParentsToBeExpunged);
        return backupParentsToBeExpunged;
    }

    private MergeDiskOnlyVmSnapshotCommand buildMergeDiskOnlyVmSnapshotCommandForCurrentBackup(NativeBackupJoinVO backupJoinVO, VirtualMachine userVm, VMSnapshotVO vmSnapshot) {
        List<DeltaMergeTreeTO> deltaMergeTreeTOs = new ArrayList<>();

        List<VolumeObjectTO> volumeTOs = vmSnapshotHelper.getVolumeTOList(backupJoinVO.getVmId());
        Map<Long, List<SnapshotDataStoreVO>> volumeIdToSnapshotDataStoreList = gatherSnapshotReferencesOfChildrenSnapshot(volumeTOs, vmSnapshot);
        List<NativeBackupStoragePoolVO> deltasOnPrimary = nativeBackupStoragePoolDao.listByBackupId(backupJoinVO.getId());

        for (VolumeObjectTO volumeObjectTO : volumeTOs) {
            KnibTO knibTO = new KnibTO(volumeObjectTO, volumeIdToSnapshotDataStoreList.getOrDefault(volumeObjectTO.getId(), new ArrayList<>()));
            createDeltaMergeTreeForVolume(vmSnapshot == null, userVm.getState() == VirtualMachine.State.Running, deltasOnPrimary, vmSnapshot, knibTO);
            if (knibTO.getDeltaMergeTreeTO() != null) {
                deltaMergeTreeTOs.add(knibTO.getDeltaMergeTreeTO());
            } else {
                logger.debug("Volume [{}] does not have any deltas to merge as part of the backup delete process.", volumeObjectTO.getUuid());
            }
        }

        return new MergeDiskOnlyVmSnapshotCommand(deltaMergeTreeTOs, userVm.getState().equals(VirtualMachine.State.Running), userVm.getInstanceName());
    }

    private DataStore addBackupDeltasToDeleteCommand(long backupId, Commands deleteCommands) {
        NativeBackupJoinVO nativeBackupJoinVO = nativeBackupJoinDao.findById(backupId);
        List<NativeBackupDataStoreVO> nativeBackupDataStoreVOs = nativeBackupDataStoreDao.listByBackupId(backupId);
        DataStore dataStore = dataStoreManager.getDataStore(nativeBackupJoinVO.getImageStoreId(), DataStoreRole.Image);
        DataStoreTO dataStoreTO = dataStore.getTO();
        for (NativeBackupDataStoreVO nativeBackupDataStoreVO : nativeBackupDataStoreVOs) {
            BackupDeltaTO backupDeltaTO = new BackupDeltaTO(dataStoreTO, Hypervisor.HypervisorType.KVM, nativeBackupDataStoreVO.getBackupPath());
            backupDeltaTO.setId(backupId);
            DeleteCommand deleteCommand = new DeleteCommand(backupDeltaTO);
            deleteCommands.addCommand(deleteCommand);
        }
        return dataStore;
    }

    /**
     * Gets the list of backup parents of a given BackupVO.
     * @param backupVO the backup in question.
     * @param includeRemoved whether to include removed (but not expunged) parents or not.
     * @return list of parents, or an empty list if no parents found.
     * */
    protected List<NativeBackupJoinVO> getBackupJoinParents(BackupVO backupVO, boolean includeRemoved) {
        List<NativeBackupJoinVO> ancestorBackups;

        if (includeRemoved) {
            ancestorBackups = nativeBackupJoinDao.listIncludingRemovedByVmIdAndBeforeDateOrderByCreatedDesc(backupVO.getVmId(), backupVO.getDate());
        } else {
            ancestorBackups = nativeBackupJoinDao.listByBackedUpAndVmIdAndBeforeDateOrderByCreatedDesc(backupVO.getVmId(), backupVO.getDate());
        }

        for (int i = 0; i < ancestorBackups.size(); i++) {
            if (ancestorBackups.get(i).getEndOfChain()) {
                return ancestorBackups.subList(0, i);
            }
        }

        logger.debug("Found the following backup chain ancestors of backup [{}]: [{}].", backupVO, ancestorBackups);
        return ancestorBackups;
    }

    /**
     * Creates a detail for the given BackupVO if the remaining chain size is one or less and the value of backupChainSize is greater than 0.
     * */
    protected void setEndOfChainTrueIfRemainingChainSizeIsOneOrLess(int remainingChainSize, long zoneId, long backupId, String backupUuid) {
        if (remainingChainSize <= 1 && backupChainSize.valueIn(zoneId) > 0) {
            logger.debug("Setting backup [{}] as end of chain.", backupUuid);
            backupDetailDao.persist(new BackupDetailVO(backupId, END_OF_CHAIN, Boolean.TRUE.toString()));
        }
    }

    private void setBackupVirtualSize(List<VolumeObjectTO> volumeTOs, BackupVO backupVO) {
        long virtualSize = 0;
        for (VolumeObjectTO volumeObjectTO : volumeTOs) {
            virtualSize += volumeObjectTO.getSize();
        }

        backupVO.setProtectedSize(virtualSize);
    }

    private void updateBackupStatusToBackingUp(List<VolumeObjectTO> volumeTOs, BackupVO backupVO) {
        setBackupVirtualSize(volumeTOs, backupVO);
        backupVO.setStatus(Backup.Status.BackingUp);
        backupDao.update(backupVO.getId(), backupVO);
    }

    /**
     * Retrieves the current backup and removes the CURRENT detail. If the informed backup is not the end of chain, sets is as the new CURRENT
     * */
    private void updateCurrentBackup(NativeBackupJoinVO backup) {
        NativeBackupJoinVO current = nativeBackupJoinDao.findCurrent(backup.getVmId());

        if (current != null) {
            backupDetailDao.removeDetail(current.getId(), CURRENT);
        }

        if (!backup.getEndOfChain()) {
            backupDetailDao.persist(new BackupDetailVO(backup.getId(), CURRENT, Boolean.TRUE.toString()));
        }
    }

    /**
     * Given a backup, removes the CURRENT detail, and if the snapshot is not set as end of chain, sets it as end of chain.
     * */
    protected void setEndOfChainAndRemoveCurrentForBackup(NativeBackupJoinVO currentBackup) {
        backupDetailDao.removeDetail(currentBackup.getId(), CURRENT);
        if (!currentBackup.getEndOfChain()) {
            backupDetailDao.persist(new BackupDetailVO(currentBackup.getId(), END_OF_CHAIN, Boolean.TRUE.toString()));
        }
    }

    private void createDetails(Long imageStoreId, Long parentId, BackupVO backupVO) {
        backupDetailDao.persist(new BackupDetailVO(backupVO.getId(), IMAGE_STORE_ID, imageStoreId.toString()));
        backupDetailDao.persist(new BackupDetailVO(backupVO.getId(), PARENT_ID, parentId.toString()));
    }

    private void updateReferencesAfterPrepareForSnapshotRevert(List<DeltaMergeTreeTO> deltaMergeTreeTOList, List<SnapshotDataStoreVO> snapRefsSucceedingCurrentBackup,
            List<NativeBackupStoragePoolVO> deletedDeltas, NativeBackupJoinVO backupVO) {
        for (DeltaMergeTreeTO deltaMergeTreeTO : deltaMergeTreeTOList) {
            SnapshotDataStoreVO snapshotRef = snapRefsSucceedingCurrentBackup.stream()
                    .filter(ref -> Objects.equals(ref.getVolumeId(), deltaMergeTreeTO.getVolumeObjectTO().getVolumeId()))
                    .findFirst()
                    .orElse(null);
            if (snapshotRef != null) {
                snapshotRef.setInstallPath(deltaMergeTreeTO.getParent().getPath());
                logger.debug("Updating snapshot reference [{}] path to [{}] as part of the preparation to restore a VM snapshot.", snapshotRef.getId(), snapshotRef.getInstallPath());
                snapshotDataStoreDao.update(snapshotRef.getId(), snapshotRef);
            }
            nativeBackupStoragePoolDao.expungeByVolumeId(deltaMergeTreeTO.getVolumeObjectTO().getVolumeId());
        }

        for (NativeBackupStoragePoolVO delta : deletedDeltas) {
            nativeBackupStoragePoolDao.expungeByVolumeId(delta.getVolumeId());
        }

        setEndOfChainAndRemoveCurrentForBackup(backupVO);
    }

    private void validateVmState(VirtualMachine vm, String operation, VirtualMachine.State... additionalStates) {
        List<VirtualMachine.State> allowedStates = new ArrayList<>(this.allowedVmStates);
        allowedStates.addAll(Arrays.asList(additionalStates));
        if (!allowedStates.contains(vm.getState())) {
            throw new InvalidParameterValueException(String.format("VM [%s] is not in the right state to %s. It must be in one of these states: %s", vm.getUuid(), operation,
                    allowedStates));
        }
    }

    private void validateStorages(List<VolumeObjectTO> volumeTOs, String vmUuid) {
        for (VolumeObjectTO volumeObjectTO : volumeTOs) {
            StoragePoolVO storagePoolVO = storagePoolDao.findById(volumeObjectTO.getPoolId());
            if (!supportedStoragePoolTypes.contains(storagePoolVO.getPoolType())) {
                logger.error("Only able to take backups of VMs with volumes in the following storage types [{}]. Throwing an exception.", supportedStoragePoolTypes);
                throw new InvalidParameterValueException(String.format("Unable to take backup of VM [%s], please check the logs.", vmUuid));
            }
        }
    }

    private void validateNoVmSnapshots(VirtualMachine vm) {
        List<VMSnapshotVO> vmSnapshotVOs = vmSnapshotDao.findByVm(vm.getId());
        if (!vmSnapshotVOs.isEmpty()) {
            throw new InvalidParameterValueException(String.format("Restoring VM [%s] would remove the current VM snapshots it has. Please remove the VM snapshots [%s] before" +
                    " restoring the backup.", vm.getUuid(), vmSnapshotVOs.stream().map(VMSnapshotVO::getUuid).collect(Collectors.toList())));
        }
    }

    protected void transitStateWithoutThrow(VirtualMachine vm, VirtualMachine.Event event, long hostId) {
        try {
            virtualMachineManager.stateTransitTo(vm, event, hostId);
        } catch (NoTransitionException e) {
            String msg = String.format("Failed to change VM [%s] state with event [%s].", vm.getUuid(), event.toString());
            logger.error(msg, e);
            throw new CloudRuntimeException(msg, e);
        }
    }
}
