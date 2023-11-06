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
import java.util.List;

import javax.inject.Inject;

import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaStatementItemResponse;
import org.apache.cloudstack.api.response.QuotaStatementResponse;
import org.apache.cloudstack.quota.vo.QuotaUsageJoinVO;
import org.apache.log4j.Logger;

import com.cloud.user.Account;

@APICommand(name = "quotaStatement", responseObject = QuotaStatementItemResponse.class, description = "Create a quota statement", since = "4.7.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaStatementCmd extends QuotaBaseCmd {

    public static final Logger s_logger = Logger.getLogger(QuotaStatementCmd.class);

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "Account name for which statement will be generated. Deprecated, please use " +
            ApiConstants.ACCOUNT_ID + " instead.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "If domain Id is given and the caller is "
        + "domain admin then the statement is generated for domain.")
    private Long domainId;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, required = true, description = "End date range for quota query. " +
            ApiConstants.PARAMETER_DESCRIPTION_END_DATE_POSSIBLE_FORMATS)
    private Date endDate;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, required = true, description = "Start date range quota query. " +
            ApiConstants.PARAMETER_DESCRIPTION_START_DATE_POSSIBLE_FORMATS)
    private Date startDate;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.INTEGER, description = "List quota usage records for the specified usage type.")
    private Integer usageType;

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, description = "List usage records for the specified account")
    private Long accountId;

    @Parameter(name = ApiConstants.SHOW_RESOURCES, type = CommandType.BOOLEAN, description = "List the resources of each quota type in the period.")
    private boolean showResources;

    @Inject
    protected QuotaResponseBuilder responseBuilder;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Integer getUsageType() {
        return usageType;
    }

    public void setUsageType(Integer usageType) {
        this.usageType = usageType;
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

    public boolean isShowResources() {
        return showResources;
    }

    @Override
    public long getEntityOwnerId() {
        if (accountId != null) {
            if (_accountService.getActiveAccountById(accountId) != null) {
                return accountId;
            }
            return Account.ACCOUNT_ID_SYSTEM;
        }
        if (accountName == null && domainId == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("%s is required.", ApiConstants.ACCOUNT_ID));
        }
        try {
            Account activeAccount = _accountService.getActiveAccountByName(accountName, domainId);
            if (activeAccount != null) {
                return activeAccount.getId();
            }
            return Account.ACCOUNT_ID_SYSTEM;
        } catch (InvalidParameterValueException exception) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Both %s and %s are needed if using either. Consider using %s instead.",
                    ApiConstants.ACCOUNT, ApiConstants.DOMAIN_ID, ApiConstants.ACCOUNT_ID));
        }
    }

    @Override
    public void execute() {
        List<QuotaUsageJoinVO> quotaUsage = responseBuilder.getQuotaUsage(this);

        QuotaStatementResponse response = responseBuilder.createQuotaStatementResponse(quotaUsage, this);
        response.setStartDate(startDate);
        response.setEndDate(endDate);

        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

}
