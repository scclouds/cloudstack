package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ScanDevicesAnswer;
import com.cloud.agent.api.storage.EraseHostDeviceCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;
import org.libvirt.Connect;
import org.libvirt.Device;

@ResourceWrapper(handles = EraseHostDeviceCommand.class)
public class LibvirtEraseHostDeviceCommandWrapper extends CommandWrapper<EraseHostDeviceCommand, Answer, LibvirtComputingResource> {
    @Override
    public Answer execute(EraseHostDeviceCommand command, LibvirtComputingResource libvirtComputingResource) {
        LibvirtUtilitiesHelper helper = libvirtComputingResource.getLibvirtUtilitiesHelper();
        String pciName = command.getDevicePciName();
        logger.debug("Received PCI device {} for erasure.", pciName);

        try {
            Connect conn = helper.getConnection();
            Device device = conn.deviceLookupByName(pciName);

            if (device == null) {
                throw new CloudRuntimeException("Device with PCI name " + pciName + " not found.");
            }

        } catch (Exception e) {
            String errorMessage = "Failed to erase host device due to " + e.getMessage();
            logger.error(errorMessage, e);
            return new ScanDevicesAnswer(command, false, errorMessage);
        }

        return new Answer(command, true, null);
    }
}
