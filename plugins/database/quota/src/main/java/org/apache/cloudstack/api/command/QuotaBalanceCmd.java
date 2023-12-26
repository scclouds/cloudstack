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

import java.util.Date;

import javax.inject.Inject;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.QuotaBalanceResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaStatementItemResponse;
import org.apache.cloudstack.quota.QuotaService;

@APICommand(name = "quotaBalance", responseObject = QuotaStatementItemResponse.class, description = "Create quota balance statements for the account.", since = "4.7.0",
requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaBalanceCmd extends BaseCmd {

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "Account's name for which statement will be generated. Deprecated, please use " +
            ApiConstants.ACCOUNT_ID + " instead.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "If the domain's id is given and the"
            + " caller is domain admin, then the statement is generated for domain.")
    private Long domainId;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, description = "Date of the last quota balance to be returned. Must be informed together with the " +
            "parameter startdate. Cannot be before startdate. " + ApiConstants.PARAMETER_DESCRIPTION_END_DATE_POSSIBLE_FORMATS)
    private Date endDate;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, description = "Date of the first quota balance to be returned. Must be before today. " +
            ApiConstants.PARAMETER_DESCRIPTION_START_DATE_POSSIBLE_FORMATS)
    private Date startDate;

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, description = "Account's id for which statement will be generated. Can not be specified with projectId.")
    private Long accountId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "Project's id for which statement will be generated. Can not be specified with accountId.")
    private Long projectId;

    @Inject
    QuotaService quotaService;

    @Inject
    QuotaResponseBuilder responseBuilder;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
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

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Override
    public long getEntityOwnerId() {
        try {
            return quotaService.finalizeAccountId(accountId, accountName, domainId, projectId);
        }
        catch (InvalidParameterValueException exception) {
            return Account.ACCOUNT_ID_SYSTEM;
        }
    }

    @Override
    public void execute() {
        QuotaBalanceResponse response = responseBuilder.createQuotaBalanceResponse(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

}
