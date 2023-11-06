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

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaStatementItemResponse;
import org.apache.cloudstack.api.response.QuotaStatementResponse;
import org.apache.cloudstack.quota.vo.QuotaUsageJoinVO;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.user.Account;

@APICommand(name = "quotaStatement", responseObject = QuotaStatementItemResponse.class, description = "Create a quota statement", since = "4.7.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaStatementCmd extends QuotaBaseCmd {

    public static final Logger s_logger = Logger.getLogger(QuotaStatementCmd.class);

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, required = true, description = "Account name for which statement will be generated.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, required = true, entityType = DomainResponse.class, description = "If domain Id is given and the caller is "
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

    @Parameter(name = ApiConstants.AGGREGATION_INTERVAL, type = CommandType.STRING, description = "Aggregation interval for the usage records. Options are None, Hourly and Daily.")
    private String aggregationInterval;

    @Parameter(name = ApiConstants.TIMEZONE, type = CommandType.STRING, description = "Timezone to be used in the response if time aggregation is used.")
    private String timezone;

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

    public ApiConstants.AggregationInterval getAggregationInterval() {
        if (StringUtils.isBlank(aggregationInterval)) {
            return ApiConstants.AggregationInterval.NONE;
        }
        try {
            String type = aggregationInterval.trim().toUpperCase();
            return ApiConstants.AggregationInterval.valueOf(type);
        } catch (IllegalArgumentException e) {
            String errMsg = String.format("Not setting aggregation interval because an invalid value was received [%s]." +
                    " Valid values are: [%s].", aggregationInterval, Arrays.toString(ApiConstants.AggregationInterval.values()));
            s_logger.warn(errMsg);
            return ApiConstants.AggregationInterval.NONE;
        }
    }

    public void setAggregationInterval(String aggregationInterval) {
        this.aggregationInterval = aggregationInterval;
    }

    public String getTimezone() {
        return ObjectUtils.defaultIfNull(timezone, "UTC");
    }

    @Override
    public long getEntityOwnerId() {
        Account activeAccountByName = _accountService.getActiveAccountByName(accountName, domainId);
        if (activeAccountByName != null) {
            return activeAccountByName.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
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
