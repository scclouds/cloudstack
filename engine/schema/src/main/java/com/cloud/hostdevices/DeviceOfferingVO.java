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

import org.apache.cloudstack.hostdevices.DeviceOffering;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "device_offerings")
public class DeviceOfferingVO implements DeviceOffering {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    @Column(name = "public")
    private Boolean isPublic;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "zone_id")
    private Long zoneId;

    public DeviceOfferingVO() {
    }

    public DeviceOfferingVO(String name, String description, Long domainId, Long zoneId) {
        this.name = name;
        this.description = description;
        this.domainId = domainId;
        this.zoneId = zoneId;
        this.uuid = UUID.randomUUID().toString();
        this.state = State.Active;
        this.isPublic = domainId == null && zoneId == null;
    }

    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public State getState() {
        return state;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    @Override
    public String toString() {
        return String.format("Device offering %s", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, "id", "uuid", "name", "state", "domainId", "zoneId"));
    }
}
