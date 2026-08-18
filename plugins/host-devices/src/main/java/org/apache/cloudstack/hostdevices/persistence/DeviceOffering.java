package org.apache.cloudstack.hostdevices.persistence;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

public interface DeviceOffering extends Identity, InternalIdentity {
    enum State {
        Disabled, Enabled
    }
}
