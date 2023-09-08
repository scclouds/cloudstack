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

import com.cloud.user.AccountService;
import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaTariffResponse;
import org.apache.cloudstack.api.response.QuotaTariffStatementResponse;
import org.apache.log4j.Logger;

@APICommand(name = "quotaTariffStatement", responseObject = QuotaTariffStatementResponse.class, description = "Create a quota tariff statement", since = "4.18.0.4", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaTariffStatementCmd extends QuotaBaseCmd {

    public static final Logger s_logger = Logger.getLogger(QuotaTariffStatementCmd.class);

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "Account name for which the tariff statement will be generated.")
    private String accountName;

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, description = "Account Id for which the " +
            "tariff statement will be generated.")
    private Long accountId;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "Domain Id for which the tariff " +
            "statement will be generated.")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "Project Id for which the tariff " +
            "statement will be generated.")
    private Long projectId;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, required = true, description = "Start date range for quota query. " +
            ApiConstants.PARAMETER_DESCRIPTION_START_DATE_POSSIBLE_FORMATS)
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, required = true, description = "End date range for quota query. " +
            ApiConstants.PARAMETER_DESCRIPTION_END_DATE_POSSIBLE_FORMATS)
    private Date endDate;

    @Parameter(name = ApiConstants.TARIFF_NAME, type = CommandType.STRING, entityType = QuotaTariffResponse.class, description = "Generate the statement with only " +
            "the specified tariff.")
    private String tariffName;

    @Parameter(name = ApiConstants.USAGE_TYPE, type = CommandType.INTEGER, description = "Generate the statement with only tariffs of the specified usage type.")
    private Integer usageType;

    @Parameter(name = ApiConstants.SHOW_RESOURCES, type = CommandType.BOOLEAN, description = "List the resources of each quota tariff in the period.")
    private boolean showResources;

    @Inject
    protected QuotaResponseBuilder responseBuilder;

    @Inject
    private AccountService _accountService;

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

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getTariffName() {
        return tariffName;
    }

    public void setTariffName(String tariffName) {
        this.tariffName = tariffName;
    }

    public Integer getUsageType() {
        return usageType;
    }

    public void setUsageType(Integer usageType) {
        this.usageType = usageType;
    }

    public boolean isShowResources() {
        return showResources;
    }

    public void setShowResources(boolean showResources) {
        this.showResources = showResources;
    }

    @Override
    public long getEntityOwnerId() {
        if (accountId != null) {
            return accountId;
        }

        if (accountName != null && projectId != null) {
            throw new InvalidParameterValueException("Account and project can not be specified together");
        }

        if (accountName == null && projectId == null) {
            throw new InvalidParameterValueException("Either account or project must be specified");
        }

        return _accountService.finalyzeAccountId(accountName, domainId, projectId, true);
    }

    @Override
    public void execute() {
        QuotaTariffStatementResponse response = responseBuilder.listQuotaTariffUsage(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
