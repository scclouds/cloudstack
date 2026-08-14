package org.apache.cloudstack.kvm.libvirt.mappers;

import com.cloud.utils.xmlobject.XmlObject;
import org.apache.cloudstack.kvm.libvirt.model.PciDevice;

public class PciDeviceMapper implements DeviceMapper<PciDevice> {
    @Override
    public boolean supportsDevice(String deviceType) {
        return deviceType.equals("pci");
    }

    @Override
    public PciDevice parse(XmlObject deviceDefinition) {
        String name = getTextFromTag(deviceDefinition, "name");
        XmlObject capabilityTag = deviceDefinition.get("capability");
        String domain = getTextFromTag(capabilityTag, "domain");
        String bus = getTextFromTag(capabilityTag, "bus");
        String slot = getTextFromTag(capabilityTag, "slot");
        String function = getTextFromTag(capabilityTag, "function");
        String classCode = getTextFromTag(capabilityTag, "class");

        XmlObject productTag = capabilityTag.get("product");
        String productId = (String) productTag.getElement("id");
        String productName = productTag.getText();
        XmlObject vendorTag = capabilityTag.get("vendor");
        String vendorId = (String) vendorTag.getElement("id");
        String vendorName = vendorTag.getText();

        return new PciDevice(name, classCode, domain, bus, slot, function, productId, vendorId, productName, vendorName);
    }

    private String getTextFromTag(XmlObject xmlObject, String tagName) {
        XmlObject tag = xmlObject.get(tagName);

        return tag != null ? tag.getText() : null;
    }
}

