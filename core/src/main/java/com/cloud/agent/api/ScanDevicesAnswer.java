package com.cloud.agent.api;

public class ScanDevicesAnswer extends Answer {

    public ScanDevicesAnswer() {
    }

    public ScanDevicesAnswer(Command command, boolean success, String details) {
        super(command, success, details);
    }
}
