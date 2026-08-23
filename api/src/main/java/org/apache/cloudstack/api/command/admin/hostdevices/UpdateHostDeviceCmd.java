package org.apache.cloudstack.api.command.admin.hostdevices;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostDeviceResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.hostdevices.HostDevice;
import org.apache.cloudstack.hostdevices.HostDevicesManager;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = "updateHostDevice",
        description = "Updates a host device",
        responseObject = HostDeviceResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin},
        since = "?")
public class UpdateHostDeviceCmd extends BaseCmd  {
    @Inject
    private HostDevicesManager hostDevicesManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = HostDeviceResponse.class,
            required = true, description = "The ID of the host device to be updated")
    private Long deviceId;

    @Parameter(name = ApiConstants.ENABLED, type = CommandType.BOOLEAN, description = "Whether the host device should be enabled or disabled")
    private Boolean isEnabled;

    @Parameter(name = ApiConstants.DISPLAY_NAME, type = CommandType.STRING, description = "The display name of the host device")
    private String displayName;

    @Parameter(name = ApiConstants.DEVICE_TAG, type = CommandType.STRING, description = "The tags for the host device")
    private String tag;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description = "The type of the host device")
    private String type;

    public Long getDeviceId() {
        return deviceId;
    }

    public Boolean getEnabled() {
        return isEnabled;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTag() {
        return tag;
    }

    public String getType() {
        return type;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        HostDevice hostDevice = hostDevicesManager.updateHostDevice(this);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }
}
