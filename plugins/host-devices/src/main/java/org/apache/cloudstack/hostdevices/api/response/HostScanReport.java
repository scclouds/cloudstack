package org.apache.cloudstack.hostdevices.api.response;

import org.apache.cloudstack.hostdevices.persistence.HostDeviceVO;

import java.util.ArrayList;
import java.util.List;

public class HostScanReport {
    private List<HostDeviceVO> savedDevices;
    private List<HostDeviceVO> missingDevices;

    public HostScanReport() {
        this.savedDevices = new ArrayList<>();
    }

    public List<HostDeviceVO> getSavedDevices() {
        return this.savedDevices;
    }

    public void setSavedDevices(List<HostDeviceVO> savedDevices) {
        this.savedDevices = savedDevices;
    }


    public void setMissingDevices(List<HostDeviceVO> missingDevices) {
        this.missingDevices = missingDevices;
    }
}