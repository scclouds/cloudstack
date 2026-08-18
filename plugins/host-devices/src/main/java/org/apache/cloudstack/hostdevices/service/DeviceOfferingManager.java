package org.apache.cloudstack.hostdevices.service;

import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.hostdevices.api.command.CreateDeviceOfferingCmd;
import org.apache.cloudstack.hostdevices.api.response.DeviceOfferingResponse;
import org.apache.cloudstack.hostdevices.persistence.DeviceOfferingVO;

public interface DeviceOfferingManager extends Configurable, Manager, PluggableService {
    DeviceOfferingVO createDeviceOffering(CreateDeviceOfferingCmd cmd);

    DeviceOfferingResponse createDeviceOfferingResponse(DeviceOfferingVO deviceOffering);

    boolean assignVirtualMachineToDeviceOffering(Long virtualMachineId, Long deviceOfferingId);
}
