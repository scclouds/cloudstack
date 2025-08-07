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

import com.cloud.event.ActionEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.gson.Gson;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.backup.UpdateBackupOfferingCmd;
import org.apache.cloudstack.api.command.user.backup.DeleteBackupScheduleCmd;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.dao.BackupScheduleDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BackupManagerTest {
    @Spy
    @InjectMocks
    BackupManagerImpl backupManager = new BackupManagerImpl();

    @Mock
    BackupOfferingDao backupOfferingDao;

    @Mock
    BackupProvider backupProvider;

    @Mock
    VirtualMachineManager virtualMachineManager;

    @Mock
    VolumeApiService volumeApiService;

    @Mock
    VolumeDao volumeDao;

    @Mock
    private VMInstanceVO vmInstanceVOMock;

    @Mock
    private CallContext callContextMock;

    @Mock
    private VMInstanceDao vmInstanceDao;

    @Mock
    private AccountManager accountManager;

    @Mock
    private AccountVO accountVOMock;

    @Mock
    private DeleteBackupScheduleCmd deleteBackupScheduleCmdMock;

    @Mock
    private BackupScheduleVO backupScheduleVOMock;

    @Mock
    private BackupScheduleDao backupScheduleDaoMock;

    @Mock
    private BackupOfferingVO backupOfferingVOMock;

    @Mock
    private AsyncJobVO asyncJobVOMock;

    @Mock
    private BackupDao backupDaoMock;

    private Gson gson;

    private String[] hostPossibleValues = {"127.0.0.1", "hostname"};
    private String[] datastoresPossibleValues = {"e9804933-8609-4de3-bccc-6278072a496c", "datastore-name"};
    private AutoCloseable closeable;

    @Before
    public void setup() throws Exception {
        gson = new Gson();

        closeable = MockitoAnnotations.openMocks(this);
        when(backupOfferingDao.findById(null)).thenReturn(null);
        when(backupOfferingDao.findById(123l)).thenReturn(null);

        BackupOfferingVO offering = Mockito.spy(BackupOfferingVO.class);
        when(offering.getName()).thenCallRealMethod();
        when(offering.getDescription()).thenCallRealMethod();
        when(offering.isUserDrivenBackupAllowed()).thenCallRealMethod();

        BackupOfferingVO offeringUpdate = Mockito.spy(BackupOfferingVO.class);

        when(backupOfferingDao.findById(1234l)).thenReturn(offering);
        when(backupOfferingDao.createForUpdate(1234l)).thenReturn(offeringUpdate);
        when(backupOfferingDao.update(1234l, offeringUpdate)).thenAnswer(answer -> {
            offering.setName("New name");
            offering.setDescription("New description");
            offering.setUserDrivenBackupAllowed(true);
            return true;
        });
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testExceptionWhenUpdateWithNullId() {
        try {
            Long id = null;

            UpdateBackupOfferingCmd cmd = Mockito.spy(UpdateBackupOfferingCmd.class);
            when(cmd.getId()).thenReturn(id);

            backupManager.updateBackupOffering(cmd);
        } catch (InvalidParameterValueException e) {
            assertEquals("Unable to find Backup Offering with id: [null].", e.getMessage());
        }
    }

    @Test (expected = InvalidParameterValueException.class)
    public void testExceptionWhenUpdateWithNonExistentId() {
        Long id = 123l;

        UpdateBackupOfferingCmd cmd = Mockito.spy(UpdateBackupOfferingCmd.class);
        when(cmd.getId()).thenReturn(id);

        backupManager.updateBackupOffering(cmd);
    }

    @Test (expected = ServerApiException.class)
    public void testExceptionWhenUpdateWithoutChanges() {
        UpdateBackupOfferingCmd cmd = Mockito.spy(UpdateBackupOfferingCmd.class);
        when(cmd.getName()).thenReturn(null);
        when(cmd.getDescription()).thenReturn(null);
        when(cmd.getAllowUserDrivenBackups()).thenReturn(null);

        Mockito.doCallRealMethod().when(cmd).execute();

        cmd.execute();
    }

    @Test
    public void testUpdateBackupOfferingSuccess() {
        Long id = 1234l;

        UpdateBackupOfferingCmd cmd = Mockito.spy(UpdateBackupOfferingCmd.class);
        when(cmd.getId()).thenReturn(id);
        when(cmd.getName()).thenReturn("New name");
        when(cmd.getDescription()).thenReturn("New description");
        when(cmd.getAllowUserDrivenBackups()).thenReturn(true);

        BackupOffering updated = backupManager.updateBackupOffering(cmd);
        assertEquals("New name", updated.getName());
        assertEquals("New description", updated.getDescription());
        assertEquals(true, updated.isUserDrivenBackupAllowed());
    }

    @Test
    public void restoreBackedUpVolumeTestHostIpAndDatastoreUuid() {
        BackupVO backupVO = new BackupVO();
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        String volumeUuid = "5f4ed903-ac23-4f8a-b595-69c73c40593f";
        String vmName = "i-2-3-VM";
        VirtualMachine.State vmState = VirtualMachine.State.Running;
        Mockito.when(vm.getName()).thenReturn(vmName);
        Mockito.when(vm.getState()).thenReturn(vmState);
        Pair<String, VirtualMachine.State> vmNameAndState = new Pair<>("i-2-3-VM", VirtualMachine.State.Running);

        Mockito.when(backupProvider.restoreBackedUpVolume(Mockito.any(), Mockito.eq(volumeUuid),
                Mockito.eq("127.0.0.1"), Mockito.eq("e9804933-8609-4de3-bccc-6278072a496c"), Mockito.eq(vmNameAndState), Mockito.any(), Mockito.any())).thenReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success"));
        Pair<Boolean,String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeUuid, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues, vm, false);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success", restoreBackedUpVolume.second());

        Mockito.verify(backupProvider, times(1)).restoreBackedUpVolume(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), any(Pair.class), Mockito.any(), Mockito.any());
    }

    @Test
    public void restoreBackedUpVolumeTestHostIpAndDatastoreName() {
        BackupVO backupVO = new BackupVO();
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        String volumeUuid = "5f4ed903-ac23-4f8a-b595-69c73c40593f";
        String vmName = "i-2-3-VM";
        VirtualMachine.State vmState = VirtualMachine.State.Running;
        Mockito.when(vm.getName()).thenReturn(vmName);
        Mockito.when(vm.getState()).thenReturn(vmState);
        Pair<String, VirtualMachine.State> vmNameAndState = new Pair<>("i-2-3-VM", VirtualMachine.State.Running);
        Mockito.when(backupProvider.restoreBackedUpVolume(Mockito.any(), Mockito.eq(volumeUuid),
                Mockito.eq("127.0.0.1"), Mockito.eq("datastore-name"), Mockito.eq(vmNameAndState), Mockito.any(), Mockito.any())).thenReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success2"));
        Pair<Boolean,String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeUuid, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues, vm, false);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success2", restoreBackedUpVolume.second());

        Mockito.verify(backupProvider, times(2)).restoreBackedUpVolume(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), any(Pair.class), Mockito.any(), Mockito.any());
    }

    @Test
    public void restoreBackedUpVolumeTestHostNameAndDatastoreUuid() {
        BackupVO backupVO = new BackupVO();
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        String volumeUuid = "5f4ed903-ac23-4f8a-b595-69c73c40593f";
        String vmName = "i-2-3-VM";
        VirtualMachine.State vmState = VirtualMachine.State.Running;
        Mockito.when(vm.getName()).thenReturn(vmName);
        Mockito.when(vm.getState()).thenReturn(vmState);
        Pair<String, VirtualMachine.State> vmNameAndState = new Pair<>("i-2-3-VM", VirtualMachine.State.Running);

        Mockito.when(backupProvider.restoreBackedUpVolume(Mockito.any(), Mockito.eq(volumeUuid),
                Mockito.eq("hostname"), Mockito.eq("e9804933-8609-4de3-bccc-6278072a496c"), Mockito.eq(vmNameAndState), Mockito.any(), Mockito.any())).thenReturn(new Pair<>(Boolean.TRUE, "Success3"));
        Pair<Boolean,String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeUuid, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues, vm, true);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success3", restoreBackedUpVolume.second());

        Mockito.verify(backupProvider, times(3)).restoreBackedUpVolume(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), any(Pair.class), Mockito.any(), Mockito.any());
    }

    @Test
    public void restoreBackedUpVolumeTestHostAndDatastoreName() {
        BackupVO backupVO = new BackupVO();
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        String volumeUuid = "5f4ed903-ac23-4f8a-b595-69c73c40593f";
        String vmName = "i-2-3-VM";
        VirtualMachine.State vmState = VirtualMachine.State.Running;
        Mockito.when(vm.getName()).thenReturn(vmName);
        Mockito.when(vm.getState()).thenReturn(vmState);
        Pair<String, VirtualMachine.State> vmNameAndState = new Pair<>("i-2-3-VM", VirtualMachine.State.Running);

        Mockito.when(backupProvider.restoreBackedUpVolume(Mockito.any(), Mockito.eq(volumeUuid),
                Mockito.eq("hostname"), Mockito.eq("datastore-name"),  Mockito.eq(vmNameAndState), Mockito.any(), Mockito.any())).thenReturn(new Pair<Boolean, String>(Boolean.TRUE, "Success4"));
        Pair<Boolean,String> restoreBackedUpVolume = backupManager.restoreBackedUpVolume(volumeUuid, backupVO, backupProvider, hostPossibleValues, datastoresPossibleValues, vm, true);

        assertEquals(Boolean.TRUE, restoreBackedUpVolume.first());
        assertEquals("Success4", restoreBackedUpVolume.second());

        Mockito.verify(backupProvider, times(4)).restoreBackedUpVolume(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), any(Pair.class), Mockito.eq(vm), Mockito.eq(true));
    }

    @Test
    public void tryRestoreVMTestRestoreSucceeded() throws NoTransitionException {
        BackupOffering offering = Mockito.mock(BackupOffering.class);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        BackupVO backup = Mockito.mock(BackupVO.class);

        try (MockedStatic<ActionEventUtils> utils = Mockito.mockStatic(ActionEventUtils.class)) {
            Mockito.when(ActionEventUtils.onStartedActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.eq(true), Mockito.eq(0))).thenReturn(1L);
            Mockito.when(ActionEventUtils.onCompletedActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.eq(0))).thenReturn(2L);

            Mockito.when(volumeDao.findIncludingRemovedByInstanceAndType(1L, null)).thenReturn(Collections.singletonList(volumeVO));
            Mockito.when(virtualMachineManager.stateTransitTo(Mockito.eq(vm), Mockito.eq(VirtualMachine.Event.RestoringRequested), Mockito.any())).thenReturn(true);
            Mockito.when(volumeApiService.stateTransitTo(Mockito.eq(volumeVO), Mockito.eq(Volume.Event.RestoreRequested))).thenReturn(true);

            Mockito.when(vm.getId()).thenReturn(1L);
            Mockito.when(offering.getProvider()).thenReturn("veeam");
            Mockito.doReturn(backupProvider).when(backupManager).getBackupProvider("veeam");
            Mockito.when(backupProvider.restoreVMFromBackup(vm, backup)).thenReturn(true);

            backupManager.tryRestoreVM(backup, vm, offering, "Nothing to write here.");
        }
    }

    @Test
    public void tryRestoreVMTestRestoreFails() throws NoTransitionException {
        BackupOffering offering = Mockito.mock(BackupOffering.class);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        BackupVO backup = Mockito.mock(BackupVO.class);

        try (MockedStatic<ActionEventUtils> utils = Mockito.mockStatic(ActionEventUtils.class)) {
            Mockito.when(ActionEventUtils.onStartedActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.eq(true), Mockito.eq(0))).thenReturn(1L);
            Mockito.when(ActionEventUtils.onCompletedActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.eq(0))).thenReturn(2L);

            Mockito.when(volumeDao.findIncludingRemovedByInstanceAndType(1L, null)).thenReturn(Collections.singletonList(volumeVO));
            Mockito.when(virtualMachineManager.stateTransitTo(Mockito.eq(vm), Mockito.eq(VirtualMachine.Event.RestoringRequested), Mockito.any())).thenReturn(true);
            Mockito.when(volumeApiService.stateTransitTo(Mockito.eq(volumeVO), Mockito.eq(Volume.Event.RestoreRequested))).thenReturn(true);
            Mockito.when(virtualMachineManager.stateTransitTo(Mockito.eq(vm), Mockito.eq(VirtualMachine.Event.RestoringFailed), Mockito.any())).thenReturn(true);
            Mockito.when(volumeApiService.stateTransitTo(Mockito.eq(volumeVO), Mockito.eq(Volume.Event.RestoreFailed))).thenReturn(true);

            Mockito.when(vm.getId()).thenReturn(1L);
            Mockito.when(offering.getProvider()).thenReturn("veeam");
            Mockito.doReturn(backupProvider).when(backupManager).getBackupProvider("veeam");
            Mockito.when(backupProvider.restoreVMFromBackup(vm, backup)).thenReturn(false);
            try {
                backupManager.tryRestoreVM(backup, vm, offering, "Checking message error.");
                fail("An exception is needed.");
            } catch (CloudRuntimeException e) {
                assertEquals("Error restoring VM from backup [Checking message error.].", e.getMessage());
            }
        }
    }

    @Test
    public void checkCallerAccessToBackupScheduleVmTestExecuteAccessCheckMethods() {
        long vmId = 1L;
        long dataCenterId = 2L;

        try (MockedStatic<CallContext> mockedCallContext = Mockito.mockStatic(CallContext.class)) {
            Mockito.when(vmInstanceDao.findById(vmId)).thenReturn(vmInstanceVOMock);
            Mockito.when(vmInstanceVOMock.getDataCenterId()).thenReturn(dataCenterId);
            Mockito.when(backupManager.isDisabled(dataCenterId)).thenReturn(false);

            mockedCallContext.when(CallContext::current).thenReturn(callContextMock);
            Mockito.when(callContextMock.getCallingAccount()).thenReturn(accountVOMock);
            Mockito.doNothing().when(accountManager).checkAccess(accountVOMock, null, true, vmInstanceVOMock);
            backupManager.checkCallerAccessToBackupScheduleVm(vmId);

            verify(accountManager, times(1)).checkAccess(accountVOMock, null, true, vmInstanceVOMock);
        }
    }

    @Test
    public void deleteAllVmBackupSchedulesTestReturnSuccessWhenAllSchedulesAreDeleted() {
        long vmId = 1L;
        List<BackupScheduleVO> backupSchedules = List.of(Mockito.mock(BackupScheduleVO.class), Mockito.mock(BackupScheduleVO.class));
        Mockito.when(backupScheduleDaoMock.listByVM(vmId)).thenReturn(backupSchedules);
        Mockito.when(backupSchedules.get(0).getId()).thenReturn(2L);
        Mockito.when(backupSchedules.get(1).getId()).thenReturn(3L);
        Mockito.when(backupScheduleDaoMock.remove(Mockito.anyLong())).thenReturn(true);

        boolean success = backupManager.deleteAllVmBackupSchedules(vmId);
        assertTrue(success);
        Mockito.verify(backupScheduleDaoMock, times(2)).remove(Mockito.anyLong());
    }

    @Test
    public void deleteAllVmBackupSchedulesTestReturnFalseWhenAnyDeletionFails() {
        long vmId = 1L;
        List<BackupScheduleVO> backupSchedules = List.of(Mockito.mock(BackupScheduleVO.class), Mockito.mock(BackupScheduleVO.class));
        Mockito.when(backupScheduleDaoMock.listByVM(vmId)).thenReturn(backupSchedules);
        Mockito.when(backupSchedules.get(0).getId()).thenReturn(2L);
        Mockito.when(backupSchedules.get(1).getId()).thenReturn(3L);
        Mockito.when(backupScheduleDaoMock.remove(2L)).thenReturn(true);
        Mockito.when(backupScheduleDaoMock.remove(3L)).thenReturn(false);

        boolean success = backupManager.deleteAllVmBackupSchedules(vmId);
        assertFalse(success);
        Mockito.verify(backupScheduleDaoMock, times(2)).remove(Mockito.anyLong());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void deleteBackupScheduleTestThrowExceptionWhenVmIdAndScheduleIdAreNull() {
        backupManager.deleteBackupSchedule(null, null);
    }

    @Test
    public void deleteBackupScheduleTestDeleteVmSchedulesWhenVmIdIsSpecified() {
        long vmId = 1L;

        Mockito.doNothing().when(backupManager).checkCallerAccessToBackupScheduleVm(vmId);
        Mockito.doReturn(true).when(backupManager).deleteAllVmBackupSchedules(vmId);

        boolean success = backupManager.deleteBackupSchedule(null, vmId);
        assertTrue(success);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void deleteBackupScheduleTestThrowExceptionWhenSpecificScheduleIsNotFound() {
        long id = 1L;
        backupManager.deleteBackupSchedule(id, null);
    }

    @Test
    public void deleteBackupScheduleTestDeleteSpecificScheduleWhenItsIdIsSpecified() {
        long id = 1L;
        long vmId = 2L;
        when(backupScheduleDaoMock.findById(id)).thenReturn(backupScheduleVOMock);
        when(backupScheduleVOMock.getVmId()).thenReturn(vmId);
        Mockito.doNothing().when(backupManager).checkCallerAccessToBackupScheduleVm(vmId);
        when(backupScheduleVOMock.getId()).thenReturn(id);
        when(backupScheduleDaoMock.remove(id)).thenReturn(true);

        boolean success = backupManager.deleteBackupSchedule(id, vmId);
        assertTrue(success);
    }

    @Test
    public void validateAndGetDefaultBackupRetentionIfRequiredTestReturnZeroAsDefaultValue() {
        int retention = backupManager.validateAndGetDefaultBackupRetentionIfRequired(null, backupOfferingVOMock);
        assertEquals(0, retention);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateAndGetDefaultBackupRetentionIfRequiredTestThrowExceptionWhenBackupOfferingProviderIsVeeam() {
        Mockito.when(backupOfferingVOMock.getProvider()).thenReturn("veeam");
        backupManager.validateAndGetDefaultBackupRetentionIfRequired(1, backupOfferingVOMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateAndGetDefaultBackupRetentionIfRequiredTestThrowExceptionWhenMaxBackupsIsLessThanZero() {
        backupManager.validateAndGetDefaultBackupRetentionIfRequired(-1, backupOfferingVOMock);
    }

    @Test
    public void validateAndGetDefaultBackupRetentionIfRequiredTestReturnProvidedRetentionWhenValidationsDoNotNeedToBeApplied() {
        int retention = backupManager.validateAndGetDefaultBackupRetentionIfRequired(7, backupOfferingVOMock);
        assertEquals(7, retention);
    }

    @Test
    public void getBackupScheduleTestReturnNullWhenBackupIsManual() {
        String jobParams = "{}";
        when(asyncJobVOMock.getCmdInfo()).thenReturn(jobParams);
        when(asyncJobVOMock.getId()).thenReturn(1L);

        Long backupScheduleId = backupManager.getBackupScheduleId(asyncJobVOMock);
        assertNull(backupScheduleId);
    }

    @Test
    public void getBackupScheduleTestReturnBackupScheduleIdWhenBackupIsScheduled() {
        Map<String, String> params = Map.of(
                ApiConstants.BACKUP_SCHEDULE_ID, "100"
        );
        String jobParams = gson.toJson(params);
        when(asyncJobVOMock.getCmdInfo()).thenReturn(jobParams);
        when(asyncJobVOMock.getId()).thenReturn(1L);

        Long backupScheduleId = backupManager.getBackupScheduleId(asyncJobVOMock);
        assertEquals(Long.valueOf("100"), backupScheduleId);
    }

    @Test
    public void getBackupScheduleTestReturnNullWhenSpecifiedBackupScheduleIdIsNotALongValue() {
        Map<String, String> params = Map.of(
                ApiConstants.BACKUP_SCHEDULE_ID, "InvalidValue"
        );
        String jobParams = gson.toJson(params);
        when(asyncJobVOMock.getCmdInfo()).thenReturn(jobParams);
        when(asyncJobVOMock.getId()).thenReturn(1L);

        Long backupScheduleId = backupManager.getBackupScheduleId(asyncJobVOMock);
        assertNull(backupScheduleId);
    }

    @Test
    public void deleteOldestBackupFromScheduleIfRequiredTestSkipDeletionWhenBackupScheduleIsNotFound() {
        backupManager.deleteOldestBackupFromScheduleIfRequired(1L, 1L);
        Mockito.verify(backupManager, Mockito.never()).deleteExcessBackups(Mockito.anyList(), Mockito.anyInt(), Mockito.anyLong());
    }

    @Test
    public void deleteOldestBackupFromScheduleIfRequiredTestSkipDeletionWhenRetentionIsEqualToZero() {
        Mockito.when(backupScheduleDaoMock.findById(1L)).thenReturn(backupScheduleVOMock);
        Mockito.when(backupScheduleVOMock.getMaxBackups()).thenReturn(0);
        backupManager.deleteOldestBackupFromScheduleIfRequired(1L, 1L);
        Mockito.verify(backupManager, Mockito.never()).deleteExcessBackups(Mockito.anyList(), Mockito.anyInt(), Mockito.anyLong());
    }

    @Test
    public void deleteOldestBackupFromScheduleIfRequiredTestSkipDeletionWhenAmountOfBackupsToBeDeletedIsLessThanOne() {
        List<BackupVO> backups = List.of(Mockito.mock(BackupVO.class), Mockito.mock(BackupVO.class));
        Mockito.when(backupScheduleDaoMock.findById(1L)).thenReturn(backupScheduleVOMock);
        Mockito.when(backupScheduleVOMock.getMaxBackups()).thenReturn(2);
        Mockito.when(backupDaoMock.listByScheduleAndBackedUpStatus(1L)).thenReturn(backups);
        backupManager.deleteOldestBackupFromScheduleIfRequired(1L, 1L);
        Mockito.verify(backupManager, Mockito.never()).deleteExcessBackups(Mockito.anyList(), Mockito.anyInt(), Mockito.anyLong());
    }

    @Test
    public void deleteOldestBackupFromScheduleIfRequiredTestDeleteBackupsWhenRequired() {
        List<BackupVO> backups = List.of(Mockito.mock(BackupVO.class), Mockito.mock(BackupVO.class));
        Mockito.when(backupScheduleDaoMock.findById(1L)).thenReturn(backupScheduleVOMock);
        Mockito.when(backupScheduleVOMock.getMaxBackups()).thenReturn(1);
        Mockito.when(backupDaoMock.listByScheduleAndBackedUpStatus(1L)).thenReturn(backups);
        Mockito.doNothing().when(backupManager).deleteExcessBackups(Mockito.anyList(), Mockito.anyInt(), Mockito.anyLong());
        backupManager.deleteOldestBackupFromScheduleIfRequired(1L, 1L);
        Mockito.verify(backupManager).deleteExcessBackups(Mockito.anyList(), Mockito.anyInt(), Mockito.anyLong());
    }

    @Test
    public void deleteExcessBackupsTestEnsureBackupsAreDeletedWhenMethodIsCalled() {
        try (MockedStatic<ActionEventUtils> actionEventUtils = Mockito.mockStatic(ActionEventUtils.class)) {
            List<BackupVO> backups = List.of(Mockito.mock(BackupVO.class),
                    Mockito.mock(BackupVO.class),
                    Mockito.mock(BackupVO.class));

            Mockito.when(backups.get(0).getId()).thenReturn(1L);
            Mockito.when(backups.get(1).getId()).thenReturn(2L);
            Mockito.when(backups.get(0).getAccountId()).thenReturn(1L);
            Mockito.when(backups.get(1).getAccountId()).thenReturn(2L);
            Mockito.doReturn(true).when(backupManager).deleteBackup(Mockito.anyLong(), Mockito.eq(false));

            actionEventUtils.when(() -> ActionEventUtils.onStartedActionEvent(
                    Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.anyBoolean(), Mockito.anyInt())).thenReturn(1L);
            actionEventUtils.when(() -> ActionEventUtils.onCompletedActionEvent(
                    Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyInt())).thenReturn(2L);

            backupManager.deleteExcessBackups(backups, 2, 1L);
            Mockito.verify(backupManager, times(2)).deleteBackup(Mockito.anyLong(), Mockito.eq(false));
        }
    }
}
