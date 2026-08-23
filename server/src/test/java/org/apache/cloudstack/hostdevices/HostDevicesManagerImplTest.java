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
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.user.Account;
import com.cloud.user.User;
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

    @Test
    public void testScanHostDevicesCaseNotAdminThrowsInvalidParameterValueException() {
        Mockito.when(mockAccount.getType()).thenReturn(Account.Type.NORMAL);

        Assert.assertThrows(InvalidParameterValueException.class, () -> {
            hostDevicesManager.scanHostDevice(mockScanHostDevices);
        });
    }

    @Test
    public void testScanHostDevicesCaseNullHostListThrowsInvalidParameterValueException() {
        Mockito.doReturn(null).when(hostDevicesManager).getHostsListForDeviceScan(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
        Assert.assertThrows(InvalidParameterValueException.class, () -> {
            hostDevicesManager.scanHostDevice(mockScanHostDevices);
        });
        Mockito.verify(hostDevicesManager, Mockito.times(1)).getHostsListForDeviceScan(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    public void testGetHostsListForDeviceScanHostNotFoundThrowsInvalidParameterValueException() {
        Mockito.when(hostDao.findUpAndRoutingHypervisorHostById(Mockito.anyLong(), Mockito.any(Hypervisor.HypervisorType.class))).thenReturn(null);
        Assert.assertThrows(InvalidParameterValueException.class, () -> {
            hostDevicesManager.getHostsListForDeviceScan(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong());
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
}