
package com.cloud.agent.api;

public class ScanDevicesCommand extends Command {
    @Override
    public boolean executeInSequence() {
        return true;
    }
}