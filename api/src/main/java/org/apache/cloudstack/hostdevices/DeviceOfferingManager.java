package org.apache.cloudstack.hostdevices;

import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.command.admin.hostdevices.CreateDeviceOfferingCmd;
import org.apache.cloudstack.api.response.DeviceOfferingResponse;
import org.apache.cloudstack.framework.config.Configurable;

public interface DeviceOfferingManager extends Configurable, Manager, PluggableService {
    DeviceOffering createDeviceOffering(CreateDeviceOfferingCmd cmd);

    DeviceOfferingResponse createDeviceOfferingResponse(DeviceOffering deviceOffering);

    boolean assignVirtualMachineToDeviceOffering(Long virtualMachineId, Long deviceOfferingId);

    boolean removeVirtualMachineFromDeviceOffering(Long virtualMachineId, Long deviceOfferingId);
}
