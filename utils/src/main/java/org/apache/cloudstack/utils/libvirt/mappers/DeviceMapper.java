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
