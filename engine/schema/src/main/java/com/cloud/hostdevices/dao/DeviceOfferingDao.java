package com.cloud.hostdevices.dao;

import com.cloud.hostdevices.DeviceOfferingVO;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.hostdevices.DeviceOffering;

import java.util.List;

public interface DeviceOfferingDao extends GenericDao<DeviceOfferingVO, Long> {
    List<DeviceOfferingVO> listVirtualMachineDeviceOfferings(Long virtualMachineId);

    List<DeviceOfferingVO> listDeviceOfferings(String name, List<Long> domainIds, Long zoneId, List<String> deviceTags, DeviceOffering.State state, boolean showOnlyPublic);
}
