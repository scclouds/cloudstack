package org.apache.cloudstack.hostdevices.service;

import org.apache.cloudstack.hostdevices.persistence.HostDevice;

import java.util.List;

public interface HostDevicesService {
    List<HostDevice> scanHostDevice(Long hostId, Long clusterId);
}
