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

package org.apache.cloudstack.hostdevices;

import com.cloud.host.Host;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.command.admin.hostdevices.ScanHostDevicesCmd;
import org.apache.cloudstack.api.command.admin.hostdevices.UpdateHostDeviceCmd;
import org.apache.cloudstack.api.command.user.hostdevices.ListHostDevicesCmd;
import org.apache.cloudstack.api.response.HostDeviceResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import java.util.List;
import java.util.Map;

public interface HostDevicesManager extends Configurable, Manager, PluggableService {
    ConfigKey<Long> DefaultMaxAccountHostDevices = new ConfigKey<>("Account Defaults", Long.class, "max.account.host.devices", "20",
     "The default maximum host devices that can be used for an account", false);
    ConfigKey<Long> DefaultMaxDomainHostDevices = new ConfigKey<>("Domain Defaults", Long.class, "max.domain.host.devices", "20",
     "The default maximum host devices that can be used for a domain", false);
    ConfigKey<Long> DefaultMaxProjectHostDevices = new ConfigKey<>("Project Defaults", Long.class, "max.project.host.devices", "20",
     "The default maximum host devices that can be used for a project", false);
    ConfigKey<Boolean> HostDeviceAutomaticScanEnabled = new ConfigKey<>("Advanced", Boolean.class, "host.device.automatic.scan.enabled", "false","Enable automatic scanning for host devices. When enabled, the scanning executes after the interval defined by host.device.automatic.scan.interval configuration.", true, ConfigKey.Scope.Cluster);
    ConfigKey<Integer> HostDeviceAutomaticScanInterval = new ConfigKey<>("Advanced", Integer.class, "host.device.automatic.scan.interval", "21600", "The interval in seconds to scan for host devices", true, ConfigKey.Scope.Cluster);

    String LAST_HOST_DEVICE_SCAN_EXECUTION_TIMESTAMP = "lastHostDeviceScanExecutionTimestamp";

    void scanHostDevice(ScanHostDevicesCmd cmd);

    Pair<List<? extends HostDevice>, Integer> listHostDevices(ListHostDevicesCmd cmd);

    HostDeviceResponse generateHostDeviceResponse(HostDevice device);

    HostDevice updateHostDevice(UpdateHostDeviceCmd updateHostDeviceCmd);

    void releaseHostDevicesForVm(Long vmId);

    void releaseHostDevicesForVm(Long vmId, Map<String, Integer> deviceTags);

    void putHostDevicesInMaintenanceMode(Long hostId);

    void removeHostDevicesFromMaintenanceMode(long hostId);

    void updateVMHostDevicesOwnership(Long vmId, Account oldAccount, Account newAccount);

    boolean reserveDevicesForVm(Long vmId, Long selectedHostId);

    boolean hasHostDevicesReservedForVm(Long vmId);

    boolean doesHostMatchDeviceOfferingTags(Host host, List<? extends DeviceOffering> deviceOfferings, Long virtualMachineId);

    boolean doesHostMatchVmDeviceOfferings(Host host, Long virtualMachineId);
}
