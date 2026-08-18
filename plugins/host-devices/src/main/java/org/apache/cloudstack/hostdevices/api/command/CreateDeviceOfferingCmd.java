package org.apache.cloudstack.hostdevices.api.command;

import com.cloud.exception.ConcurrentOperationException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.hostdevices.api.response.DeviceOfferingResponse;
import org.apache.cloudstack.hostdevices.persistence.DeviceOfferingVO;
import org.apache.cloudstack.hostdevices.service.DeviceOfferingManager;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = "createDeviceOffering",
        description = "Creates a device offering.",
        responseObject = DeviceOfferingResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin})
public class CreateDeviceOfferingCmd extends BaseCmd {
    @Inject
    private DeviceOfferingManager deviceOfferingManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = "name", type = CommandType.STRING, required = true, description = "the name for the device offering")
    private String name;

    @Parameter(name = "description", type = CommandType.STRING, required = true, description = "the description for the device offering")
    private String description;

    @ACL
    @Parameter(name = "domainId", type = CommandType.LONG, required = false, description = "the domain for the device offering to be dedicated to. Mutually exclusive with the zoneId parameter.")
    private Long domainId;

    @ACL
    @Parameter(name = "zoneId", type = CommandType.LONG, required = false, description = "the zone for device offering to be dedicated to. Mutually exclusive with the domainId parameter.")
    private Long zoneId;

    @Parameter(name = "devicetags", type = CommandType.LIST, collectionType = CommandType.STRING, required = true, description = "the list of device tags for the device offering")
    private List<String> tags;

    //////////////////////////////////////////////////
    //////////////// Accessors ///////////////////////
    //////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public List<String> getTags() {
        return tags;
    }

    //////////////////////////////////////////////////
    //////////// API Implementation///////////////////
    //////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        DeviceOfferingVO deviceOffering = deviceOfferingManager.createDeviceOffering(this);
        DeviceOfferingResponse response = deviceOfferingManager.createDeviceOfferingResponse(deviceOffering);
        response.setObjectName("deviceoffering");
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }
}
