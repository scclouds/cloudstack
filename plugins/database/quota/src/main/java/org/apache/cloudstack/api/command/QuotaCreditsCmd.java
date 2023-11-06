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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.QuotaCreditsResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.quota.QuotaService;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.Date;

@APICommand(name = "quotaCredits", responseObject = QuotaCreditsResponse.class, description = "Add +-credits to an account", since = "4.7.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaCreditsCmd extends QuotaBaseCmd {

    @Inject
    QuotaResponseBuilder _responseBuilder;

    @Inject
    QuotaService _quotaService;

    public static final Logger s_logger = Logger.getLogger(QuotaStatementCmd.class);


    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "Account name for which quota credits need to be added. Deprecated, please use " +
            ApiConstants.ACCOUNT_ID + " instead.")
    private String accountName;

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = CommandType.UUID, entityType = AccountResponse.class, description = "Account id for which quota credits need to be added")
    private Long accountId;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "Domain for which quota credits need to be added")
    private Long domainId;

    @Parameter(name = ApiConstants.VALUE, type = CommandType.DOUBLE, required = true, description = "Value of the credits to be added+, subtracted-")
    private Double value;

    @Parameter(name = "min_balance", type = CommandType.DOUBLE, required = false, description = "Minimum balance threshold of the account")
    private Double minBalance;

    @Parameter(name = "quota_enforce", type = CommandType.BOOLEAN, required = false, description = "Account for which quota enforce is set to false will not be locked when there is no credit balance")
    private Boolean quotaEnforce;

    @Parameter(name = ApiConstants.POSTING_DATE, type = CommandType.DATE, description = "Posting date of the payment. Inform null to use the current date. "
            + ApiConstants.PARAMETER_DESCRIPTION_START_DATE_POSSIBLE_FORMATS)
    private Date postingDate;

    public Double getMinBalance() {
        return minBalance;
    }

    public void setMinBalance(Double minBalance) {
        this.minBalance = minBalance;
    }

    public Boolean getQuotaEnforce() {
        return quotaEnforce;
    }

    public void setQuotaEnforce(Boolean quotaEnforce) {
        this.quotaEnforce = quotaEnforce;
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

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Date getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(Date postingDate) {
        this.postingDate = postingDate;
    }

    public QuotaCreditsCmd() {
        super();
    }

    @Override
    public void execute() {
        Account account;
        if (getAccountId() == null) {
            try{
                account = _accountService.getActiveAccountByName(getAccountName(), getDomainId());
            } catch (InvalidParameterValueException exception) {
                if (getAccountName() == null && getDomainId() == null) {
                    throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Please send a valid non-empty %s", ApiConstants.ACCOUNT_ID));
                }
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Both %s and %s are needed if using either. Consider using %s instead.",
                        ApiConstants.ACCOUNT, ApiConstants.DOMAIN_ID, ApiConstants.ACCOUNT_ID));
            }
        } else {
            account = _accountService.getActiveAccountById(getAccountId());
        }

        if (account == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "The account does not exist or has been removed/disabled.");
        }
        if (getAccountId() == null) {
            setAccountId(account.getAccountId());
        }
        if (getValue() == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Please send a valid non-empty quota value");
        }
        if (getQuotaEnforce() != null) {
            _quotaService.setLockAccount(getAccountId(), getQuotaEnforce());
        }
        if (getMinBalance() != null) {
            _quotaService.setMinBalance(getAccountId(), getMinBalance());
        }

        final QuotaCreditsResponse response = _responseBuilder.addQuotaCredits(getAccountId(), getValue(), CallContext.current().getCallingUserId(),
                getQuotaEnforce(), getPostingDate());
        response.setResponseName(getCommandName());
        response.setObjectName("quotacredits");
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
