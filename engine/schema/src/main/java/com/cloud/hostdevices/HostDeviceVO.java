package com.cloud.hostdevices;

import com.cloud.user.Account;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.hostdevices.HostDevice;
import org.apache.cloudstack.utils.libvirt.model.LibvirtDevice;
import org.apache.cloudstack.utils.libvirt.model.PciDevice;

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
@Table(name = "host_pci_devices")
public class HostDeviceVO implements HostDevice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "pci_name")
    private String pciName;

    @Column(name = "pci_class")
    private String pciClass;

    @Column(name = "pci_domain")
    private String pciDomain;

    @Column(name = "pci_bus")
    private String pciBus;

    @Column(name = "pci_slot")
    private String pciSlot;

    @Column(name = "pci_function")
    private String pciFunction;

    @Column(name = "pci_vendor_id")
    private String pciVendorId;

    @Column(name = "pci_device_id")
    private String pciDeviceId;

    @Column(name = "device_tag")
    private String deviceTag;

    @Column(name = "created")
    @Temporal(value = TemporalType.DATE)
    private Date created;

    @Column(name = "removed")
    @Temporal(value = TemporalType.DATE)
    private Date removed;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private HostDevice.State state;

    @Column(name = "type")
    @Enumerated(value = EnumType.STRING)
    private HostDevice.Type type;

    @Column(name = "instance_id")
    private Long instanceId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "host_id")
    private Long hostId;

    public HostDeviceVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public HostDeviceVO(PciDevice device, Long hostId) {
        this();
        this.pciName = device.getName();
        this.pciClass = device.getClassCode();
        this.pciDomain = device.getDomain();
        this.pciBus = device.getBus();
        this.pciSlot = device.getSlot();
        this.pciFunction = device.getFunction();
        this.pciVendorId = device.getVendorId();
        this.pciDeviceId = device.getProductId();
        this.state = State.Disabled;
        this.type = HostDevice.Type.getFromClassCode(device.getClassCode());
        this.displayName = String.format("%s - %s", device.getProductName(), device.getVendorName());
        this.deviceTag = this.type.toString();
        this.hostId = hostId;
    }

    public static HostDeviceVO mapLibvirtDevice(LibvirtDevice libvirtDevice, Long hostId) {
        if (libvirtDevice instanceof PciDevice) {
            PciDevice pci = (PciDevice) libvirtDevice;
            return new HostDeviceVO(pci, hostId);
        }
        throw new IllegalArgumentException("Unsupported device type: " + libvirtDevice.getDeviceType());
    }

    public long getId() {
        return id;
    }

    @Override
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

    public String getPciFunction() {
        return pciFunction;
    }

    public void setPciFunction(String pciFunction) {
        this.pciFunction = pciFunction;
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

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
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

    public void reserveToVM(VirtualMachineProfile vm) {
        Account owner = vm.getOwner();
        this.instanceId = vm.getId();
        this.accountId = owner.getAccountId();
        this.domainId = owner.getDomainId();
        this.state = State.Attached;
    }
}
