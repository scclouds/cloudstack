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

package org.apache.cloudstack.hostdevices.api.command;

import com.cloud.exception.ConcurrentOperationException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.hostdevices.api.response.ScanHostDevicesResponse;
import org.apache.cloudstack.hostdevices.service.HostDevicesManager;

import javax.inject.Inject;

@APICommand(name = "scanHostDevices",
        description = "Scans for available host PCI devices and saves them to the database",
        responseObject = ScanHostDevicesResponse.class,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin},
        since = "?")
public class ScanHostDevicesCmd extends BaseCmd {

    @Inject
    private HostDevicesManager hostDevicesManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL
    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class,
            required = false, description = "The ID of the host to scan for PCI devices")
    private Long hostId;

    @ACL
    @Parameter(name = ApiConstants.CLUSTER_ID, type = CommandType.UUID, entityType = ClusterResponse.class,
            required = false, description = "The ID of the cluster to scan for PCI devices")
    private Long clusterId;

    @ACL
    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class,
            required = false, description = "The ID of the zone to scan for PCI devices")
    private Long zoneId;

    public Long getHostId() {
        return hostId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException {
        hostDevicesManager.scanHostDevice(this);
        ScanHostDevicesResponse response = new ScanHostDevicesResponse();
        response.setObjectName("scanhostdevices");
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}
