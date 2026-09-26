/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.hostdevices;

import com.cloud.agent.AgentManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.host.dao.HostDao;
import com.cloud.hostdevices.HostDeviceVO;
import com.cloud.hostdevices.dao.HostDeviceDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.api.command.admin.hostdevices.ScanHostDevicesCmd;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class HostDevicesManagerImplTest {
    @Spy
    @InjectMocks
    private HostDevicesManagerImpl hostDevicesManager;
    @Mock
    AgentManager agentManager;
    @Mock
    private HostDao hostDao;
    @Mock
    private ClusterDao clusterDao;
    @Mock
    private HostDeviceDao hostDeviceDao;

    @Mock
    private Account mockAccount;
    @Mock
    private ClusterVO mockCluster;

    @Mock
    private ScanHostDevicesCmd mockScanHostDevices;

    @Before
    public void setUp() {
        Mockito.when(mockAccount.getType()).thenReturn(Account.Type.ADMIN);
        CallContext.register(Mockito.mock(User.class), mockAccount);
    }

    @After
    public void tearDown() throws Exception {
        CallContext.unregisterAll();
    }

    private long nextDeviceId = 1L;

    private HostDeviceVO createDevice(String pciName, String vendorId, String deviceId, HostDevice.State state, Long instanceId) {
        HostDeviceVO device = new HostDeviceVO();
        ReflectionTestUtils.setField(device, "id", nextDeviceId++);
        device.setPciName(pciName);
        device.setPciVendorId(vendorId);
        device.setPciDeviceId(deviceId);
        device.setState(state);
        device.setInstanceId(instanceId);
        device.setHostId(1L);
        return device;
    }

    @Test
    public void testScanHostDevicesCaseNotAdminThrowsPermissionDeniedException() {
        Mockito.when(mockAccount.getType()).thenReturn(Account.Type.NORMAL);

        Assert.assertThrows(PermissionDeniedException.class, () -> {
            hostDevicesManager.scanHostDevice(mockScanHostDevices);
        });
    }

    @Test
    public void testScanHostDevicesCaseNullHostListThrowsInvalidParameterValueException() {
        Mockito.doReturn(null).when(hostDevicesManager).getHostsListForDeviceScan(Mockito.any(), Mockito.any(), Mockito.any());
        Assert.assertThrows(InvalidParameterValueException.class, () -> {
            hostDevicesManager.scanHostDevice(mockScanHostDevices);
        });
        Mockito.verify(hostDevicesManager, Mockito.times(1)).getHostsListForDeviceScan(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testGetHostsListForDeviceScanHostNotFoundThrowsInvalidParameterValueException() {
        Mockito.when(hostDao.findUpAndRoutingHypervisorHostById(Mockito.anyLong(), Mockito.any(Hypervisor.HypervisorType.class))).thenReturn(null);
        Assert.assertThrows(InvalidParameterValueException.class, () -> {
            hostDevicesManager.getHostsListForDeviceScan(1L, 1L, 1L);
        });
        Mockito.verify(hostDao, Mockito.times(1)).findUpAndRoutingHypervisorHostById(Mockito.anyLong(), Mockito.any(Hypervisor.HypervisorType.class));
    }

    @Test
    public void testGetHostsListForDeviceScanClusterNotFoundInvalidParameterValueException() {
        Mockito.when(clusterDao.findById(Mockito.anyLong())).thenReturn(null);
        Assert.assertThrows(InvalidParameterValueException.class, () -> {
            hostDevicesManager.getHostsListForDeviceScan(1L, 1L, null);
        });
        Mockito.verify(clusterDao, Mockito.times(1)).findById(Mockito.anyLong());
    }

    @Test
    public void testGetHostsListForDeviceScanClusterNotKVMThrowsInvalidParameterValueException() {
        Mockito.when(mockCluster.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.VMware);
        Mockito.when(clusterDao.findById(Mockito.anyLong())).thenReturn(mockCluster);
        Assert.assertThrows(InvalidParameterValueException.class, () -> {
            hostDevicesManager.getHostsListForDeviceScan(1L, 1L, null);
        });
        Mockito.verify(clusterDao, Mockito.times(1)).findById(Mockito.anyLong());
    }

    @Test
    public void testGetHostsListForDeviceScanClusterAllNull() {
        Assert.assertNull(hostDevicesManager.getHostsListForDeviceScan(null, null, null));
    }

    @Test
    public void testValidateVmHostDevicesForStartAllDevicesAttachedDoesNotThrow() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getId()).thenReturn(10L);
        Mockito.when(hostDeviceDao.listHostDevicesByVmId(10L)).thenReturn(List.of(createDevice("pci_0000_01_00_0", "0x10de", "0x2230", HostDevice.State.Attached, 10L)));

        hostDevicesManager.validateVmHostDevicesForStart(vm);
    }

    @Test
    public void testValidateVmHostDevicesForStartMissingDeviceThrowsCloudRuntimeException() {
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getId()).thenReturn(10L);
        Mockito.when(hostDeviceDao.listHostDevicesByVmId(10L)).thenReturn(List.of(createDevice("pci_0000_01_00_0", "0x10de", "0x2230", HostDevice.State.Attached, 10L),
                createDevice("pci_0000_02_00_0", "0x10de", "0x2230", HostDevice.State.Missing, 10L)));

        Assert.assertThrows(CloudRuntimeException.class, () -> hostDevicesManager.validateVmHostDevicesForStart(vm));
    }

    @Test
    public void testReconcileHostDevicesNoRegisteredDevicesPersistsAllIncomingDevices() {
        List<HostDeviceVO> incomingDevices = List.of(createDevice("pci_0000_01_00_0", "0x10de", "0x2230", HostDevice.State.Disabled, null),
                createDevice("pci_0000_02_00_0", "0x15b3", "0x101d", HostDevice.State.Disabled, null));

        HostDevicesManagerImpl.DeviceScanReconciliation result = hostDevicesManager.reconcileHostDevices(new ArrayList<>(), incomingDevices, 1L);

        Mockito.verify(hostDeviceDao, Mockito.times(2)).persist(Mockito.any(HostDeviceVO.class));
        Assert.assertEquals(incomingDevices, result.getRegisteredDevices());
        Assert.assertTrue(result.getMissingDevices().isEmpty());
        Assert.assertTrue(result.getRecoveredDevices().isEmpty());
    }

    @Test
    public void testReconcileHostDevicesEmptyScanDoesNotChangeRegisteredDevices() {
        HostDeviceVO freeDevice = createDevice("pci_0000_01_00_0", "0x10de", "0x2230", HostDevice.State.Free, null);

        HostDevicesManagerImpl.DeviceScanReconciliation result = hostDevicesManager.reconcileHostDevices(List.of(freeDevice), new ArrayList<>(), 1L);

        Assert.assertEquals(HostDevice.State.Free, freeDevice.getState());
        Assert.assertTrue(result.getMissingDevices().isEmpty());
        Mockito.verify(hostDeviceDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any(HostDeviceVO.class));
        Mockito.verify(hostDeviceDao, Mockito.never()).persist(Mockito.any(HostDeviceVO.class));
    }

    @Test
    public void testReconcileHostDevicesNotReturnedDevicesAreMarkedAsMissing() {
        HostDeviceVO freeDevice = createDevice("pci_0000_01_00_0", "0x10de", "0x2230", HostDevice.State.Free, null);
        HostDeviceVO disabledDevice = createDevice("pci_0000_02_00_0", "0x10de", "0x2230", HostDevice.State.Disabled, null);
        HostDeviceVO attachedDevice = createDevice("pci_0000_03_00_0", "0x10de", "0x2230", HostDevice.State.Attached, 10L);
        HostDeviceVO returnedDevice = createDevice("pci_0000_04_00_0", "0x10de", "0x2230", HostDevice.State.Free, null);
        List<HostDeviceVO> incomingDevices = List.of(createDevice("pci_0000_04_00_0", "0x10de", "0x2230", HostDevice.State.Disabled, null));

        HostDevicesManagerImpl.DeviceScanReconciliation result = hostDevicesManager.reconcileHostDevices(List.of(freeDevice, disabledDevice, attachedDevice, returnedDevice), incomingDevices, 1L);

        Assert.assertEquals(List.of(freeDevice, disabledDevice, attachedDevice), result.getMissingDevices());
        Assert.assertEquals(HostDevice.State.Missing, freeDevice.getState());
        Assert.assertEquals(HostDevice.State.Missing, disabledDevice.getState());
        Assert.assertEquals(HostDevice.State.Missing, attachedDevice.getState());
        Assert.assertEquals(Long.valueOf(10L), attachedDevice.getInstanceId());
        Assert.assertEquals(HostDevice.State.Free, returnedDevice.getState());
        Assert.assertTrue(result.getRegisteredDevices().isEmpty());
    }

    @Test
    public void testReconcileHostDevicesIgnoredStatesAreNotMarkedAsMissing() {
        HostDeviceVO needsCleanupDevice = createDevice("pci_0000_01_00_0", "0x10de", "0x2230", HostDevice.State.NeedsCleanup, null);
        HostDeviceVO inMaintenanceDevice = createDevice("pci_0000_02_00_0", "0x10de", "0x2230", HostDevice.State.HostInMaintenance, null);
        HostDeviceVO missingDevice = createDevice("pci_0000_03_00_0", "0x10de", "0x2230", HostDevice.State.Missing, null);
        List<HostDeviceVO> incomingDevices = List.of(createDevice("pci_0000_04_00_0", "0x10de", "0x2230", HostDevice.State.Disabled, null));

        HostDevicesManagerImpl.DeviceScanReconciliation result = hostDevicesManager.reconcileHostDevices(List.of(needsCleanupDevice, inMaintenanceDevice, missingDevice), incomingDevices, 1L);

        Assert.assertTrue(result.getMissingDevices().isEmpty());
        Assert.assertEquals(HostDevice.State.NeedsCleanup, needsCleanupDevice.getState());
        Assert.assertEquals(HostDevice.State.HostInMaintenance, inMaintenanceDevice.getState());
        Assert.assertEquals(HostDevice.State.Missing, missingDevice.getState());
    }

    @Test
    public void testReconcileHostDevicesReturnedMissingDevicesAreRecovered() {
        HostDeviceVO missingAttachedDevice = createDevice("pci_0000_01_00_0", "0x10de", "0x2230", HostDevice.State.Missing, 10L);
        HostDeviceVO missingFreeDevice = createDevice("pci_0000_02_00_0", "0x10de", "0x2230", HostDevice.State.Missing, null);
        List<HostDeviceVO> incomingDevices = List.of(createDevice("pci_0000_01_00_0", "0x10de", "0x2230", HostDevice.State.Disabled, null),
                createDevice("pci_0000_02_00_0", "0x10de", "0x2230", HostDevice.State.Disabled, null));

        HostDevicesManagerImpl.DeviceScanReconciliation result = hostDevicesManager.reconcileHostDevices(List.of(missingAttachedDevice, missingFreeDevice), incomingDevices, 1L);

        Assert.assertEquals(List.of(missingAttachedDevice, missingFreeDevice), result.getRecoveredDevices());
        Assert.assertEquals(HostDevice.State.Attached, missingAttachedDevice.getState());
        Assert.assertEquals(HostDevice.State.Disabled, missingFreeDevice.getState());
        Assert.assertTrue(result.getRegisteredDevices().isEmpty());
        Mockito.verify(hostDeviceDao, Mockito.never()).persist(Mockito.any(HostDeviceVO.class));
    }

    @Test
    public void testReconcileHostDevicesReplacedDeviceIsMarkedAsMissingAndNewDeviceIsRegistered() {
        HostDeviceVO oldDevice = createDevice("pci_0000_01_00_0", "0x10de", "0x2230", HostDevice.State.Attached, 10L);
        HostDeviceVO newDevice = createDevice("pci_0000_01_00_0", "0x10de", "0x2236", HostDevice.State.Disabled, null);

        HostDevicesManagerImpl.DeviceScanReconciliation result = hostDevicesManager.reconcileHostDevices(List.of(oldDevice), List.of(newDevice), 1L);

        Assert.assertEquals(List.of(oldDevice), result.getMissingDevices());
        Assert.assertEquals(HostDevice.State.Missing, oldDevice.getState());
        Assert.assertEquals(Long.valueOf(10L), oldDevice.getInstanceId());
        Assert.assertEquals(List.of(newDevice), result.getRegisteredDevices());
        Mockito.verify(hostDeviceDao).persist(newDevice);
    }
}