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
package org.apache.cloudstack.api.response;

import com.cloud.user.User;
import org.apache.cloudstack.api.command.QuotaBalanceCmd;
import org.apache.cloudstack.api.command.QuotaConfigureEmailCmd;
import org.apache.cloudstack.api.command.QuotaCreditsListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateUpdateCmd;
import org.apache.cloudstack.api.command.QuotaPresetVariablesListCmd;
import org.apache.cloudstack.api.command.QuotaStatementCmd;
import org.apache.cloudstack.api.command.QuotaStatementDetailsCmd;
import org.apache.cloudstack.api.command.QuotaSummaryCmd;
import org.apache.cloudstack.api.command.QuotaTariffCreateCmd;
import org.apache.cloudstack.api.command.QuotaTariffListCmd;
import org.apache.cloudstack.api.command.QuotaTariffStatementCmd;
import org.apache.cloudstack.api.command.QuotaTariffUpdateCmd;
import org.apache.cloudstack.api.command.QuotaValidateActivationRuleCmd;
import org.apache.cloudstack.quota.vo.QuotaEmailConfigurationVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageJoinVO;

import java.util.Date;
import java.util.List;

import com.cloud.utils.Pair;
import org.apache.cloudstack.quota.vo.ResourcesToQuoteVO;

public interface QuotaResponseBuilder {

    QuotaTariffVO updateQuotaTariffPlan(QuotaTariffUpdateCmd cmd);

    Pair<List<QuotaTariffVO>, Integer> listQuotaTariffPlans(QuotaTariffListCmd cmd);

    QuotaTariffResponse createQuotaTariffResponse(QuotaTariffVO quotaTariff, boolean returnActivationRule);

    boolean isUserAllowedToSeeActivationRules(User user);

    QuotaStatementResponse createQuotaStatementResponse(List<QuotaUsageJoinVO> quotaUsage, QuotaStatementCmd cmd);

    QuotaBalanceResponse createQuotaBalanceResponse(QuotaBalanceCmd cmd);

    Pair<List<QuotaSummaryResponse>, Integer> createQuotaSummaryResponse(QuotaSummaryCmd cmd);

    List<QuotaUsageJoinVO> getQuotaUsage(QuotaStatementCmd cmd);

    QuotaCreditsResponse addQuotaCredits(Long accountId, Double amount, Long updatedBy, Boolean enforce, Date postingDate);

    List<QuotaEmailTemplateResponse> listQuotaEmailTemplates(QuotaEmailTemplateListCmd cmd);

    boolean updateQuotaEmailTemplate(QuotaEmailTemplateUpdateCmd cmd);

    Date startOfNextDay(Date dt);

    Date startOfNextDay();

    QuotaTariffVO createQuotaTariff(QuotaTariffCreateCmd cmd);

    boolean deleteQuotaTariff(String quotaTariffUuid);

    Pair<QuotaEmailConfigurationVO, Double> configureQuotaEmail(QuotaConfigureEmailCmd cmd);

    QuotaConfigureEmailResponse createQuotaConfigureEmailResponse(QuotaEmailConfigurationVO quotaEmailConfigurationVO, Double minBalance, long accountId);

    List<QuotaConfigureEmailResponse> listEmailConfiguration(long accountId);

    Pair<List<QuotaCreditsResponse>, Integer> createQuotaCreditsListResponse(QuotaCreditsListCmd cmd);

    /**
     * Quotes the resources based in the current valid Quota tariffs.
     * @param resourcesToQuoteAsJson String containing the resources to be quoted. This string will be converted to a list of
     * {@link ResourcesToQuoteVO} with Gson.
     * @return a list of {@link ResourcesQuotingResultResponse}, containing the total of each quoting.
     */
    List<ResourcesQuotingResultResponse> quoteResources(String resourcesToQuoteAsJson);

    QuotaUsageDetailsResponse listUsageDetails(QuotaStatementDetailsCmd cmd);

    /**
     * Lists the preset variables for the usage type informed in the command.
     * @param cmd used to retrieve the Quota usage type parameter.
     * @return the response consisting of a {@link List} of the preset variables and their descriptions.
     */
    List<QuotaPresetVariablesItemResponse> listQuotaPresetVariables(QuotaPresetVariablesListCmd cmd);

    QuotaTariffStatementResponse listQuotaTariffUsage(QuotaTariffStatementCmd cmd);

    QuotaValidateActivationRuleResponse validateActivationRule(QuotaValidateActivationRuleCmd cmd);
}
