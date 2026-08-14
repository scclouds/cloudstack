package org.apache.cloudstack.hostdevices.persistence;

import com.cloud.utils.db.GenericDao;

import java.util.List;

public interface HostDeviceDao extends GenericDao<HostDeviceVO, Long> {
    List<HostDeviceVO> listHostDevicesByHostId(Long hostId);
}
