package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ScanDevicesAnswer;
import com.cloud.agent.api.storage.EraseHostDeviceCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.utils.xmlobject.XmlObject;
import com.cloud.utils.xmlobject.XmlObjectParser;
import org.apache.commons.lang3.ObjectUtils;
import org.libvirt.Connect;
import org.libvirt.Device;
import org.libvirt.LibvirtException;

import java.util.Arrays;

@ResourceWrapper(handles = EraseHostDeviceCommand.class)
public class LibvirtEraseHostDeviceCommandWrapper extends CommandWrapper<EraseHostDeviceCommand, Answer, LibvirtComputingResource> {
    /*
     * Used parameters:
     *  - e 7: defines that only disk devices should be listed;
     *  - n: disables the printing of the header;
     *  - d: displays only devices (excludes partitions);
     *  - o NAME: returns only the device name.
     */
    // TODO ERIK: nome temporario por causa do checkstyle do projeto
    private final String lsblkCommand = "/usr/bin/lsblk -e 7 -n -d -o NAME";
    private final String udevCommand = "/usr/bin/udevadm info --query=property --path=";
    private final String devicePrefix = "/dev/";

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

            String deviceSystemPath = getDeviceSystemPath(device);
            String[] hostDiskDevices = getHostDiskDevices();

            for (String deviceName : hostDiskDevices) {
                String result = Script.runSimpleBashScriptWithFullResult(udevCommand + deviceName, 60000);

                if (result == null) {
                    logger.warn("Could not retrieve udev information for device {}. Skipping it.", deviceName);
                    continue;
                }

                String udevHostDevicePath = getUdevHostDevicePath(result);

                if (udevHostDevicePath == null) {
                    logger.warn("Could not determine udev host device path for device {}. Skipping it.", deviceName);
                    continue;
                }

                logger.info("Udev host device path for {}: {}", deviceName, udevHostDevicePath);
                String devicePciIdentifier = extractPciIdentifierFromPath(deviceSystemPath);
                String udevPciIdentifier = extractPciIdentifierFromPath(udevHostDevicePath);

                if (ObjectUtils.anyNull(devicePciIdentifier, udevPciIdentifier)) {
                    logger.warn("Could not extract PCI identifiers for device {}. Skipping it.", deviceName);
                    continue;
                }

                if (!devicePciIdentifier.equals(udevPciIdentifier)) {
                    logger.warn("Device {} does not match the PCI identifier {}. Skipping it.", deviceName, devicePciIdentifier);
                    continue;
                }

                logger.info("Device {} matches the PCI identifier {}. Proceeding with erasure.", deviceName, devicePciIdentifier);
//                Script.runSimpleBashScript("wipefs -a " + deviceName);
//                logger.info("Successfully erased device {}.", deviceName);
                return new ScanDevicesAnswer(command, true, null);
            }
        } catch (Exception e) {
            String errorMessage = "Failed to erase host device due to " + e.getMessage();
            logger.error(errorMessage, e);
            return new ScanDevicesAnswer(command, false, errorMessage);
        }

        return new Answer(command, true, null);
    }

    private String getDeviceSystemPath(Device libvirtDevice) throws LibvirtException {
        XmlObject deviceXml = XmlObjectParser.parseFromString(libvirtDevice.getXMLDescription());
        String fullDeviceSystemPath = deviceXml.getTextFromInnerTag("path");

        if (fullDeviceSystemPath == null) {
            throw new CloudRuntimeException("Failed to get device system path for device");
        }

        // Remove the "/sys" prefix
        return fullDeviceSystemPath.substring(4);
    }

    private String[] getHostDiskDevices() {
        // TODO ERIK: isso aqui pode travar se o storage tiver em hang, conversar sobre
        String result = Script.runSimpleBashScriptWithFullResult(lsblkCommand, 60000);

        if (result == null) {
            throw new CloudRuntimeException("Failed to list host's disk devices with command: " + lsblkCommand);
        }

        String[] diskDevices = result.split("\n");

        if (diskDevices.length == 0) {
            throw new CloudRuntimeException("No disk devices found on the host.");
        }

        for (int i = 0; i < diskDevices.length; i++) {
            diskDevices[i] = devicePrefix + diskDevices[i];
        }

        logger.info("List of disk devices: {}", Arrays.toString(diskDevices));
        return diskDevices;
    }

    private String getUdevHostDevicePath(String commandResult) {
        String[] lines = commandResult.split("\n");
        logger.info("Udev command result: {}", Arrays.toString(lines));

        for (String line : lines) {
            if (line.startsWith("DEVPATH=")) {
                // Remove "DEVPATH=" prefix
                return line.substring(8);
            }
        }

        return null;
    }

    private String extractPciIdentifierFromPath(String fullPath) {
        logger.info("Trying to extract PCI identifiers from path: {}", fullPath);
        String[] parts = fullPath.split("/");

        // TODO ERIK: perguntar se essa validação tá certa
        if (parts.length < 5) {
            logger.warn("Path {} does not contain enough parts to extract PCI identifiers. Returning null", fullPath);
            return null;
        }

        String[] pciParts = Arrays.copyOfRange(parts, 2, 5);
        String formattedPciIdentifier = String.join("/", pciParts);
        logger.info("Extracted PCI identifiers from path: {}", formattedPciIdentifier);

        return formattedPciIdentifier;
    }
}
