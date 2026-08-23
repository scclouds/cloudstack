package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.agent.api.ScanDevicesAnswer;
import com.cloud.agent.api.ScanDevicesCommand;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.xmlobject.XmlObject;import com.cloud.utils.xmlobject.XmlObjectParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.cloudstack.utils.libvirt.LibvirtDeviceMapper;
import org.apache.cloudstack.utils.libvirt.model.LibvirtDevice;
import org.libvirt.Connect;
import org.libvirt.Device;import org.libvirt.LibvirtException;

import java.util.ArrayList;
import java.util.List;

@ResourceWrapper(handles = ScanDevicesCommand.class)
public class LibvirtScanDevicesCommandWrapper extends CommandWrapper<ScanDevicesCommand, ScanDevicesAnswer, LibvirtComputingResource> {
    List<String> supportedDevices = List.of("pci");

    @Override
    public ScanDevicesAnswer execute(ScanDevicesCommand command, LibvirtComputingResource serverResource) {
        Connect conn = null;
        LibvirtUtilitiesHelper libvirtUtilitiesHelper = serverResource.getLibvirtUtilitiesHelper();

        try {
            conn = libvirtUtilitiesHelper.getConnection();
            logger.debug("Listing all host devices. Supported devices: {}", supportedDevices);
            String[] hostDevices = conn.listDevices(String.join(",", supportedDevices));

            List<LibvirtDevice> mappedDevices = new ArrayList<>();
            final LibvirtDeviceMapper libvirtDeviceMapper = new LibvirtDeviceMapper();

            for (String deviceName : hostDevices) {
                Device device = conn.deviceLookupByName(deviceName);
                XmlObject deviceDefinition = XmlObjectParser.parseFromString(device.getXMLDescription());
                LibvirtDevice mappedDevice =  libvirtDeviceMapper.mapDevice(deviceDefinition);
                mappedDevices.add(mappedDevice);
            }

            return new ScanDevicesAnswer(command, true, new ObjectMapper().writeValueAsString(mappedDevices));
        } catch (LibvirtException e) {
            logger.debug("Failed to scan host devices due to {}", e.getMessage());
            return new ScanDevicesAnswer(command, false, "Failed to scan host devices due to " + e.getMessage());
        } catch (JsonProcessingException e) {
            String errorMessage = "Failed to serialize mapped devices due to " + e.getMessage();
            logger.debug(errorMessage);
            return new ScanDevicesAnswer(command, false, errorMessage);
        }
    }
}
