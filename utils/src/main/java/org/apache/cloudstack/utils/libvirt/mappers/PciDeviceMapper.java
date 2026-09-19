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

package org.apache.cloudstack.utils.libvirt.mappers;

import com.cloud.utils.exception.CloudRuntimeException;
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

        if (capabilityTag == null) {
            throw new CloudRuntimeException("Failed to parse PCI device definition. Missing capability tag for device " + name);
        }

        String domain = getTextFromTag(capabilityTag, "domain");
        String bus = getTextFromTag(capabilityTag, "bus");
        String slot = getTextFromTag(capabilityTag, "slot");
        String function = getTextFromTag(capabilityTag, "function");
        String classCode = getTextFromTag(capabilityTag, "class");

        XmlObject productTag = capabilityTag.get("product");
        String productId = getElementAsString(productTag, "id");
        String productName = productTag == null ? null : productTag.getText();

        XmlObject vendorTag = capabilityTag.get("vendor");
        String vendorId = getElementAsString(vendorTag, "id");
        String vendorName = vendorTag == null ? null : vendorTag.getText();

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

    private String getElementAsString(XmlObject xmlObject, String elementName) {
        if (xmlObject == null) {
            return null;
        }

        Object element = xmlObject.getElement(elementName);

        return element == null ? null : element.toString();
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

