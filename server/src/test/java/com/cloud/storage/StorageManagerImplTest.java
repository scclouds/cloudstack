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
package com.cloud.storage;

import com.cloud.storage.dao.VolumeDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.host.Host;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class StorageManagerImplTest {

    @Spy
    @InjectMocks
    private StorageManagerImpl storageManagerImpl;

    @Mock
    private StoragePoolVO storagePoolVOMock;

    @Mock
    private VolumeVO volume1VOMock;

    @Mock
    private VolumeVO volume2VOMock;

    @Mock
    private VMInstanceVO vmInstanceVOMock;

    @Mock
    private VMInstanceDao vmInstanceDaoMock;

    @Mock
    private VolumeDao volumeDaoMock;

    @Test
    public void createLocalStoragePoolName() {
        String hostMockName = "host1";
        executeCreateLocalStoragePoolNameForHostName(hostMockName);
    }

    @Test
    public void createLocalStoragePoolNameUsingHostNameWithSpaces() {
        String hostMockName = "      hostNameWithSpaces      ";
        executeCreateLocalStoragePoolNameForHostName(hostMockName);
    }

    private void executeCreateLocalStoragePoolNameForHostName(String hostMockName) {
        String firstBlockUuid = "dsdsh665";

        String expectedLocalStorageName = hostMockName.trim() + "-local-" + firstBlockUuid;

        Host hostMock = Mockito.mock(Host.class);
        StoragePoolInfo storagePoolInfoMock = Mockito.mock(StoragePoolInfo.class);

        Mockito.when(hostMock.getName()).thenReturn(hostMockName);
        Mockito.when(storagePoolInfoMock.getUuid()).thenReturn(firstBlockUuid + "-213151-df21ef333d-2d33f1");

        String localStoragePoolName = storageManagerImpl.createLocalStoragePoolName(hostMock, storagePoolInfoMock);
        Assert.assertEquals(expectedLocalStorageName, localStoragePoolName);
    }

    @Test
    public void getStoragePoolNonDestroyedVolumesLogTestNonDestroyedVolumesReturnLog() {
        Mockito.doReturn(1L).when(storagePoolVOMock).getId();
        Mockito.doReturn(1L).when(volume1VOMock).getInstanceId();
        Mockito.doReturn("786633d1-a942-4374-9d56-322dd4b0d202").when(volume1VOMock).getUuid();
        Mockito.doReturn(1L).when(volume2VOMock).getInstanceId();
        Mockito.doReturn("ffb46333-e983-4c21-b5f0-51c5877a3805").when(volume2VOMock).getUuid();
        Mockito.doReturn("58760044-928f-4c4e-9fef-d0e48423595e").when(vmInstanceVOMock).getUuid();

        Mockito.when(volumeDaoMock.findByPoolId(storagePoolVOMock.getId(), null)).thenReturn(List.of(volume1VOMock, volume2VOMock));
        Mockito.doReturn(vmInstanceVOMock).when(vmInstanceDaoMock).findById(Mockito.anyLong());

        String log = storageManagerImpl.getStoragePoolNonDestroyedVolumesLog(storagePoolVOMock.getId());
        String expected = String.format("[Volume [%s] (attached to VM [%s]), Volume [%s] (attached to VM [%s])]", volume1VOMock.getUuid(), vmInstanceVOMock.getUuid(), volume2VOMock.getUuid(), vmInstanceVOMock.getUuid());

        Assert.assertEquals(expected, log);
    }
}
