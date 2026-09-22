package com.cloud.agent.api.storage;

import com.cloud.agent.api.Command;

public class EraseHostDeviceCommand extends Command {
    private final String devicePciName;

    public EraseHostDeviceCommand(String devicePciName) {
        this.devicePciName = devicePciName;
    }

    public String getDevicePciName() {
        return devicePciName;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
