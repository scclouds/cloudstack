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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.vm.VmDetailConstants;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.event.EventTypes;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.kubernetes.version.KubernetesVersionEventTypes;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

@Component
public class KubernetesServiceHelperImpl extends AdapterBase implements KubernetesServiceHelper, Configurable {
    private static final Logger logger = LogManager.getLogger(KubernetesServiceHelperImpl.class);

    @Inject
    private KubernetesClusterDao kubernetesClusterDao;
    @Inject
    private KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Inject
    protected ServiceOfferingDao serviceOfferingDao;

    protected void setEventTypeEntityDetails(Class<?> eventTypeDefinedClass, Class<?> entityClass) {
        Field[] declaredFields = eventTypeDefinedClass.getDeclaredFields();
        for (Field field : declaredFields) {
            int modifiers = field.getModifiers();
            if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers)) {
                continue;
            }
            try {
                Object value = field.get(null);
                if (ObjectUtils.allNotNull(value, value.toString())) {
                    EventTypes.addEntityEventDetail(value.toString(), entityClass);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public ControlledEntity findByUuid(String uuid) {
        return kubernetesClusterDao.findByUuid(uuid);
    }

    @Override
    public ControlledEntity findByVmId(long vmId) {
        KubernetesClusterVmMapVO clusterVmMapVO = kubernetesClusterVmMapDao.getClusterMapFromVmId(vmId);
        if (Objects.isNull(clusterVmMapVO)) {
            return null;
        }
        return kubernetesClusterDao.findById(clusterVmMapVO.getClusterId());
    }

    @Override
    public void checkVmCanBeDestroyed(UserVm userVm) {
        if (!UserVmManager.CKS_NODE.equals(userVm.getUserVmType())) {
            return;
        }
        KubernetesClusterVmMapVO vmMapVO = kubernetesClusterVmMapDao.findByVmId(userVm.getId());
        if (vmMapVO == null) {
            return;
        }
        logger.error(String.format("VM ID: %s is a part of Kubernetes cluster ID: %d", userVm.getId(), vmMapVO.getClusterId()));
        KubernetesCluster kubernetesCluster = kubernetesClusterDao.findById(vmMapVO.getClusterId());
        String msg = "Instance is a part of a Kubernetes cluster";
        if (kubernetesCluster != null) {
            if (KubernetesCluster.ClusterType.ExternalManaged.equals(kubernetesCluster.getClusterType())) {
                return;
            }
            msg += String.format(": %s", kubernetesCluster.getName());
        }
        msg += ". Use Instance delete option from Kubernetes cluster details or scale API for " +
                "Kubernetes clusters with 'nodeids' to destroy the instance.";
        throw new CloudRuntimeException(msg);
    }

    @Override
    public boolean isValidNodeType(String nodeType) {
        logger.debug("Verifying if node type [{}] is valid. Valid Kubernetes cluster node types are: {}", nodeType,
                List.of(KubernetesClusterNodeType.CONTROL.name().toLowerCase(), KubernetesClusterNodeType.WORKER.name().toLowerCase()));
        if (StringUtils.isBlank(nodeType)) {
            logger.trace("Node type is blank and, therefore, invalid.");
            return false;
        }
        try {
            KubernetesClusterNodeType.valueOf(nodeType.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            logger.trace("Node type is not one of the valid Kubernetes cluster node types and, therefore, invalid.");
            return false;
        }
    }

    @Override
    public Map<String, Long> getServiceOfferingNodeTypeMap(Map<String, Map<String, String>> serviceOfferingNodeTypeMap) {
        logger.debug("Parsing [nodeofferings] API parameter value into a mapping of node types to their respective service offering ID.");
        Map<String, Long> mapping = new HashMap<>();
        if (MapUtils.isNotEmpty(serviceOfferingNodeTypeMap)) {
            for (Map<String, String> entry : serviceOfferingNodeTypeMap.values()) {
                processNodeTypeOfferingEntryAndAddToMappingIfValid(entry, mapping);
            }
        }
        return mapping;
    }

    protected void checkNodeTypeOfferingEntryCompleteness(String nodeTypeStr, String serviceOfferingUuid) {
        if (StringUtils.isAnyEmpty(nodeTypeStr, serviceOfferingUuid)) {
            String error = String.format("Incomplete node type to Service Offering ID mapping: [%s -> %s]", nodeTypeStr, serviceOfferingUuid);
            logger.error(error);
            throw new InvalidParameterValueException(error);
        }
    }

    protected void checkNodeTypeOfferingEntryValues(String nodeTypeStr, ServiceOffering serviceOffering, String serviceOfferingUuid) {
        if (!isValidNodeType(nodeTypeStr)) {
            String error = String.format("The provided value [%s] for node type is invalid", nodeTypeStr);
            logger.error(error);
            throw new InvalidParameterValueException(error);
        }
        if (serviceOffering == null) {
            String error = String.format("Cannot find a service offering with ID %s", serviceOfferingUuid);
            logger.error(error);
            throw new InvalidParameterValueException(error);
        }
    }

    protected void addNodeTypeOfferingEntry(String nodeTypeStr, ServiceOffering serviceOffering, Map<String, Long> mapping) {
        logger.debug("Configuring Kubernetes cluster [{}] VMs to use the [{} - ID: {}] compute offering", nodeTypeStr, serviceOffering.getName(), serviceOffering.getUuid());
        mapping.put(nodeTypeStr.toUpperCase(), serviceOffering.getId());
    }

    protected void processNodeTypeOfferingEntryAndAddToMappingIfValid(Map<String, String> entry, Map<String, Long> mapping) {
        if (MapUtils.isEmpty(entry)) {
            return;
        }
        String nodeTypeStr = entry.get(VmDetailConstants.CKS_NODE_TYPE);
        String serviceOfferingUuid = entry.get(VmDetailConstants.OFFERING);
        logger.trace("Processing mapping entry: [{} -> {}]", nodeTypeStr, serviceOfferingUuid);
        checkNodeTypeOfferingEntryCompleteness(nodeTypeStr, serviceOfferingUuid);

        ServiceOffering serviceOffering = serviceOfferingDao.findByUuid(serviceOfferingUuid);
        checkNodeTypeOfferingEntryValues(nodeTypeStr, serviceOffering, serviceOfferingUuid);

        addNodeTypeOfferingEntry(nodeTypeStr, serviceOffering, mapping);
    }

    @Override
    public String getConfigComponentName() {
        return KubernetesServiceHelper.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{};
    }

    @Override
    public boolean start() {
        setEventTypeEntityDetails(KubernetesClusterEventTypes.class, KubernetesCluster.class);
        setEventTypeEntityDetails(KubernetesVersionEventTypes.class, KubernetesSupportedVersion.class);
        ApiCommandResourceType.setClassMapping(ApiCommandResourceType.KubernetesCluster, KubernetesCluster.class);
        ApiCommandResourceType.setClassMapping(ApiCommandResourceType.KubernetesSupportedVersion,
                KubernetesSupportedVersion.class);
        return super.start();
    }
}
