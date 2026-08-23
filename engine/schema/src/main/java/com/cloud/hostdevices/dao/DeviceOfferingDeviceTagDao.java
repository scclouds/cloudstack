package com.cloud.hostdevices.dao;

import com.cloud.hostdevices.DeviceOfferingDeviceTagVO;
import com.cloud.hostdevices.DeviceOfferingVO;
import com.cloud.utils.db.GenericDao;

import java.util.List;

public interface DeviceOfferingDeviceTagDao extends GenericDao<DeviceOfferingDeviceTagVO, Long> {
    List<String> getDeviceOfferingsTags(List<DeviceOfferingVO> deviceOfferingIds);
}
