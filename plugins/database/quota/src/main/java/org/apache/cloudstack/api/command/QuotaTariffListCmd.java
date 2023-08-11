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
import com.cloud.user.User;
import com.cloud.utils.Pair;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiArgValidator;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaTariffResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.quota.constant.ProcessingPeriod;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@APICommand(name = "quotaTariffList", responseObject = QuotaTariffResponse.class, description = "Lists all quota tariff plans", since = "4.7.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaTariffListCmd extends QuotaBaseListCmd {
    public static final Logger s_logger = Logger.getLogger(QuotaTariffListCmd.class);

    @Inject
    QuotaResponseBuilder _responseBuilder;

    @Parameter(name = ApiConstants.USAGE_TYPE, type = CommandType.INTEGER, description = "Usage type of the resource")
    private Integer usageType;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, description = "The start date of the quota tariff. " +
            ApiConstants.PARAMETER_DESCRIPTION_START_DATE_POSSIBLE_FORMATS)
    private Date effectiveDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, description = "The end date of the quota tariff. " +
            ApiConstants.PARAMETER_DESCRIPTION_END_DATE_POSSIBLE_FORMATS)
    private Date endDate;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "The name of the quota tariff.")
    private String name;

    @Parameter(name = ApiConstants.LIST_ALL, type = CommandType.BOOLEAN, description = "False will list only not removed quota tariffs. If set to true, we will "
            + "list all, including the removed ones. The default is false.")
    private boolean listAll = false;

    @Parameter(name = ApiConstants.LIST_ONLY_REMOVED, type = CommandType.BOOLEAN, description = "If set to true, we will list only the removed tariffs."
            + " The default is false.")
    private boolean listOnlyRemoved = false;

    @Parameter(name = ApiConstants.ID, type = CommandType.STRING, description = "The quota tariff's id.", validations = {ApiArgValidator.UuidString})
    private String id;

    @Parameter(name = "processingperiod", type = CommandType.STRING, description = "Filter by the period in which the tariff is processed. " +
            ApiConstants.PARAMETER_DESCRIPTION_QUOTA_PROCESSING_PERIOD_POSSIBLE_FORMATS)
    private String processingPeriod;

    @Parameter(name = "executeon", type = CommandType.INTEGER, description = "Filter by when the tariff is processed according to the processing period. " +
            ApiConstants.PARAMETER_DESCRIPTION_QUOTA_EXECUTE_ON_POSSIBLE_VALUES)
    private Integer executeOn;

    @Override
    public void execute() {
        final Pair<List<QuotaTariffVO>, Integer> result = _responseBuilder.listQuotaTariffPlans(this);

        User user = CallContext.current().getCallingUser();
        boolean returnActivationRules = _responseBuilder.isUserAllowedToSeeActivationRules(user);
        if (!returnActivationRules) {
            s_logger.debug(String.format("User [%s] does not have permission to create or update quota tariffs, therefore we will not return the activation rules.", user.getUuid()));
        }

        final List<QuotaTariffResponse> responses = new ArrayList<>();

        s_logger.trace(String.format("Adding quota tariffs [%s] to response of API quotaTariffList.", ReflectionToStringBuilderUtils.reflectCollection(responses)));

        for (final QuotaTariffVO resource : result.first()) {
            responses.add(_responseBuilder.createQuotaTariffResponse(resource, returnActivationRules));
        }

        final ListResponse<QuotaTariffResponse> response = new ListResponse<>();
        response.setResponses(responses, result.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public Integer getUsageType() {
        return usageType;
    }

    public Date getEndDate() {
        return endDate;
    }

    public String getName() {
        return name;
    }

    public boolean isListAll() {
        return listAll;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isListOnlyRemoved() {
        return listOnlyRemoved;
    }

    public ProcessingPeriod getProcessingPeriod() {
        if (processingPeriod == null) {
            return null;
        }

        return ProcessingPeriod.getProcessingPeriodByString(processingPeriod);
    }

    public Integer getExecuteOn() {
        if (executeOn == null || getProcessingPeriod() == null) {
            return executeOn;
        }

        if (getProcessingPeriod() == ProcessingPeriod.BY_ENTRY) {
            throw new InvalidParameterValueException(String.format("executeOn [%s] should not be informed for processing period 'BY_ENTRY'.", executeOn));
        }

        if (executeOn < 1 || executeOn > 28) {
            throw new InvalidParameterValueException(String.format("Invalid executeOn [%s] for processing period 'MONTHLY'. It must be a value between 1 and 28 (included).",
                executeOn));
        }

        return executeOn;
    }
}
