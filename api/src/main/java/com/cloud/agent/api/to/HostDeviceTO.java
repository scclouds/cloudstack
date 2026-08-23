package com.cloud.agent.api.to;

import org.apache.cloudstack.hostdevices.HostDevice;
import org.apache.cloudstack.utils.libvirt.model.LibvirtDevice;
import org.apache.cloudstack.utils.libvirt.model.PciDevice;

import java.util.Date;

public class HostDeviceTO {
    private Long id;
    private String uuid;
    private String displayName;
    private String pciName;
    private String pciClass;
    private String pciDomain;
    private String pciBus;
    private String pciSlot;
    private String pciFunction;
    private String pciVendorId;
    private String pciDeviceId;
    private String deviceTag;
    private Date created;
    private Date removed;
    private HostDevice.State state;
    private HostDevice.Type type;
    private Long instanceId;
    private Long accountId;
    private Long domainId;
    private Long hostId;

    public HostDeviceTO(HostDevice hostDevice) {
        this.id = hostDevice.getId();
        this.uuid = hostDevice.getUuid();
        this.displayName = hostDevice.getDisplayName();
        this.pciName = hostDevice.getPciName();
        this.pciClass = hostDevice.getPciClass();
        this.pciDomain = hostDevice.getPciDomain();
        this.pciBus = hostDevice.getPciBus();
        this.pciSlot = hostDevice.getPciSlot();
        this.pciFunction = hostDevice.getPciFunction();
        this.pciVendorId = hostDevice.getPciVendorId();
        this.pciDeviceId = hostDevice.getPciDeviceId();
        this.deviceTag = hostDevice.getDeviceTag();
        this.created = hostDevice.getCreated();
        this.removed = hostDevice.getRemoved();
        this.state = hostDevice.getState();
        this.type = hostDevice.getType();
        this.instanceId = hostDevice.getInstanceId();
        this.accountId = hostDevice.getAccountId();
        this.domainId = hostDevice.getDomainId();
        this.hostId = hostDevice.getHostId();
    }

    public LibvirtDevice toLibvirtDevice() {
//        switch(this.type.toString()) {
//            case "pci":
//
//            default:
//                throw new IllegalArgumentException("Unsupported device type: " + type);
//        }
        return new PciDevice(pciDomain, pciBus, pciSlot, pciFunction);
    }

    public String getPciFunction() {
        return pciFunction;
    }

    public void setPciFunction(String pciFunction) {
        this.pciFunction = pciFunction;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getPciClass() {
        return pciClass;
    }

    public void setPciClass(String pciClass) {
        this.pciClass = pciClass;
    }

    public String getPciDomain() {
        return pciDomain;
    }

    public void setPciDomain(String pciDomain) {
        this.pciDomain = pciDomain;
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

    public String getPciVendorId() {
        return pciVendorId;
    }

    public void setPciVendorId(String pciVendorId) {
        this.pciVendorId = pciVendorId;
    }

    public String getPciDeviceId() {
        return pciDeviceId;
    }

    public void setPciDeviceId(String pciDeviceId) {
        this.pciDeviceId = pciDeviceId;
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

    public HostDevice.State getState() {
        return state;
    }

    public void setState(HostDevice.State state) {
        this.state = state;
    }

    public HostDevice.Type getType() {
        return type;
    }

    public void setType(HostDevice.Type type) {
        this.type = type;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }
}
