package org.apache.cloudstack.utils.libvirt;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.xmlobject.XmlObject;
import org.apache.cloudstack.utils.libvirt.mappers.DeviceMapper;
import org.apache.cloudstack.utils.libvirt.mappers.PciDeviceMapper;
import org.apache.cloudstack.utils.libvirt.model.LibvirtDevice;

import java.util.List;

public class LibvirtDeviceMapper {
    private final List<DeviceMapper<? extends LibvirtDevice>> mappers;

    public LibvirtDeviceMapper() {
        this.mappers = List.of(new PciDeviceMapper());
    }

    public LibvirtDevice mapDevice(XmlObject rawDeviceDefinition) {
        String deviceType = getDeviceType(rawDeviceDefinition);

        if (deviceType == null) {
            throw new CloudRuntimeException("Failed to parse device definition. Missing capability type tag in " + rawDeviceDefinition);
        }

        for (DeviceMapper<? extends LibvirtDevice> mapper : mappers) {
            if (mapper.supportsDevice(deviceType)) {
                return mapper.parse(rawDeviceDefinition);
            }
        }

        throw new CloudRuntimeException("Failed to parse device definition. Device type not supported: " + deviceType);
    }

    private String getDeviceType(XmlObject rawDeviceDefinition) {
        XmlObject capabilityTag = rawDeviceDefinition.get("capability");

        if (capabilityTag == null) {
            return null;
        }

        Object typeTag = capabilityTag.getElement("type");

        if (typeTag == null) {
            return null;
        }

        return typeTag.toString();
    }

    public String mapDeviceToLibvirtXml(LibvirtDevice device) {
        for (DeviceMapper<? extends LibvirtDevice> mapper : mappers) {
            if (mapper.getDeviceClass().isInstance(device)) {
                return mapDeviceInternal(mapper, device);
            }
        }
        throw new CloudRuntimeException("Failed to map device to Libvirt XML. Device type not supported: " + device.getClass().getName());
    }

    private <T extends LibvirtDevice> String mapDeviceInternal(DeviceMapper<T> mapper, LibvirtDevice device) {
        T typedDevice = mapper.getDeviceClass().cast(device);
        return mapper.mapToLibvirtHostDeviceXml(typedDevice);
    }
}
