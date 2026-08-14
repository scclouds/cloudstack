package org.apache.cloudstack.hostdevices.persistence;

import org.apache.cloudstack.kvm.libvirt.model.LibvirtDevice;
import org.apache.cloudstack.kvm.libvirt.model.PciDevice;

import javax.persistence.Column;
import javax.persistence.Entity;
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
    private String deviceTags;

    @Column(name = "created")
    @Temporal(value = TemporalType.DATE)
    private Date created;

    @Column(name = "removed")
    @Temporal(value = TemporalType.DATE)
    private Date removed;

    @Column(name = "state")
    private HostDevice.State state;

    @Column(name = "type")
    private HostDevice.Type type;

    @Column(name = "instance_id")
    private Long instanceId;

    @Column(name = "account_id")
    private Long accountId;

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
        this.deviceTags = this.type.toString();
        this.hostId = hostId;
    }

    public static HostDeviceVO mapLibvirtDevice(LibvirtDevice libvirtDevice, Long hostId) {
        if (libvirtDevice instanceof PciDevice) {
            PciDevice pci = (PciDevice) libvirtDevice;
            return new HostDeviceVO(pci, hostId);
        }
        throw new IllegalArgumentException("Unsupported device type: " + libvirtDevice.getDeviceType());
    }

    public Long getHostId() {
        return hostId;
    }

    public String getPciName() {
        return pciName;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public void setState(State state) {
        this.state = state;
    }
}
