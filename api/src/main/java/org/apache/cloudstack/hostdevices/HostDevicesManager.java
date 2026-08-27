package org.apache.cloudstack.hostdevices;

import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.command.admin.hostdevices.ScanHostDevicesCmd;
import org.apache.cloudstack.api.command.admin.hostdevices.UpdateHostDeviceCmd;
import org.apache.cloudstack.api.command.user.hostdevices.ListHostDevicesCmd;
import org.apache.cloudstack.api.response.HostDeviceResponse;
import org.apache.cloudstack.framework.config.Configurable;
import java.util.List;

public interface HostDevicesManager extends Configurable, Manager, PluggableService {
    void scanHostDevice(ScanHostDevicesCmd cmd);

    List<? extends HostDevice> listHostDevices(ListHostDevicesCmd cmd);

    HostDeviceResponse generateHostDeviceResponse(HostDevice device);

    HostDevice updateHostDevice(UpdateHostDeviceCmd updateHostDeviceCmd);

    void releaseHostDevicesForVm(Long vmId);

    void putHostDevicesInMaintenanceMode(Long hostId);

    void removeHostDevicesFromMaintenanceMode(long hostId);
}
