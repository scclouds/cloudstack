package org.apache.cloudstack.hostdevices.api.command;

import com.cloud.exception.ConcurrentOperationException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.hostdevices.api.response.HostDeviceResponse;
import org.apache.cloudstack.hostdevices.persistence.HostDeviceVO;
import org.apache.cloudstack.hostdevices.service.HostDevicesManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = "listHostDevices",
        description = "Lists registered host devices",
        responseObject = HostDeviceResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        since = "?")
public class ListHostDevicesCmd extends BaseListCmd {
    @Inject
    private HostDevicesManager hostDevicesManager;

    @ACL
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = HostDeviceResponse.class, description = "The ID of the host device")
    private Long id;

    @ACL
    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, description = "The device's allocator account ID.", authorized = {RoleType.Admin, RoleType.DomainAdmin})
    private Long accountId;

    @ACL
    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class, description = "The device's host ID.", authorized = {RoleType.Admin, RoleType.DomainAdmin})
    private Long hostId;

    @ACL
    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID, entityType = UserVmResponse.class, description = "The ID of the virtual machine the device is attached to.")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.DEVICE_TAG, type = CommandType.STRING, entityType = String.class, description = "The device tag to be searched for in host devices.")
    private String deviceTag;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "The state of the host device. Possible states are: Attached, Disabled, Failure, Free, HostInMaintenance and Missing . By default, devices in the Disabled state will be hidden.")
    private String state;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description = "The type of the host device. Possible types are: Display, Network, Storage, USB and Generic.")
    private String type;

    @Parameter(name = ApiConstants.LIST_ALL, type = CommandType.BOOLEAN, description = "False (default) lists the host devices allocated for calling Account. True lists all host devices accessible to caller")
    private Boolean listAll;

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getHostId() {
        return hostId;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public String getDeviceTag() {
        return deviceTag;
    }

    public String getState() {
        return state;
    }

    public String getType() {
        return type;
    }

    public boolean listAll() {
        return listAll != null && listAll;
    }

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        List<HostDeviceVO> hostDevices = hostDevicesManager.listHostDevices(this);
        List<HostDeviceResponse> responseList = new ArrayList<HostDeviceResponse>();
        for (HostDeviceVO device : hostDevices) {
            responseList.add(hostDevicesManager.generateHostDeviceResponse(device));
        }

        ListResponse<HostDeviceResponse> response = new ListResponse<HostDeviceResponse>();
        response.setResponses(responseList);
        response.setObjectName("hostdevices");
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
