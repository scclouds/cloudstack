package org.apache.cloudstack.api.command.user.hostdevices;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DeviceOfferingResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.hostdevices.DeviceOfferingManager;

import javax.inject.Inject;

@APICommand(name = "removeVirtualMachineFromDeviceOffering", description = "Removes a device offering from a virtual machine.", responseObject = SuccessResponse.class)
public class RemoveVirtualMachineFromDeviceOfferingCmd extends BaseCmd {
    @Inject
    private DeviceOfferingManager deviceOfferingManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
            type = CommandType.UUID,
            required = true,
            description = "the ID of the virtual machine that the device offering will be removed from",
            entityType = UserVmResponse.class)
    private Long virtualMachineId;
    @Parameter(name = ApiConstants.DEVICE_OFFERING_ID,
            type = CommandType.UUID,
            required = true,
            description = "the ID of the device offering that will be removed from the virtual machine",
            entityType = DeviceOfferingResponse.class)
    private Long deviceOfferingId;

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(Long virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public Long getDeviceOfferingId() {
        return deviceOfferingId;
    }

    public void setDeviceOfferingId(Long deviceOfferingId) {
        this.deviceOfferingId = deviceOfferingId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        boolean success = deviceOfferingManager.removeVirtualMachineFromDeviceOffering(getVirtualMachineId(), getDeviceOfferingId());
        if (success) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to remove device offering from virtual machine");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}
