package org.apache.cloudstack.api.response;

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.hostdevices.HostDevice;

import java.util.Date;

@EntityReference(value = HostDevice.class)
public class HostDeviceResponse extends BaseResponse {
    private String id;
    private String displayName;
    private String pciName;
    private String pciDomain;
    private String pciClass;
    private String pciBus;
    private String pciSlot;
    private String pciFunction;
    private String vendorId;
    private String deviceId;
    private String deviceTag;
    private Date created;
    private Date removed;
    private String state;
    private String type;
    private String instanceId;
    private String accountId;
    private String domainId;
    private String hostId;

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPciName() {
        return pciName;
    }

    public String getPciDomain() {
        return pciDomain;
    }

    public String getPciClass() {
        return pciClass;
    }

    public String getPciBus() {
        return pciBus;
    }

    public String getPciSlot() {
        return pciSlot;
    }

    public String getPciFunction() {
        return pciFunction;
    }

    public String getVendorId() {
        return vendorId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceTag() {
        return deviceTag;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public String getState() {
        return state;
    }

    public String getType() {
        return type;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getHostId() {
        return hostId;
    }

    public void setPciSlot(String pciSlot) {
        this.pciSlot = pciSlot;
    }

    public void setId(String uuid) {
        this.id = uuid;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setPciName(String pciName) {
        this.pciName = pciName;
    }

    public void setPciDomain(String pciDomain) {
        this.pciDomain = pciDomain;
    }

    public void setPciClass(String pciClass) {
        this.pciClass = pciClass;
    }

    public void setPciBus(String pciBus) {
        this.pciBus = pciBus;
    }

    public void setPciFunction(String pciFunction) {
        this.pciFunction = pciFunction;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setDeviceTag(String deviceTag) {
        this.deviceTag = deviceTag;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }
}
