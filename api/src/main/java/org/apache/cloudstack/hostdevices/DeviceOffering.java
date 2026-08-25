package org.apache.cloudstack.hostdevices;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import java.util.Date;

public interface DeviceOffering extends Identity, InternalIdentity {
    enum State {
        Inactive, Active
    }

    String getName();

    String getDescription();

    State getState();

    Long getDomainId();

    Long getZoneId();

    Date getCreated();

    Date getRemoved();

    Boolean getIsPublic();
}
