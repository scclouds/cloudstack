package org.apache.cloudstack.api.command.user.hostdevices;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostDeviceResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.hostdevices.HostDevicesManager;

import javax.inject.Inject;

@APICommand(name = "deleteHostDevice",
        description = "Deletes a host device",
        responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "24.0.0",
        authorized = {RoleType.Admin})
public class DeleteHostDeviceCmd extends BaseCmd {
    @Inject
    private HostDevicesManager hostDevicesManager;

    @ACL
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = HostDeviceResponse.class, required = true, description = "The ID of the host device to delete")
    private Long id;

    public Long getId() {
        return id;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        boolean result = hostDevicesManager.deleteHostDevice(getId());

        if (!result) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete host device");
        }

        SuccessResponse response = new SuccessResponse(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
