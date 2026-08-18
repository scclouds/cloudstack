package org.apache.cloudstack.hostdevices.persistence;

import com.cloud.utils.db.GenericDao;

import java.util.List;

public interface HostDeviceDao extends GenericDao<HostDeviceVO, Long> {
    List<HostDeviceVO> listHostDevicesByHostId(Long hostId);

    List<HostDeviceVO> listHostDevices(Long hostDeviceId, Long accountId, List<Long> domainIds, Long hostId, Long virtualMachineId, String deviceTag, HostDevice.State state, HostDevice.Type type);
}
