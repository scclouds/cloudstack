package com.cloud.hypervisor.kvm.resource;

import com.cloud.agent.api.to.HostDeviceTO;
import org.apache.cloudstack.utils.libvirt.LibvirtDeviceMapper;

public class LibvirtHostDeviceDef {
    private HostDeviceTO device;

    public void setDevice(HostDeviceTO device) {
        this.device = device;
    }

    @Override
    public String toString() {
        return new LibvirtDeviceMapper().mapDeviceToLibvirtXml(device.toLibvirtDevice());
    }
}
