package org.apache.cloudstack.kvm.libvirt.model;

public class PciDevice implements LibvirtDevice {
    private String name;
    private String classCode;
    private String domain;
    private String bus;
    private String slot;
    private String function;
    private String productId;
    private String productName;
    private String vendorId;
    private String vendorName;
    private final String deviceType;

    public PciDevice() {
        this.deviceType = "pci";
    }

    public PciDevice(String name, String classCode, String domain, String bus, String slot, String function, String productId, String vendorId, String productName, String vendorName) {
        this.name = name;
        this.classCode = classCode;
        this.domain = domain;
        this.bus = bus;
        this.slot = slot;
        this.function = function;
        this.productId = productId;
        this.vendorId = vendorId;
        this.productName = productName;
        this.vendorName = vendorName;
        this.deviceType = "pci";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassCode() {
        return classCode;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getBus() {
        return bus;
    }

    public void setBus(String bus) {
        this.bus = bus;
    }

    public String getSlot() {
        return slot;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getProductId() {
        return productId;
    }

    public String getVendorId() {
        return vendorId;
    }

    public String getProductName() {
        return productName;
    }

    public String getVendorName() {
        return vendorName;
    }

    @Override
    public String getDeviceType() {
        return this.deviceType;
    }
}
