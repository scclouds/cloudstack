package org.apache.cloudstack.hostdevices.service;

import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.hostdevices.api.command.ListHostDevicesCmd;
import org.apache.cloudstack.hostdevices.api.command.ScanHostDevicesCmd;
import org.apache.cloudstack.hostdevices.api.response.HostDeviceResponse;
import org.apache.cloudstack.hostdevices.persistence.HostDeviceVO;

import java.util.List;

public interface HostDevicesManager extends Configurable, Manager, PluggableService {
    void scanHostDevice(ScanHostDevicesCmd cmd);

    List<HostDeviceVO> listHostDevices(ListHostDevicesCmd cmd);

    HostDeviceResponse generateHostDeviceResponse(HostDeviceVO device);
}
