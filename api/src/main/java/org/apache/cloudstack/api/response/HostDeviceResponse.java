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
import org.apache.cloudstack.hostdevices.HostDevice;

import java.util.Date;

@EntityReference(value = HostDevice.class)
public class HostDeviceResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the host device", since = "24.0.0")
    private String id;

    @SerializedName(ApiConstants.DISPLAY_NAME)
    @Param(description = "the display name of the host device", since = "24.0.0")
    private String displayName;

    @SerializedName(ApiConstants.PCI_NAME)
    @Param(description = "the unique PCI identifier of the host device", since = "24.0.0")
    private String pciName;

    @SerializedName(ApiConstants.PCI_DOMAIN)
    @Param(description = "the PCI domain of the host device", since = "24.0.0")
    private String pciDomain;

    @SerializedName(ApiConstants.PCI_CLASS)
    @Param(description = "the PCI class of the host device", since = "24.0.0")
    private String pciClass;

    @SerializedName(ApiConstants.PCI_BUS)
    @Param(description = "the PCI bus of the host device", since = "24.0.0")
    private String pciBus;

    @SerializedName(ApiConstants.PCI_SLOT)
    @Param(description = "the PCI slot of the host device", since = "24.0.0")
    private String pciSlot;

    @SerializedName(ApiConstants.PCI_FUNCTION)
    @Param(description = "the PCI function of the host device", since = "24.0.0")
    private String pciFunction;

    @SerializedName(ApiConstants.VENDOR_ID)
    @Param(description = "the vendor identifier of the host device", since = "24.0.0")
    private String vendorId;

    @SerializedName(ApiConstants.DEVICE_ID)
    @Param(description = "the device identifier of the host device, set by its vendor", since = "24.0.0")
    private String deviceId;

    @SerializedName(ApiConstants.DEVICE_TAG)
    @Param(description = "the device tag used to match the host device against device offerings", since = "24.0.0")
    private String deviceTag;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date the host device was registered", since = "24.0.0")
    private Date created;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "the date the host device was removed", since = "24.0.0")
    private Date removed;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the host device", since = "24.0.0")
    private String state;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "the type of the host device", since = "24.0.0")
    private String type;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "the ID of the Instance the host device is attached to", since = "24.0.0")
    private String virtualMachineId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_NAME)
    @Param(description = "the name of the Instance the host device is attached to", since = "24.0.0")
    private String virtualMachineName;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "the ID of the account the host device is allocated to", since = "24.0.0")
    private String accountId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the name of the account the host device is allocated to", since = "24.0.0")
    private String account;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the ID of the domain the host device is allocated to", since = "24.0.0")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the name of the domain the host device is allocated to", since = "24.0.0")
    private String domain;

    @SerializedName(ApiConstants.HOST_ID)
    @Param(description = "the ID of the host the device belongs to", since = "24.0.0")
    private String hostId;

    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "the name of the host the device belongs to", since = "24.0.0")
    private String hostName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPciName() {
        return pciName;
    }

    public void setPciName(String pciName) {
        this.pciName = pciName;
    }

    public String getPciDomain() {
        return pciDomain;
    }

    public void setPciDomain(String pciDomain) {
        this.pciDomain = pciDomain;
    }

    public String getPciClass() {
        return pciClass;
    }

    public void setPciClass(String pciClass) {
        this.pciClass = pciClass;
    }

    public String getPciBus() {
        return pciBus;
    }

    public void setPciBus(String pciBus) {
        this.pciBus = pciBus;
    }

    public String getPciSlot() {
        return pciSlot;
    }

    public void setPciSlot(String pciSlot) {
        this.pciSlot = pciSlot;
    }

    public String getPciFunction() {
        return pciFunction;
    }

    public void setPciFunction(String pciFunction) {
        this.pciFunction = pciFunction;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceTag() {
        return deviceTag;
    }

    public void setDeviceTag(String deviceTag) {
        this.deviceTag = deviceTag;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public String getVirtualMachineName() {
        return virtualMachineName;
    }

    public void setVirtualMachineName(String virtualMachineName) {
        this.virtualMachineName = virtualMachineName;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String accountName) {
        this.account = accountName;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getHostname() {
        return hostName;
    }

    public void setHostname(String hostName) {
        this.hostName = hostName;
    }
}
