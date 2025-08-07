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
package org.apache.cloudstack.api.command.user.backup.nativeoffering;

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
import org.apache.cloudstack.api.response.NativeBackupOfferingResponse;
import org.apache.cloudstack.backup.NativeBackupOffering;
import org.apache.cloudstack.backup.NativeBackupOfferingService;

import javax.inject.Inject;

@APICommand(name = "createNativeBackupOffering", description = "Creates a native backup offering", responseObject = NativeBackupOfferingResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin}, since = "4.20.0.4-scclouds")
public class CreateNativeBackupOfferingCmd extends BaseCmd {

    @Inject
    private NativeBackupOfferingService nativeBackupOfferingService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "Backup offering name.", required = true)
    private String name;

    @Parameter(name = ApiConstants.COMPRESS, type = CommandType.BOOLEAN, description = "Whether the backups should be compressed or not.")
    private Boolean compress;

    @Parameter(name = ApiConstants.VALIDATE, type = CommandType.BOOLEAN, description = "Whether the backups should be validated or not.")
    private Boolean validate;

    @Parameter(name = ApiConstants.ALLOW_QUICK_RESTORE, type = CommandType.BOOLEAN, description = "Whether the backups are allowed to be restored or not.")
    private Boolean allowQuickRestore;

    @Parameter(name = ApiConstants.ALLOW_EXTRACT_FILE, type = CommandType.BOOLEAN, description = "Whether files may be extracted from backups or not.")
    private Boolean allowExtractFile;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public boolean isCompress() {
        return Boolean.TRUE.equals(compress);
    }

    public boolean isValidate() {
        return Boolean.TRUE.equals(validate);
    }

    public boolean isAllowQuickRestore() {
        return Boolean.TRUE.equals(allowQuickRestore);
    }

    public boolean isAllowExtractFile() {
        return Boolean.TRUE.equals(allowExtractFile);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {
        NativeBackupOffering offering = nativeBackupOfferingService.createNativeBackupOffering(this);
        NativeBackupOfferingResponse response = _responseGenerator.createNativeBackupOfferingResponse(offering);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}
