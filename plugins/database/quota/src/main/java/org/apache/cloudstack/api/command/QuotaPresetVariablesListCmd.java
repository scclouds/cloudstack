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
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.QuotaPresetVariablesItemResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = "quotaListPresetVariables", responseObject = QuotaPresetVariablesItemResponse.class, description = "List the preset variables available for using in the " +
    "Quota tariff activation rules given the usage type.", since = "4.18.0.4-scclouds", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaPresetVariablesListCmd extends BaseCmd {

    @Inject
    QuotaResponseBuilder quotaResponseBuilder;

    @Parameter(name = ApiConstants.USAGE_TYPE, type = CommandType.STRING, required = true, description = "The usage type for which the preset variables will be retrieved.",
        since = "4.18.0.4-scclouds")
    private String quotaResourceType;

    @Override
    public void execute() {
        List<QuotaPresetVariablesItemResponse> responses = quotaResponseBuilder.listQuotaPresetVariables(this);
        ListResponse<QuotaPresetVariablesItemResponse> listResponse = new ListResponse<>();
        listResponse.setResponses(responses);
        listResponse.setResponseName(getCommandName());
        setResponseObject(listResponse);
    }

    public String getQuotaResourceType() {
        return quotaResourceType;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
