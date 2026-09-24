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
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DeviceOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.hostdevices.DeviceOffering;
import org.apache.cloudstack.hostdevices.DeviceOfferingManager;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = "createDeviceOffering",
        description = "Creates a device offering.",
        responseObject = DeviceOfferingResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class CreateDeviceOfferingCmd extends BaseCmd {
    @Inject
    private DeviceOfferingManager deviceOfferingManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name for the device offering")
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, required = true, description = "the description for the device offering")
    private String description;

    @ACL
    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, required = false, description = "the domain for the device offering to be dedicated to. Mutually exclusive with the zoneId parameter.")
    private Long domainId;

    @ACL
    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = false, description = "the zone for device offering to be dedicated to. Mutually exclusive with the domainId parameter.")
    private Long zoneId;

    @Parameter(name = ApiConstants.DEVICE_TAGS, type = CommandType.LIST, collectionType = CommandType.STRING, required = true, description = "a comma separated list of device tags for the device offering. If the offering should have multiple equal tags, a colon and the number of tags must be inserted after the tag name. For example, devicetags=tag1:2,tag2 would create a device offering with two equal tags named tag1 and one tag named tag2.")
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
        DeviceOffering deviceOffering = deviceOfferingManager.createDeviceOffering(this);
        DeviceOfferingResponse response = deviceOfferingManager.generateDeviceOfferingResponse(deviceOffering);
        response.setObjectName("deviceoffering");
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }
}
