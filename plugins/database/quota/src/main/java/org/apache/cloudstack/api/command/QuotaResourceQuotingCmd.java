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
import org.apache.cloudstack.api.response.QuotaResourceQuotingResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.ResourcesQuotingResultResponse;
import org.apache.cloudstack.quota.constant.QuotaConfig;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.List;

@APICommand(name = QuotaResourceQuotingCmd.API_NAME, responseObject = QuotaResourceQuotingResponse.class, description = "Quotes the resources passed as parameter according to " +
        "the volume passed as parameter and the current quota tariffs. It will return the total of the quoting and details the value of each usage type quoted. A detailed documentation " +
        "about the params can be found in " + QuotaResourceQuotingCmd.LINK_TO_QUOTA_SPEC, since = "4.16.0.7-scclouds", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaResourceQuotingCmd extends BaseCmd {
    public static final String API_NAME = "quotaResourceQuoting";
    public static final String LINK_TO_QUOTA_SPEC = "https://files.scclouds.com.br/sharing/r9g6EPcsB";

    @Parameter(name = ApiConstants.RESOURCES_TO_QUOTE, type = CommandType.STRING, required = true, length = 65535,
            description = "A JSON containing an array of objects with the following attributes:" +
                    "\n- usageType - REQUIRED - A string with the type of the resource to be quoted. The possible values are listed in the doc linked in the API description." +
                    "\n- id - OPTIONAL - A string that will be used to identify a quoting when several objects are sent. If not informed, the index of the object will be used." +
                    "\n- volumeToQuote - OPTIONAL - An integer representing the volume of resources to quote. For 'Compute*Month', 'IP*Month', and 'Policy*Month' resources, " +
                    "the volume to quote is referent to 1 resource per hour (e.g. 1 VM for 1 day = 24). For 'GB*Month', it is referent to 1 GB per hour " +
                    "(e.g. 50GB for 1 day = 1200). For 'GB', it is referent to 1 GB (e.g. 50GB = 50). If not informed, we will consider it as 0 and we will not quote the " +
                    "resource. The resources units can be found in in the doc linked in the API description." +
                    "\n- metadata - OPTIONAL - An string containing a JSON with the metadata to be injected as variables in the activation rules processing. The possible" +
                    " attributes and a sample can be found in the doc linked in the API description.")
    private String resourcesToQuoteAsJson;

    @Inject
    QuotaResponseBuilder quotaResponseBuilder;

    @Override
    public String getCommandName() {
        return API_NAME + RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        List<ResourcesQuotingResultResponse> responses = quotaResponseBuilder.quoteResources(resourcesToQuoteAsJson);
        QuotaResourceQuotingResponse response = new QuotaResourceQuotingResponse();
        response.setDetails(responses);
        response.setTotalQuote(responses.stream().map(ResourcesQuotingResultResponse::getQuote).reduce(BigDecimal.ZERO, BigDecimal::add));
        response.setCurrencySymbol(QuotaConfig.QuotaCurrencySymbol.value());
        response.setCurrencyLocale(QuotaConfig.QuotaCurrencyLocale.value());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
