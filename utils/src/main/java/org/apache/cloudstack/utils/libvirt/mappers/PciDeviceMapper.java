package org.apache.cloudstack.utils.libvirt.mappers;

import com.cloud.utils.xmlobject.XmlObject;
import org.apache.cloudstack.utils.libvirt.model.PciDevice;

public class PciDeviceMapper implements DeviceMapper<PciDevice> {
    @Override
    public boolean supportsDevice(String deviceType) {
        return "pci".equals(deviceType);
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

    @Override
    public String mapToLibvirtHostDeviceXml(PciDevice device) {
        String domainHex = String.format(DOMAIN_HEX_FORMAT, Integer.parseInt(device.getDomain()));
        String busHex = String.format(BUS_HEX_FORMAT, Integer.parseInt(device.getBus()));
        String slotHex = String.format(SLOT_HEX_FORMAT, Integer.parseInt(device.getSlot()));
        String functionHex = String.format(FUNCTION_HEX_FORMAT, Integer.parseInt(device.getFunction()));

        StringBuilder xml = new StringBuilder();
        xml.append("<hostdev mode='subsystem' type='pci' managed='yes'>\n");
        xml.append("  <driver name='vfio'/>\n");
        xml.append("  <source>\n");
        xml.append("    <address domain='").append(domainHex)
                .append("' bus='").append(busHex)
                .append("' slot='").append(slotHex)
                .append("' function='").append(functionHex)
                .append("'/>\n");
        xml.append("  </source>\n");
        xml.append("</hostdev>\n");
        return xml.toString();
    }

    private String getTextFromTag(XmlObject xmlObject, String tagName) {
        XmlObject tag = xmlObject.get(tagName);

        return tag != null ? tag.getText() : null;
    }

    @Override
    public Class<PciDevice> getDeviceClass() {
        return PciDevice.class;
    }
}

