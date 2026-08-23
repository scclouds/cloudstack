package org.apache.cloudstack.utils.libvirt.mappers;

import com.cloud.utils.xmlobject.XmlObject;
import org.apache.cloudstack.utils.libvirt.model.LibvirtDevice;

public interface DeviceMapper<T extends LibvirtDevice> {
    String DOMAIN_HEX_FORMAT = "0x%04x";
    String BUS_HEX_FORMAT = "0x%02x";
    String SLOT_HEX_FORMAT = "0x%02x";
    String FUNCTION_HEX_FORMAT = "0x%x";

    Class<T> getDeviceClass();

    boolean supportsDevice(String deviceType);

    T parse(XmlObject deviceDefinition);

    String mapToLibvirtHostDeviceXml(T device);
}
