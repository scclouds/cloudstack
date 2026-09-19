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

package org.apache.cloudstack.api.command.user.hostdevices;

import com.cloud.exception.ConcurrentOperationException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DeviceOfferingResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.cloudstack.hostdevices.DeviceOfferingManager;

import javax.inject.Inject;

@APICommand(name = "assignVirtualMachineToDeviceOffering", description = "Assigns a device offering to a virtual machine.", responseObject = SuccessResponse.class)
public class AssignVirtualMachineToDeviceOfferingCmd extends BaseCmd {
    @Inject
    private DeviceOfferingManager deviceOfferingManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID,
            type = CommandType.UUID,
            required = true,
            description = "the ID of the virtual machine that the device offering should be assigned to",
            entityType = UserVmResponse.class)
    private Long virtualMachineId;
    @Parameter(name = ApiConstants.DEVICE_OFFERING_ID,
            type = CommandType.UUID,
            required = true,
            description = "the ID of the device offering that should be assigned",
            entityType = DeviceOfferingResponse.class)
    private Long deviceOfferingId;

    //////////////////////////////////////////////////
    //////////////// Accessors ///////////////////////
    //////////////////////////////////////////////////

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public Long getDeviceOfferingId() {
        return deviceOfferingId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        boolean success = deviceOfferingManager.assignVirtualMachineToDeviceOffering(getVirtualMachineId(), getDeviceOfferingId());
        if (success) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to assign device offering to virtual machine");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
