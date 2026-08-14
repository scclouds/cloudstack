package org.apache.cloudstack.kvm.libvirt.mappers;

import com.cloud.utils.xmlobject.XmlObject;
import org.apache.cloudstack.kvm.libvirt.model.LibvirtDevice;

public interface DeviceMapper<T extends LibvirtDevice> {
    boolean supportsDevice(String deviceType);

    T parse(XmlObject deviceDefinition);
}
