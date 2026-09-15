package org.apache.cloudstack.api.command.admin.hostdevices;

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
import org.apache.cloudstack.api.response.DeviceOfferingResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.hostdevices.DeviceOfferingManager;

import javax.inject.Inject;

@APICommand(name = "deleteDeviceOffering",
        description = "Deletes a device offering.",
        responseObject = SuccessResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin})
public class DeleteDeviceOfferingCmd extends BaseCmd {
    @Inject
    private DeviceOfferingManager deviceOfferingManager;

    @ACL
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, required = true, entityType = DeviceOfferingResponse.class, description = "the ID of the device offering to be removed")
    private Long id;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        boolean result = deviceOfferingManager.deleteOffering(id);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete device offering");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
