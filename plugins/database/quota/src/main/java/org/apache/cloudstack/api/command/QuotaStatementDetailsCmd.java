//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.command;

import com.cloud.user.Account;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaUsageDetailsResponse;

import javax.inject.Inject;

import java.util.Date;

@APICommand(name = "quotaStatementDetails", responseObject = QuotaUsageDetailsResponse.class, description = "Lists quota statement details by usage type and id of the resource",
        since = "4.16.0.13", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaStatementDetailsCmd extends BaseCmd {
    @Inject
    QuotaResponseBuilder responseBuilder;

    @Parameter(name = ApiConstants.ID, type = CommandType.STRING, required = true, description = "The ID of the resource.")
    private String id;

    @Parameter(name = ApiConstants.USAGE_TYPE, type = CommandType.INTEGER, required = true, description = "The usage type of the quota usage.")
    private Integer usageType;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, required = true, description = "The start date for the quota query. " +
            ApiConstants.PARAMETER_DESCRIPTION_START_DATE_POSSIBLE_FORMATS)
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, required = true, description = "The end date range for the quota query. " +
            ApiConstants.PARAMETER_DESCRIPTION_END_DATE_POSSIBLE_FORMATS)
    private Date endDate;

    @Override
    public void execute() {
        QuotaUsageDetailsResponse response = responseBuilder.listUsageDetails(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public String getId() {
        return id;
    }

    public Integer getUsageType() {
        return usageType;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

}
