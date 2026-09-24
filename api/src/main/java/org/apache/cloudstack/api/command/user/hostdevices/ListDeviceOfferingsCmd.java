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
import com.cloud.utils.Pair;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DeviceOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.hostdevices.DeviceOffering;
import org.apache.cloudstack.hostdevices.DeviceOfferingManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = "listDeviceOfferings",
        description = "Lists device offerings",
        responseObject = DeviceOfferingResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        since = "4.24.0",
        authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin, RoleType.User})
public class ListDeviceOfferingsCmd extends BaseListCmd {
    @Inject
    private DeviceOfferingManager deviceOfferingManager;

    @ACL
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DeviceOfferingResponse.class, description = "The ID of the device offerings")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "The name of the device offerings. This parameter can contain only a part of the device offering name")
    private String name;

    @ACL
    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "The device offering's domain ID.")
    private Long domainId;

    @ACL
    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "The device offering's zone ID.")
    private Long zoneId;

    @Parameter(name = ApiConstants.DEVICE_TAGS, type = CommandType.LIST, collectionType = CommandType.STRING, description = "The device offering's device tags. Tags informe here must match exactly the ones configured into the offerings.")
    private List<String> deviceTags;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "The state of the device offering. Possible states are: Active and Inactive. By default, devices in the Inactive state will be hidden.")
    private String state;

    @Parameter(name = ApiConstants.LIST_ALL, type = CommandType.BOOLEAN, description = "False (default) lists the device offerings accessible for calling Account. True lists all device offerings (including dedicated ones).")
    private Boolean listAll;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public List<String> getDeviceTags() {
        return deviceTags;
    }

    public String getState() {
        return state;
    }

    public Boolean getListAll() {
        return listAll;
    }

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        Pair<List<? extends DeviceOffering>, Integer> deviceOfferings = deviceOfferingManager.listDeviceOfferings(this);

        List<DeviceOfferingResponse> responseList = new ArrayList<DeviceOfferingResponse>();
        for (DeviceOffering offering : deviceOfferings.first()) {
            responseList.add(deviceOfferingManager.generateDeviceOfferingResponse(offering));
        }

        ListResponse<DeviceOfferingResponse> response = new ListResponse<>();
        response.setResponses(responseList, deviceOfferings.second());
        response.setObjectName("deviceofferings");
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
