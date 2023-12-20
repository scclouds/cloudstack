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
import com.cloud.utils.Pair;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaSummaryResponse;
import org.apache.cloudstack.quota.QuotaAccountStateFilter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.List;

import javax.inject.Inject;

@APICommand(name = "quotaSummary", responseObject = QuotaSummaryResponse.class, description = "\"Lists accounts' balance summary.", since = "4.7.0",
    requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaSummaryCmd extends QuotaBaseListCmd {
    public static final Logger s_logger = Logger.getLogger(QuotaSummaryCmd.class);

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "Account's name for which balance will be listed. Deprecated, please use " +
            ApiConstants.ACCOUNT_ID + " instead.")
    private String accountName;

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, description = "ID of the account for which balance will be listed. Can not be specified with projectId.",
            validations = {ApiArgValidator.UuidString})
    private Long accountId;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "ID of the account's domain.",
        validations = {ApiArgValidator.UuidString})
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "Project Id for which balance will be listed. Can not be specified with accountId.")
    private Long projectId;

    @Parameter(name = ApiConstants.LIST_ALL, type = CommandType.BOOLEAN, description = "False (default) lists balance summary for account. True lists balance summary for " +
        "accounts which the caller has access.")
    private Boolean listAll;

    @Parameter(name = ApiConstants.ACCOUNT_STATE_TO_SHOW, type = CommandType.STRING, description =  "Possible values are [ALL, ACTIVE, REMOVED]. ALL will list summaries for " +
        "active and removed accounts; ACTIVE will list summaries only for active accounts; REMOVED will list summaries only for removed accounts. The default value is ACTIVE.")
    private String accountStateToShow;

    @Inject
    QuotaResponseBuilder quotaResponseBuilder;

    @Override
    public void execute() {
        Pair<List<QuotaSummaryResponse>, Integer> responses = quotaResponseBuilder.createQuotaSummaryResponse(this);
        ListResponse<QuotaSummaryResponse> response = new ListResponse<>();
        response.setResponses(responses.first(), responses.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Boolean isListAll() {
        return BooleanUtils.toBoolean(listAll);
    }

    public void setListAll(Boolean listAll) {
        this.listAll = listAll;
    }

    public QuotaAccountStateFilter getAccountStateToShow() {
        if (StringUtils.isNotBlank(accountStateToShow)) {
            QuotaAccountStateFilter state = QuotaAccountStateFilter.getValue(accountStateToShow);
            if (state != null) {
                return state;
            }
        }

        return QuotaAccountStateFilter.ACTIVE;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
