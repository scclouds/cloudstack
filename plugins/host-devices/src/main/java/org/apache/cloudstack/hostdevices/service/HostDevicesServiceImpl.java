package org.apache.cloudstack.hostdevices.service;

import org.apache.cloudstack.hostdevices.persistence.HostDevice;

import java.util.List;

public class HostDevicesServiceImpl implements HostDevicesService {
    @Override
    public List<HostDevice> scanHostDevice(Long hostId, Long clusterId) {
        return List.of();
    }
}
