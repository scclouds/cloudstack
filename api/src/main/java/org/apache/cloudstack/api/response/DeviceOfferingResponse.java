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

package org.apache.cloudstack.api.response;

import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import com.cloud.serializer.Param;
import org.apache.cloudstack.hostdevices.DeviceOffering;

import java.util.Date;
import java.util.Map;

@EntityReference(value = DeviceOffering.class)
public class DeviceOfferingResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the device offering", since = "4.24.0")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the device offering", since = "4.24.0")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the device offering", since = "4.24.0")
    private String description;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the device offering", since = "4.24.0")
    private String state;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the ID of the domain the device offering is dedicated to", since = "4.24.0")
    private String domainId;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the ID of the zone the device offering is dedicated to", since = "4.24.0")
    private String zoneId;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date the device offering was created", since = "4.24.0")
    private Date created;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "the date the device offering was removed", since = "4.24.0")
    private Date removed;

    @SerializedName(ApiConstants.IS_PUBLIC)
    @Param(description = "true if the device offering is available for all domains and zones", since = "4.24.0")
    private Boolean isPublic;

    @SerializedName(ApiConstants.DEVICE_TAGS)
    @Param(description = "the device tags of the device offering", since = "4.24.0")
    private Map<String, Integer> deviceTags;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Map<String, Integer> getDeviceTags() {
        return deviceTags;
    }

    public void setDeviceTags(Map<String, Integer> deviceTags) {
        this.deviceTags = deviceTags;
    }
}
