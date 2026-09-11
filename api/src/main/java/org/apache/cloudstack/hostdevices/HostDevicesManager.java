package org.apache.cloudstack.hostdevices;

import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.command.admin.hostdevices.ScanHostDevicesCmd;
import org.apache.cloudstack.api.command.admin.hostdevices.UpdateHostDeviceCmd;
import org.apache.cloudstack.api.command.user.hostdevices.ListHostDevicesCmd;
import org.apache.cloudstack.api.response.HostDeviceResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import java.util.List;

public interface HostDevicesManager extends Configurable, Manager, PluggableService {
    ConfigKey<Long> DefaultMaxAccountHostDevices = new ConfigKey<>("Account Defaults", Long.class, "max.account.host.devices", "20",
     "The default maximum host devices that can be used for an account", false);
    ConfigKey<Long> DefaultMaxDomainHostDevices = new ConfigKey<>("Domain Defaults", Long.class, "max.domain.host.devices", "20",
     "The default maximum host devices that can be used for a domain", false);
    ConfigKey<Long> DefaultMaxProjectHostDevices = new ConfigKey<>("Project Defaults", Long.class, "max.project.host.devices", "20",
     "The default maximum host devices that can be used for a project", false);

    void scanHostDevice(ScanHostDevicesCmd cmd);

    List<? extends HostDevice> listHostDevices(ListHostDevicesCmd cmd);

    HostDeviceResponse generateHostDeviceResponse(HostDevice device);

    HostDevice updateHostDevice(UpdateHostDeviceCmd updateHostDeviceCmd);

    void releaseHostDevicesForVm(Long vmId);

    void putHostDevicesInMaintenanceMode(Long hostId);

    void removeHostDevicesFromMaintenanceMode(long hostId);
}
