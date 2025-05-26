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
package com.cloud.kubernetes.cluster;


import com.cloud.exception.InvalidParameterValueException;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.vm.VmDetailConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.CONTROL;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesServiceHelperImplTest {
    @Mock
    KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Mock
    KubernetesClusterDao kubernetesClusterDao;

    @Mock
    private ServiceOfferingDao serviceOfferingDao;
    @Mock
    private ServiceOfferingVO workerServiceOffering;
    @Mock
    private ServiceOfferingVO controlServiceOffering;

    private static final String workerNodesOfferingId = UUID.randomUUID().toString();
    private static final String controlNodesOfferingId = UUID.randomUUID().toString();
    private static final Long workerOfferingId = 1L;
    private static final Long controlOfferingId = 2L;

    @InjectMocks
    KubernetesServiceHelperImpl kubernetesServiceHelper = new KubernetesServiceHelperImpl();

    @Test
    public void testCheckVmCanBeDestroyedNotCKSNode() {
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vm.getUserVmType()).thenReturn("");
        kubernetesServiceHelper.checkVmCanBeDestroyed(vm);
        Mockito.verify(kubernetesClusterVmMapDao, Mockito.never()).findByVmId(Mockito.anyLong());
    }

    @Test
    public void testCheckVmCanBeDestroyedNotInCluster() {
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(vm.getUserVmType()).thenReturn(UserVmManager.CKS_NODE);
        Mockito.when(kubernetesClusterVmMapDao.findByVmId(1L)).thenReturn(null);
        kubernetesServiceHelper.checkVmCanBeDestroyed(vm);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCheckVmCanBeDestroyedInCloudManagedCluster() {
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(vm.getUserVmType()).thenReturn(UserVmManager.CKS_NODE);
        KubernetesClusterVmMapVO map = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(map.getClusterId()).thenReturn(1L);
        Mockito.when(kubernetesClusterVmMapDao.findByVmId(1L)).thenReturn(map);
        KubernetesClusterVO kubernetesCluster = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(kubernetesClusterDao.findById(1L)).thenReturn(kubernetesCluster);
        Mockito.when(kubernetesCluster.getClusterType()).thenReturn(KubernetesCluster.ClusterType.CloudManaged);
        kubernetesServiceHelper.checkVmCanBeDestroyed(vm);
    }

    @Test
    public void testCheckVmCanBeDestroyedInExternalManagedCluster() {
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(vm.getUserVmType()).thenReturn(UserVmManager.CKS_NODE);
        KubernetesClusterVmMapVO map = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(map.getClusterId()).thenReturn(1L);
        Mockito.when(kubernetesClusterVmMapDao.findByVmId(1L)).thenReturn(map);
        KubernetesClusterVO kubernetesCluster = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(kubernetesClusterDao.findById(1L)).thenReturn(kubernetesCluster);
        Mockito.when(kubernetesCluster.getClusterType()).thenReturn(KubernetesCluster.ClusterType.ExternalManaged);
        kubernetesServiceHelper.checkVmCanBeDestroyed(vm);
    }

    @Before
    public void setUp() {
        kubernetesServiceHelper.serviceOfferingDao = serviceOfferingDao;
        Mockito.when(serviceOfferingDao.findByUuid(workerNodesOfferingId)).thenReturn(workerServiceOffering);
        Mockito.when(serviceOfferingDao.findByUuid(controlNodesOfferingId)).thenReturn(controlServiceOffering);
        Mockito.when(workerServiceOffering.getId()).thenReturn(workerOfferingId);
        Mockito.when(controlServiceOffering.getId()).thenReturn(controlOfferingId);
    }

    @Test
    public void testIsValidNodeTypeEmptyNodeType() {
        Assert.assertFalse(kubernetesServiceHelper.isValidNodeType(null));
    }

    @Test
    public void testIsValidNodeTypeInvalidNodeType() {
        String nodeType = "invalidNodeType";
        Assert.assertFalse(kubernetesServiceHelper.isValidNodeType(nodeType));
    }

    @Test
    public void testIsValidNodeTypeValidNodeTypeLowercase() {
        String nodeType = KubernetesServiceHelper.KubernetesClusterNodeType.WORKER.name().toLowerCase();
        Assert.assertTrue(kubernetesServiceHelper.isValidNodeType(nodeType));
    }

    private Map<String, String> createMapEntry(KubernetesServiceHelper.KubernetesClusterNodeType nodeType,
                                               String nodeTypeOfferingUuid) {
        Map<String, String> map = new HashMap<>();
        map.put(VmDetailConstants.CKS_NODE_TYPE, nodeType.name().toLowerCase());
        map.put(VmDetailConstants.OFFERING, nodeTypeOfferingUuid);
        return map;
    }

    @Test
    public void testNodeOfferingMap() {
        Map<String, Map<String, String>> serviceOfferingNodeTypeMap = new HashMap<>();
        Map<String, String> firstMap = createMapEntry(WORKER, workerNodesOfferingId);
        Map<String, String> secondMap = createMapEntry(CONTROL, controlNodesOfferingId);
        serviceOfferingNodeTypeMap.put("map1", firstMap);
        serviceOfferingNodeTypeMap.put("map2", secondMap);
        Map<String, Long> map = kubernetesServiceHelper.getServiceOfferingNodeTypeMap(serviceOfferingNodeTypeMap);
        Assert.assertNotNull(map);
        Assert.assertEquals(2, map.size());
        Assert.assertTrue(map.containsKey(WORKER.name()) && map.containsKey(CONTROL.name()));
        Assert.assertEquals(workerOfferingId, map.get(WORKER.name()));
        Assert.assertEquals(controlOfferingId, map.get(CONTROL.name()));
    }

    @Test
    public void testNodeOfferingMapNullMap() {
        Map<String, Long> map = kubernetesServiceHelper.getServiceOfferingNodeTypeMap(null);
        Assert.assertTrue(map.isEmpty());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckNodeTypeOfferingEntryCompletenessInvalidParameters() {
        kubernetesServiceHelper.checkNodeTypeOfferingEntryCompleteness(WORKER.name(), null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckNodeTypeOfferingEntryValuesInvalidNodeType() {
        String invalidNodeType = "invalidNodeTypeName";
        kubernetesServiceHelper.checkNodeTypeOfferingEntryValues(invalidNodeType, workerServiceOffering, workerNodesOfferingId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckNodeTypeOfferingEntryValuesEmptyOffering() {
        String nodeType = WORKER.name();
        kubernetesServiceHelper.checkNodeTypeOfferingEntryValues(nodeType, null, workerNodesOfferingId);
    }
}
