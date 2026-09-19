package com.cloud.hostdevices.dao;

import com.cloud.hostdevices.HostDeviceVO;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.hostdevices.HostDevice;

import java.util.List;

public interface HostDeviceDao extends GenericDao<HostDeviceVO, Long> {
    List<HostDeviceVO> listHostDevicesByHostId(Long hostId);

    List<HostDeviceVO> listHostDevicesAvailableForAllocation(Long hostId, List<String> deviceTags);

    List<HostDeviceVO> listHostDevices(Long hostDeviceId, Long accountId, List<Long> domainIds, Long hostId, Long virtualMachineId, String deviceTag, HostDevice.State state, HostDevice.Type type);

    List<HostDeviceVO> listHostDevicesByVmId(Long vmId);

    List<HostDeviceVO> listHostDevicesByHostIdAndState(Long hostId, HostDevice.State state);

    List<HostDeviceVO> listAndLockHostDevicesByHostIdAndState(Long hostId, HostDevice.State state);

    List<HostDeviceVO> listHostDevicesByAccountId(long accountId);

    List<HostDeviceVO> listHostDevicesForOfferingAndVmCheck(Long hostId, List<String> deviceOfferingsTags, Long virtualMachineId);

    List<HostDeviceVO> listAndLockHostDevicesByVmId(Long vmId);
}
