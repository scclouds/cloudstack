package com.cloud.hostdevices.dao;


import com.cloud.hostdevices.VMInstanceDeviceOfferingsVO;
import com.cloud.utils.db.GenericDao;

import java.util.List;

public interface VMInstanceDeviceOfferingsDao extends GenericDao<VMInstanceDeviceOfferingsVO, Long> {
    List<VMInstanceDeviceOfferingsVO> findByVmId(Long virtualMachineId);
}
