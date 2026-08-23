package com.cloud.hostdevices.dao;

import com.cloud.hostdevices.DeviceOfferingVO;
import com.cloud.utils.db.GenericDao;

import java.util.List;

public interface DeviceOfferingDao extends GenericDao<DeviceOfferingVO, Long> {
    List<DeviceOfferingVO> listVirtualMachineDeviceOfferings(Long virtualMachineId);
}
