// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

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

@APICommand(name = "updateHostDevice",
        description = "Updates a host device",
        responseObject = HostDeviceResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin},
        since = "4.24.0")
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

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description = "The type of the host device. Supported values are: Display, Network, Storage, USB and Generic. Case insensitive.")
    private String type;

    @Parameter(name = ApiConstants.ONE_TIME_USE, type = CommandType.BOOLEAN, description = "Whether the host device is for marked as one-time-use. Devices in this state will become unavailable after release, and operators must manually enable them again.")
    private Boolean oneTimeUse;

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

    public Boolean getOneTimeUse() {
        return oneTimeUse;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        HostDevice hostDevice = hostDevicesManager.updateHostDevice(this);
        HostDeviceResponse response = hostDevicesManager.generateHostDeviceResponse(hostDevice);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }
}
