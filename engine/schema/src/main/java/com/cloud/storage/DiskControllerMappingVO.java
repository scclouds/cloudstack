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

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.db.GenericDao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "disk_controller_mapping")
public class DiskControllerMappingVO implements DiskControllerMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", nullable = false)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "controller_reference", nullable = false)
    private String controllerReference;

    @Column(name = "bus_name", nullable = false)
    private String busName;

    @Column(name = "hypervisor", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private HypervisorType hypervisor;

    @Column(name = "max_device_count")
    private Integer maxDeviceCount = null;

    @Column(name = "max_controller_count")
    private Integer maxControllerCount = null;

    @Column(name = "vmdk_adapter_type")
    private String vmdkAdapterType = null;

    @Column(name = "min_hardware_version")
    private String minHardwareVersion = null;

    @Column(name = GenericDao.CREATED_COLUMN, nullable = false)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed = null;

    public DiskControllerMappingVO() {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getControllerReference() {
        return controllerReference;
    }

    @Override
    public String getBusName() {
        return busName;
    }

    @Override
    public HypervisorType getHypervisor() {
        return hypervisor;
    }

    @Override
    public Integer getMaxDeviceCount() {
        return maxDeviceCount;
    }

    @Override
    public Integer getMaxControllerCount() {
        return maxControllerCount;
    }

    @Override
    public String getVmdkAdapterType() {
        return vmdkAdapterType;
    }

    @Override
    public String getMinHardwareVersion() {
        return minHardwareVersion;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHypervisor(HypervisorType hypervisor) {
        this.hypervisor = hypervisor;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DiskControllerMappingVO)) {
            return false;
        }
        DiskControllerMappingVO that = (DiskControllerMappingVO) obj;
        return controllerReference.equals(that.getControllerReference());
    }
}
