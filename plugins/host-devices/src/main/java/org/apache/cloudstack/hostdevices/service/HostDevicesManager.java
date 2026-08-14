package org.apache.cloudstack.hostdevices.service;

import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.hostdevices.api.command.ScanHostDevicesCmd;

public interface HostDevicesManager extends Configurable, Manager, PluggableService {
    void scanHostDevice(ScanHostDevicesCmd cmd);
}
