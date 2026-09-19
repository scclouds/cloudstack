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

package com.cloud.hostdevices;

import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "vm_instance_device_offerings")
public class VMInstanceDeviceOfferingsVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "vm_instance_id")
    private Long virtualMachineId;
    @Column(name = "device_offering_id")
    private Long deviceOfferingId;

    public VMInstanceDeviceOfferingsVO() {
    }

    public VMInstanceDeviceOfferingsVO(Long virtualMachineId, Long deviceOfferingId) {
        this.virtualMachineId = virtualMachineId;
        this.deviceOfferingId = deviceOfferingId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(Long virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public Long getDeviceOfferingId() {
        return deviceOfferingId;
    }

    public void setDeviceOfferingId(Long deviceOfferingId) {
        this.deviceOfferingId = deviceOfferingId;
    }

    @Override
    public String toString() {
        return String.format("VM instance device offering %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "id", "virtualMachineId", "deviceOfferingId"));
    }
}
