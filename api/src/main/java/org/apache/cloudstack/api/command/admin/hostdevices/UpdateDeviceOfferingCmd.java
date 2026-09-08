package org.apache.cloudstack.api.command.admin.hostdevices;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DeviceOfferingResponse;
import org.apache.cloudstack.hostdevices.DeviceOffering;
import org.apache.cloudstack.hostdevices.DeviceOfferingManager;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = "updateDeviceOffering",
        description = "Updates a device offering",
        responseObject = DeviceOfferingResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin},
        since = "?")
public class UpdateDeviceOfferingCmd extends BaseCmd {
    @Inject
    private DeviceOfferingManager deviceOfferingManager;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, required = true, entityType = DeviceOfferingResponse.class, description = "the ID of the device offering")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name for the device offering")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "the description for the device offering")
    private String description;

    @Parameter(name = ApiConstants.DEVICE_TAGS, type = CommandType.LIST, collectionType = CommandType.STRING, description = "the list of device tags for the device offering")
    private List<String> tags;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "the state of the device offering. Can be either Active or Inactive")
    private String state;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getState() {
        return state;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        DeviceOffering updatedOffering = deviceOfferingManager.updateDeviceOffering(this);
        DeviceOfferingResponse response = deviceOfferingManager.createDeviceOfferingResponse(updatedOffering);
        response.setObjectName("deviceoffering");
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}
