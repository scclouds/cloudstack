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
import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
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
        since = "4.23.0.0",
        authorized = {RoleType.Admin})
public class DeleteHostDeviceCmd extends BaseCmd {
    @Inject
    private HostDevicesManager hostDevicesManager;

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
