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

package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.ScanDevicesAnswer;
import com.cloud.agent.api.ScanDevicesCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.xmlobject.XmlObject;
import com.cloud.utils.xmlobject.XmlObjectParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.cloudstack.utils.libvirt.LibvirtDeviceMapper;
import org.apache.cloudstack.utils.libvirt.model.LibvirtDevice;
import org.libvirt.Connect;
import org.libvirt.Device;

import java.util.ArrayList;
import java.util.List;

@ResourceWrapper(handles = ScanDevicesCommand.class)
public class LibvirtScanDevicesCommandWrapper extends CommandWrapper<ScanDevicesCommand, ScanDevicesAnswer, LibvirtComputingResource> {
    private static final List<String> SUPPORTED_DEVICE_CAPABILITIES = List.of("pci");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public ScanDevicesAnswer execute(ScanDevicesCommand command, LibvirtComputingResource serverResource) {
        LibvirtUtilitiesHelper libvirtUtilitiesHelper = serverResource.getLibvirtUtilitiesHelper();

        try {
            Connect conn = libvirtUtilitiesHelper.getConnection();
            logger.debug("Listing all host devices. Supported device capabilities: {}", SUPPORTED_DEVICE_CAPABILITIES);

            List<LibvirtDevice> mappedDevices = new ArrayList<>();
            final LibvirtDeviceMapper libvirtDeviceMapper = new LibvirtDeviceMapper();

            for (String capability : SUPPORTED_DEVICE_CAPABILITIES) {
                for (String deviceName : conn.listDevices(capability)) {
                    Device device = conn.deviceLookupByName(deviceName);

                    try {
                        XmlObject deviceDefinition = XmlObjectParser.parseFromString(device.getXMLDescription());
                        mappedDevices.add(libvirtDeviceMapper.mapDevice(deviceDefinition));
                    } finally {
                        device.free();
                    }
                }
            }

            return new ScanDevicesAnswer(command, true, MAPPER.writeValueAsString(mappedDevices));
        } catch (Exception e) {
            String errorMessage = "Failed to scan host devices due to " + e.getMessage();
            logger.error(errorMessage, e);
            return new ScanDevicesAnswer(command, false, errorMessage);
        }
    }
}
