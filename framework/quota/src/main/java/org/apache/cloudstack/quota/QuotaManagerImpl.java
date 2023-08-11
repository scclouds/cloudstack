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
package org.apache.cloudstack.quota;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.user.Account;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.activationrule.presetvariables.GenericPresetVariable;
import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariableHelper;
import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariables;
import org.apache.cloudstack.quota.activationrule.presetvariables.ProcessedData;
import org.apache.cloudstack.quota.activationrule.presetvariables.Tariff;
import org.apache.cloudstack.quota.constant.ProcessingPeriod;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDetailDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageDetailVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.cloudstack.quota.vo.ResourcesToQuoteVO;
import org.apache.cloudstack.usage.UsageService;
import org.apache.cloudstack.usage.UsageTypes;
import org.apache.cloudstack.usage.UsageUnitTypes;
import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.apache.cloudstack.utils.jsinterpreter.JsInterpreter;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;

@Component
public class QuotaManagerImpl extends ManagerBase implements QuotaManager {
    private static final Logger s_logger = Logger.getLogger(QuotaManagerImpl.class.getName());

    @Inject
    private AccountDao _accountDao;
    @Inject
    private QuotaAccountDao _quotaAcc;
    @Inject
    private UsageDao _usageDao;
    @Inject
    private QuotaTariffDao _quotaTariffDao;
    @Inject
    private QuotaUsageDao _quotaUsageDao;
    @Inject
    private QuotaBalanceDao _quotaBalanceDao;
    @Inject
    private ConfigurationDao _configDao;

    @Inject
    protected QuotaUsageDetailDao quotaUsageDetailDao;

    @Inject
    protected PresetVariableHelper presetVariableHelper;

    protected TimeZone usageTimeZone = TimeZone.getTimeZone("GMT");
    static final BigDecimal GiB_DECIMAL = BigDecimal.valueOf(ByteScaleUtils.GiB);
    List<Account.Type> lockablesAccountTypes = Arrays.asList(Account.Type.NORMAL, Account.Type.DOMAIN_ADMIN);

    public QuotaManagerImpl() {
        super();
    }

    private void mergeConfigs(Map<String, String> dbParams, Map<String, Object> xmlParams) {
        for (Map.Entry<String, Object> param : xmlParams.entrySet()) {
            dbParams.put(param.getKey(), (String)param.getValue());
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        Map<String, String> configs = _configDao.getConfiguration(params);

        if (params != null) {
            mergeConfigs(configs, params);
        }

        String timeZone = ObjectUtils.defaultIfNull(_configDao.getValue(UsageService.UsageTimeZone.key()), UsageService.UsageTimeZone.defaultValue());
        usageTimeZone = TimeZone.getTimeZone(timeZone);

        return true;
    }

    @Override
    public boolean start() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Starting Quota Manager");
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Stopping Quota Manager");
        }
        return true;
    }

    protected void processQuotaBalanceForAccount(AccountVO accountVo, List<QuotaUsageVO> accountQuotaUsages) {
        String accountToString = accountVo.toString();

        if (CollectionUtils.isEmpty(accountQuotaUsages)) {
            s_logger.info(String.format("Account [%s] does not have quota usages to process. Skipping it.", accountToString));
            return;
        }

        QuotaUsageVO firstQuotaUsage = accountQuotaUsages.get(0);
        Date startDate = firstQuotaUsage.getStartDate();
        Date endDate = firstQuotaUsage.getEndDate();

        s_logger.info(String.format("Processing quota balance for account [%s] between [%s] and [%s].", accountToString, startDate,
                accountQuotaUsages.get(accountQuotaUsages.size() - 1).getEndDate()));

        BigDecimal aggregatedUsage = BigDecimal.ZERO;
        long accountId = accountVo.getAccountId();
        long domainId = accountVo.getDomainId();

        aggregatedUsage = getUsageValueAccordingToLastQuotaUsageEntryAndLastQuotaBalance(accountId, domainId, startDate, aggregatedUsage, accountToString);

        for (QuotaUsageVO quotaUsage : accountQuotaUsages) {
            Date quotaUsageStartDate = quotaUsage.getStartDate();
            Date quotaUsageEndDate = quotaUsage.getEndDate();
            BigDecimal quotaUsed = quotaUsage.getQuotaUsed();

            if (quotaUsed.equals(BigDecimal.ZERO)) {
                aggregatedUsage = aggregatedUsage.add(aggregateCreditBetweenDates(accountId, domainId, quotaUsageStartDate, quotaUsageEndDate, accountToString));
                continue;
            }

            if (startDate.compareTo(quotaUsageStartDate) == 0) {
                aggregatedUsage = aggregatedUsage.subtract(quotaUsed);
                continue;
            }

            _quotaBalanceDao.saveQuotaBalance(new QuotaBalanceVO(accountId, domainId, aggregatedUsage, endDate));

            aggregatedUsage = BigDecimal.ZERO;
            startDate = quotaUsageStartDate;
            endDate = quotaUsageEndDate;

            QuotaBalanceVO lastRealBalanceEntry = _quotaBalanceDao.getLastQuotaBalanceEntry(accountId, domainId, endDate);
            Date lastBalanceDate = new Date(0);

            if (lastRealBalanceEntry != null) {
                lastBalanceDate = lastRealBalanceEntry.getUpdatedOn();
                aggregatedUsage = aggregatedUsage.add(lastRealBalanceEntry.getCreditBalance());
            }

            aggregatedUsage = aggregatedUsage.add(aggregateCreditBetweenDates(accountId, domainId, lastBalanceDate, endDate, accountToString));
            aggregatedUsage = aggregatedUsage.subtract(quotaUsed);
        }

        _quotaBalanceDao.saveQuotaBalance(new QuotaBalanceVO(accountId, domainId, aggregatedUsage, endDate));
        saveQuotaAccount(accountId, aggregatedUsage, endDate);
    }

    protected BigDecimal getUsageValueAccordingToLastQuotaUsageEntryAndLastQuotaBalance(long accountId, long domainId, Date startDate, BigDecimal aggregatedUsage,
            String accountToString) {
        QuotaUsageVO lastQuotaUsage = _quotaUsageDao.findLastQuotaUsageEntry(accountId, domainId, startDate);

        if (lastQuotaUsage == null) {
            aggregatedUsage = aggregatedUsage.add(aggregateCreditBetweenDates(accountId, domainId, new Date(0), startDate, accountToString));
            QuotaBalanceVO firstBalance = new QuotaBalanceVO(accountId, domainId, aggregatedUsage, startDate);

            s_logger.debug(String.format("Persisting the first quota balance [%s] for account [%s].", firstBalance, accountToString));
            _quotaBalanceDao.saveQuotaBalance(firstBalance);
        } else {
            QuotaBalanceVO lastRealBalance = _quotaBalanceDao.getLastQuotaBalanceEntry(accountId, domainId, startDate);

            if (lastRealBalance != null) {
                aggregatedUsage = aggregatedUsage.add(lastRealBalance.getCreditBalance());
                aggregatedUsage = aggregatedUsage.add(aggregateCreditBetweenDates(accountId, domainId, lastRealBalance.getUpdatedOn(), startDate, accountToString));
            } else {
                s_logger.warn(String.format("Account [%s] has quota usage entries, however it does not have a quota balance.", accountToString));
            }
        }

        return aggregatedUsage;
    }

    protected void saveQuotaAccount(long accountId, BigDecimal aggregatedUsage, Date endDate) {
        QuotaAccountVO quotaAccount = _quotaAcc.findByIdQuotaAccount(accountId);

        if (quotaAccount != null) {
            quotaAccount.setQuotaBalance(aggregatedUsage);
            quotaAccount.setQuotaBalanceDate(endDate);
            _quotaAcc.updateQuotaAccount(accountId, quotaAccount);
            return;
        }

        quotaAccount = new QuotaAccountVO(accountId);
        quotaAccount.setQuotaBalance(aggregatedUsage);
        quotaAccount.setQuotaBalanceDate(endDate);
        _quotaAcc.persistQuotaAccount(quotaAccount);
    }

    protected BigDecimal aggregateCreditBetweenDates(Long accountId, Long domainId, Date startDate, Date endDate, String accountToString) {
        List<QuotaBalanceVO> creditsReceived = _quotaBalanceDao.findCreditBalances(accountId, domainId, startDate, endDate);
        s_logger.debug(String.format("Account [%s] has [%s] credit entries before [%s].", accountToString, creditsReceived.size(), endDate));

        BigDecimal aggregatedUsage = BigDecimal.ZERO;

        s_logger.debug(String.format("Aggregating the account [%s] credit entries before [%s].", accountToString, endDate));

        for (QuotaBalanceVO credit : creditsReceived) {
            aggregatedUsage = aggregatedUsage.add(credit.getCreditBalance());
        }

        s_logger.debug(String.format("The aggregation of the account [%s] credit entries before [%s] resulted in the value [%s].", accountToString, endDate, aggregatedUsage));

        return aggregatedUsage;
    }

    @Override
    public boolean calculateQuotaUsage() {
        List<AccountVO> accounts = _accountDao.listAll();
        String accountsToString = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(accounts, "id", "uuid", "accountName", "domainId");

        s_logger.info(String.format("Starting quota usage calculation for accounts [%s].", accountsToString));

        Calendar today = Calendar.getInstance();
        today.setTimeZone(usageTimeZone);
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> mapQuotaTariffsPerUsageType = createMapQuotaTariffsPerUsageType(null);

        s_logger.debug(String.format("Retrieving active monthly quota tariffs with executeOn up to [%s].", today.get(Calendar.DAY_OF_MONTH)));
        List<QuotaTariffVO> monthlyTariffs = _quotaTariffDao.listQuotaTariffsWithExecuteOnUpToTargetDate(today.get(Calendar.DAY_OF_MONTH));
        s_logger.debug(String.format("Retrieved [%s] monthly quota tariffs [%s].", monthlyTariffs.size(), monthlyTariffs));

        for (AccountVO account : accounts) {
            List<QuotaUsageVO> quotaUsages = calculateByEntryTariffs(account, mapQuotaTariffsPerUsageType);
            quotaUsages.addAll(calculateMonthlyTariffs(account, monthlyTariffs, today));
            processQuotaBalanceForAccount(account, quotaUsages);
        }

        s_logger.info(String.format("Finished quota usage calculation for accounts [%s].", accountsToString));

        return true;
    }

    protected List<QuotaUsageVO> calculateByEntryTariffs(AccountVO account, Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> mapQuotaTariffsPerUsageType) {
        s_logger.debug(String.format("Calculating BY_ENTRY tariffs for account [%s].", account.getId()));
        List<UsageVO> usageRecords = getPendingUsageRecordsForQuotaAggregation(account);

        if (usageRecords == null) {
            s_logger.debug(String.format("%s does not have pending usage records for processing 'BY_ENTRY' tariffs.", account));
            return new ArrayList<>();
        }

        return createQuotaUsagesAccordingToQuotaTariffs(account, usageRecords, mapQuotaTariffsPerUsageType);
    }

    protected List<QuotaUsageVO> calculateMonthlyTariffs(AccountVO account, List<QuotaTariffVO> monthlyTariffs, Calendar today) {
        if (!shouldCalculateUsageRecord(account, null)) {
            s_logger.debug(String.format("Skipping %s tariffs for %s since Quota is disabled for it.", ProcessingPeriod.MONTHLY, account));
            return Collections.emptyList();
        }

        s_logger.debug(String.format("Calculating %s tariffs for %s.", ProcessingPeriod.MONTHLY, account));

        Map<Long, Map<Integer, Pair<QuotaUsageVO, List<QuotaUsageDetailVO>>>> newQuotaUsages;

        try (JsInterpreter jsInterpreter = new JsInterpreter(QuotaConfig.QuotaActivationRuleTimeout.value())) {
            newQuotaUsages = createQuotaUsagesAccordingToMonthlyQuotaTariffsByResource(monthlyTariffs, today, account, jsInterpreter);
        } catch (Exception e) {
            s_logger.error(String.format("Failed to apply %s tariffs for %s due to [%s].", ProcessingPeriod.MONTHLY, account, e.getMessage()), e);
            return new ArrayList<>();
        }

        return persistMonthlyQuotaUsages(newQuotaUsages);
    }

    /**
     * Returns a map with keys ResourceID and ExecuteDate to the corresponding QuotaUsage and its Details, so we end up with one entry in table QuotaUsage per resource and
     * per execution date, with overlapping tariffs being distinguished in the Detail table, just like BY_ENTRY.
     */
    protected Map<Long, Map<Integer, Pair<QuotaUsageVO, List<QuotaUsageDetailVO>>>> createQuotaUsagesAccordingToMonthlyQuotaTariffsByResource(List<QuotaTariffVO> monthlyTariffs,
          Calendar today, AccountVO account, JsInterpreter jsInterpreter) {
        Map<Long, Map<Integer, Pair<QuotaUsageVO, List<QuotaUsageDetailVO>>>> newQuotaUsages = new HashMap<>();
        Map<Long, Map<Integer, List<Tariff>>> lastTariffs = new HashMap<>();

        monthlyTariffs.sort(Comparator.comparing(QuotaTariffVO::getPosition));

        for (QuotaTariffVO tariff : monthlyTariffs) {
            if (!isQuotaTariffOnPeriodToBeApplied(today, tariff)) {
                continue;
            }

            s_logger.debug(String.format("Applying monthly tariff [%s] to %s as its execute on is [%s].", tariff, account, tariff.getExecuteOn()));

            Pair<Date, Date> startEndDate = getProcessingPeriodStartAndEnd(today, tariff.getExecuteOn());
            Calendar cal = (Calendar) today.clone();
            cal.set(Calendar.DAY_OF_MONTH, tariff.getExecuteOn());
            Date computeDate = cal.getTime();

            s_logger.debug(String.format("Searching for usage records of type [%s] for account [%s] in period [%s] - [%s].", account.getId(), tariff.getUsageType(),
                    DateUtil.displayDateInTimezone(usageTimeZone, startEndDate.first()), DateUtil.displayDateInTimezone(usageTimeZone, startEndDate.second())));

            List<UsageVO> usageRecords = _usageDao.listAccountUsageRecordsInThePeriod(account, tariff.getUsageType(), startEndDate.first(), startEndDate.second()).first();
            if (s_logger.isTraceEnabled()) {
                s_logger.trace(String.format("Found [%s] usage records [%s].", usageRecords.size(), usageRecords));
            } else {
                s_logger.debug(String.format("Found [%s] usage records.", usageRecords.size()));
            }

            Map<Long, List<UsageVO>> mapResourceIdUsageRecords = createMapUsageRecordsPerResourceId(usageRecords);
            s_logger.debug(String.format("Resources with usage data on period are %s.", mapResourceIdUsageRecords.keySet()));
            for (Map.Entry<Long, List<UsageVO>> resourceIdToUsageRecords : mapResourceIdUsageRecords.entrySet()) {
                Long resourceId = resourceIdToUsageRecords.getKey();

                if (hasMonthlyTariffAlreadyBeenProcessedForAccountAndResourceOnCurrentMonth(account, tariff, computeDate, resourceId)) {
                    s_logger.debug(String.format("Monthly tariff [%s] has already been processed for %s and resource [%s] this month. Skipping it.", account, tariff, resourceId));
                    continue;
                }

                lastTariffs.computeIfAbsent(resourceId, k -> new HashMap<>());
                lastTariffs.get(resourceId).computeIfAbsent(tariff.getExecuteOn(), k -> new ArrayList<>());

                PresetVariables presetVariables = null;

                if (StringUtils.isNotEmpty(tariff.getActivationRule())) {
                    s_logger.trace(String.format("Loading preset variables for monthly tariff [%s].", tariff));
                    presetVariables = getMonthlyPresetVariables(resourceIdToUsageRecords.getValue());
                } else {
                    s_logger.trace(String.format("Not loading preset variables for monthly tariff [%s] as it does not have an activation rule.", tariff));
                }

                BigDecimal tariffValue = getQuotaTariffValueToBeApplied(tariff, jsInterpreter, presetVariables, lastTariffs.get(resourceId).get(tariff.getExecuteOn()));
                s_logger.debug(String.format("Monthly tariff [%s] for resource [%s] had value [%s].", tariff, resourceId, tariffValue));

                Tariff lastTariff = new Tariff();
                lastTariff.setId(tariff.getUuid());
                lastTariff.setValue(tariffValue);
                lastTariffs.get(resourceId).get(tariff.getExecuteOn()).add(lastTariff);

                newQuotaUsages.computeIfAbsent(resourceId, k -> new HashMap<>());
                Pair<QuotaUsageVO, List<QuotaUsageDetailVO>> oldQuotaUsageEntry = newQuotaUsages.get(resourceId).get(tariff.getExecuteOn());
                if (oldQuotaUsageEntry == null) {
                    QuotaUsageVO quotaUsageVo = new QuotaUsageVO();
                    quotaUsageVo.setResourceId(resourceId);
                    quotaUsageVo.setAccountId(account.getId());
                    quotaUsageVo.setDomainId(account.getDomainId());
                    quotaUsageVo.setUsageType(tariff.getUsageType());
                    quotaUsageVo.setStartDate(computeDate);
                    quotaUsageVo.setEndDate(computeDate);
                    quotaUsageVo.setQuotaUsed(BigDecimal.ZERO);
                    Pair<QuotaUsageVO, List<QuotaUsageDetailVO>> pair = new Pair<>(quotaUsageVo, new ArrayList<>());
                    newQuotaUsages.get(resourceId).put(tariff.getExecuteOn(), pair);
                    oldQuotaUsageEntry = newQuotaUsages.get(resourceId).get(tariff.getExecuteOn());
                }
                QuotaUsageDetailVO quotaUsageDetailVO = new QuotaUsageDetailVO();
                quotaUsageDetailVO.setQuotaUsed(tariffValue);
                quotaUsageDetailVO.setTariffId(tariff.getId());

                oldQuotaUsageEntry.first().setQuotaUsed(oldQuotaUsageEntry.first().getQuotaUsed().add(tariffValue));
                oldQuotaUsageEntry.second().add(quotaUsageDetailVO);
            }

        }
        return newQuotaUsages;
    }

    protected boolean isQuotaTariffOnPeriodToBeApplied(Calendar today, QuotaTariffVO tariff) {
        Calendar executeDay = (Calendar) today.clone();
        executeDay.set(Calendar.DAY_OF_MONTH, tariff.getExecuteOn());
        if (tariff.getEndDate() != null && tariff.getEndDate().before(executeDay.getTime())) {
            s_logger.debug(String.format("Monthly tariff's %s execute day is [%s], however, it ended on [%s]; not applying the tariff.", tariff.getName(),
                    DateUtil.displayDateInTimezone(usageTimeZone, executeDay.getTime()), DateUtil.displayDateInTimezone(usageTimeZone, tariff.getEndDate())));
            return false;
        }

        executeDay.set(Calendar.HOUR_OF_DAY, 23);
        executeDay.set(Calendar.MINUTE, 59);
        executeDay.set(Calendar.SECOND, 59);
        if (tariff.getEffectiveOn().after(executeDay.getTime())) {
            s_logger.debug(String.format("Monthly tariff's %s execute day is [%s], however, it starts on [%s]; not applying the tariff.", tariff.getName(),
                    DateUtil.displayDateInTimezone(usageTimeZone, executeDay.getTime()), DateUtil.displayDateInTimezone(usageTimeZone, tariff.getEffectiveOn())));
            return false;
        }

        return true;
    }

    protected Pair<Date, Date> getProcessingPeriodStartAndEnd(Calendar today, Integer executeOn) {
        Calendar cal = (Calendar) today.clone();
        cal.set(Calendar.DAY_OF_MONTH, executeOn);
        cal.add(Calendar.MONTH, -1);
        Date startDate = cal.getTime();
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.SECOND, -1);
        Date endDate = cal.getTime();
        return new Pair<>(startDate, endDate);
    }

    protected PresetVariables getMonthlyPresetVariables(List <UsageVO> usages) {
        List<ProcessedData> processedDataList = new ArrayList<>();
        for (UsageVO record : usages) {
            BigDecimal aggregatedTariffsValue = BigDecimal.ZERO;
            ProcessedData processedData = new ProcessedData();
            processedData.setUsageValue(record.getRawUsage());
            processedData.setStartDate(record.getStartDate());
            processedData.setEndDate(record.getEndDate());

            QuotaUsageVO quotaUsage = _quotaUsageDao.findByUsageItemId(record.getId());
            List<Tariff> processedDataTariffs = new ArrayList<>();
            if (quotaUsage != null) {
                List<QuotaUsageDetailVO> details = quotaUsageDetailDao.listQuotaUsageDetails(quotaUsage.getId());
                for (QuotaUsageDetailVO detail : details) {
                    Tariff detailTariff = new Tariff();
                    detailTariff.setId(detail.getTariffId().toString());
                    QuotaTariffVO foundTariff = _quotaTariffDao.findById(detail.getTariffId());
                    if (foundTariff == null) {
                        s_logger.error(String.format("Tariff [%s] of QuotaUsageDetail [%s] not found.", detail.getTariffId(), detail.getId()));
                    } else {
                        detailTariff.setName(_quotaTariffDao.findById(detail.getTariffId()).getName());
                    }
                    detailTariff.setValue(detail.getQuotaUsed());
                    aggregatedTariffsValue = aggregatedTariffsValue.add(detail.getQuotaUsed());
                    processedDataTariffs.add(detailTariff);
                }
            }
            processedData.setTariffs(processedDataTariffs);
            processedData.setAggregatedTariffsValue(aggregatedTariffsValue);
            processedDataList.add(processedData);
        }

        PresetVariables presetVariables = getPresetVariables(true, usages.get(usages.size()-1));
        presetVariables.setProcessedData(processedDataList);
        return presetVariables;
    }

    protected List<QuotaUsageVO> persistMonthlyQuotaUsages(Map<Long, Map<Integer, Pair<QuotaUsageVO, List<QuotaUsageDetailVO>>>> mapQuotaUsages) {
        List<QuotaUsageVO> quotaUsages = new ArrayList<>();

        for (Map.Entry<Long, Map<Integer, Pair<QuotaUsageVO, List<QuotaUsageDetailVO>>>> resourceToMap : mapQuotaUsages.entrySet()) {
            for (Map.Entry<Integer, Pair<QuotaUsageVO, List<QuotaUsageDetailVO>>> dayToList : resourceToMap.getValue().entrySet()) {
                QuotaUsageVO quotaUsageVO = dayToList.getValue().first();
                _quotaUsageDao.persistQuotaUsage(quotaUsageVO);
                quotaUsages.add(quotaUsageVO);
                persistQuotaUsageDetails(dayToList.getValue().second(), quotaUsageVO.getId());
            }
        }

        return quotaUsages;
    }

    protected boolean hasMonthlyTariffAlreadyBeenProcessedForAccountAndResourceOnCurrentMonth(AccountVO account, QuotaTariffVO tariff, Date monthlyEntryDate, Long resourceId) {
        List<QuotaUsageVO> quotaUsages = _quotaUsageDao.findPeriodQuotaUsage(account.getId(), account.getDomainId(), tariff.getUsageType(), monthlyEntryDate,
            monthlyEntryDate, resourceId);
        for (QuotaUsageVO quotaUsage : quotaUsages) {
            List<QuotaUsageDetailVO> details = quotaUsageDetailDao.listQuotaUsageDetails(quotaUsage.getId());
            for (QuotaUsageDetailVO detail : details) {
                if (detail.getTariffId() == tariff.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    protected Long getResourceIdAccordingToUsageType(UsageVO usageVo) {
        switch (usageVo.getUsageType()) {
            case UsageTypes.NETWORK_OFFERING:
            case UsageTypes.BACKUP:
                return usageVo.getOfferingId();
            case UsageTypes.NETWORK_BYTES_RECEIVED:
            case UsageTypes.NETWORK_BYTES_SENT:
                return usageVo.getNetworkId();
            default:
                return usageVo.getUsageId();
        }
    }

    protected Map<Long, List<UsageVO>> createMapUsageRecordsPerResourceId(List<UsageVO> usageRecords) {
        Map<Long, List<UsageVO>> mapUsageRecordsPerResourceId = new HashMap<>();
        for (UsageVO record : usageRecords) {
            Long resourceId = getResourceIdAccordingToUsageType(record);
            mapUsageRecordsPerResourceId.computeIfAbsent(resourceId, k -> new LinkedList<>());
            mapUsageRecordsPerResourceId.get(resourceId).add(record);
        }
        return mapUsageRecordsPerResourceId;
    }

    protected List<UsageVO> getPendingUsageRecordsForQuotaAggregation(AccountVO account) {
        Long accountId = account.getId();
        Long domainId = account.getDomainId();

        Pair<List<UsageVO>, Integer> usageRecords = _usageDao.listUsageRecordsPendingForQuotaAggregation(accountId, domainId);

        List<UsageVO> records = usageRecords.first();
        if (CollectionUtils.isEmpty(records)) {
            return null;
        }

        s_logger.debug(String.format("Retrieved [%s] pending usage records for %s.", usageRecords.second(), account));

        return records;
    }

    protected List<QuotaUsageVO> createQuotaUsagesAccordingToQuotaTariffs(AccountVO account, List<UsageVO> usageRecords,
            Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> mapQuotaTariffsPerUsageType) {
        String accountToString = account.toString();
        s_logger.info(String.format("Calculating quota usage of [%s] usage records for %s.", usageRecords.size(), accountToString));

        Map<UsageVO, Pair<QuotaUsageVO, List<QuotaUsageDetailVO>>> mapUsageAndQuotaUsage = new LinkedHashMap<>();

        try (JsInterpreter jsInterpreter = new JsInterpreter(QuotaConfig.QuotaActivationRuleTimeout.value())) {
            for (UsageVO usageRecord : usageRecords) {
                int usageType = usageRecord.getUsageType();

                if (!shouldCalculateUsageRecord(account, usageRecord)) {
                    mapUsageAndQuotaUsage.put(usageRecord, null);
                    continue;
                }

                Pair<List<QuotaTariffVO>, Boolean> pairQuotaTariffsPerUsageTypeAndHasActivationRule = mapQuotaTariffsPerUsageType.get(usageType);
                List<QuotaTariffVO> quotaTariffs = pairQuotaTariffsPerUsageTypeAndHasActivationRule.first();
                boolean hasAnyQuotaTariffWithActivationRule = pairQuotaTariffsPerUsageTypeAndHasActivationRule.second();

                Map<QuotaTariffVO, BigDecimal> aggregatedQuotaTariffsAndValues = aggregateQuotaTariffsValues(usageRecord, quotaTariffs, hasAnyQuotaTariffWithActivationRule,
                        jsInterpreter, accountToString);
                BigDecimal aggregatedQuotaTariffsValue = aggregatedQuotaTariffsAndValues.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                s_logger.debug(String.format("The aggregation of the quota tariffs of account [%s] resulted in [%s] for the usage record [%s].", account,
                        aggregatedQuotaTariffsValue, usageRecord));

                QuotaUsageVO quotaUsage = createQuotaUsageAccordingToUsageUnit(usageRecord, aggregatedQuotaTariffsValue, accountToString);

                List<QuotaUsageDetailVO> quotaUsageDetails = new ArrayList<>();
                if (quotaUsage != null) {
                    for (Map.Entry<QuotaTariffVO, BigDecimal> entry : aggregatedQuotaTariffsAndValues.entrySet()) {
                        QuotaUsageDetailVO quotaUsageDetail = createQuotaUsageDetail(usageRecord, entry.getKey(), entry.getValue());
                        quotaUsageDetails.add(quotaUsageDetail);
                    }
                    mapUsageAndQuotaUsage.put(usageRecord, new Pair<>(quotaUsage, quotaUsageDetails));
                } else {
                    mapUsageAndQuotaUsage.put(usageRecord, null);
                }
            }
        } catch (Exception e) {
            s_logger.error(String.format("Failed to calculate the quota usage for account [%s] due to [%s].", accountToString, e.getMessage()), e);
            return new ArrayList<>();
        }

        return persistUsagesAndQuotaUsagesAndRetrievePersistedQuotaUsages(mapUsageAndQuotaUsage);
    }

    protected boolean shouldCalculateUsageRecord(AccountVO accountVO, UsageVO usageRecord) {
        if (Boolean.FALSE.equals(QuotaConfig.QuotaAccountEnabled.valueIn(accountVO.getAccountId()))) {
            s_logger.debug(String.format("Considering usage record [%s] as calculated and skipping it because %s has the quota plugin disabled.",
                    usageRecord.toString(usageTimeZone), accountVO));
            return false;
        }
        return true;
    }


    protected List<QuotaUsageVO> persistUsagesAndQuotaUsagesAndRetrievePersistedQuotaUsages(Map<UsageVO, Pair<QuotaUsageVO, List<QuotaUsageDetailVO>>> mapUsageAndQuotaUsage) {
        List<QuotaUsageVO> quotaUsages = new ArrayList<>();

        for (Map.Entry<UsageVO, Pair<QuotaUsageVO, List<QuotaUsageDetailVO>>> usageAndQuotaUsage : mapUsageAndQuotaUsage.entrySet()) {
            UsageVO usageVo = usageAndQuotaUsage.getKey();
            usageVo.setQuotaCalculated(1);
            _usageDao.persistUsage(usageVo);

            Pair<QuotaUsageVO, List<QuotaUsageDetailVO>> pairQuotaUsageAndDetails = usageAndQuotaUsage.getValue();
            if (pairQuotaUsageAndDetails != null) {
                QuotaUsageVO quotaUsageVo = pairQuotaUsageAndDetails.first();
                _quotaUsageDao.persistQuotaUsage(quotaUsageVo);
                quotaUsages.add(quotaUsageVo);

                persistQuotaUsageDetails(pairQuotaUsageAndDetails.second(), quotaUsageVo.getId());
            }
        }

        return quotaUsages;
    }

    protected void persistQuotaUsageDetails(List<QuotaUsageDetailVO> quotaUsageDetails, Long quotaUsageId) {
        for (QuotaUsageDetailVO quotaUsageDetail : quotaUsageDetails) {
            quotaUsageDetail.setQuotaUsageId(quotaUsageId);
            quotaUsageDetailDao.persistQuotaUsageDetail(quotaUsageDetail);
        }
    }

    protected Map<QuotaTariffVO, BigDecimal> aggregateQuotaTariffsValues(UsageVO usageRecord, List<QuotaTariffVO> quotaTariffs, boolean hasAnyQuotaTariffWithActivationRule,
                                                                         JsInterpreter jsInterpreter, String accountToString) {
        String usageRecordToString = usageRecord.toString(usageTimeZone);
        s_logger.debug(String.format("Validating usage record [%s] for %s against [%s] quota tariffs.", usageRecordToString, accountToString, quotaTariffs.size()));

        PresetVariables presetVariables = getPresetVariables(hasAnyQuotaTariffWithActivationRule, usageRecord);
        Map<QuotaTariffVO, BigDecimal> aggregatedQuotaTariffsAndValues = new HashMap<>();

        quotaTariffs.sort(Comparator.comparing(QuotaTariffVO::getPosition));

        List<Tariff> lastTariffs = new ArrayList<>();
        for (QuotaTariffVO quotaTariff : quotaTariffs) {
            if (isQuotaTariffInPeriodToBeApplied(usageRecord, quotaTariff, accountToString)) {
                BigDecimal tariffValue = getQuotaTariffValueToBeApplied(quotaTariff, jsInterpreter, presetVariables, lastTariffs);

                aggregatedQuotaTariffsAndValues.put(quotaTariff, tariffValue);

                Tariff tariffPresetVariable = new Tariff();
                tariffPresetVariable.setId(quotaTariff.getUuid());
                tariffPresetVariable.setValue(tariffValue);
                lastTariffs.add(tariffPresetVariable);
            }
        }

        s_logger.debug(String.format("The aggregation of the quota tariffs resulted in [%s] quota tariffs for the usage record [%s]. The values of the quota tariffs will be used"
                + " to calculate the final usage value.", aggregatedQuotaTariffsAndValues.size(), usageRecordToString));

        return aggregatedQuotaTariffsAndValues;

    }

    protected PresetVariables getPresetVariables(boolean hasAnyQuotaTariffWithActivationRule, UsageVO usageRecord) {
        if (hasAnyQuotaTariffWithActivationRule) {
            return presetVariableHelper.getPresetVariables(usageRecord);
        }

        return null;
    }

    protected QuotaUsageDetailVO createQuotaUsageDetail(UsageVO usageRecord, QuotaTariffVO quotaTariff, BigDecimal quotaTariffValue) {

        BigDecimal quotaUsageValue = BigDecimal.ZERO;

        if (!quotaTariffValue.equals(BigDecimal.ZERO)) {
            String quotaUnit = QuotaTypes.getQuotaType(usageRecord.getUsageType()).getQuotaUnit();

            s_logger.debug(String.format("Calculating the value of the quota tariff [%s] according to its value [%s] and its quota unit [%s].", quotaTariff,
                    quotaTariffValue, quotaUnit));

            quotaUsageValue = getUsageValueAccordingToUsageUnitType(usageRecord, quotaTariffValue, quotaUnit);

            s_logger.debug(String.format("The calculation of the value of the quota tariff [%s] according to its value [%s] and its usage unit [%s] resulted in the value [%s].",
                    quotaTariff, quotaTariffValue, quotaUnit, quotaUsageValue));
        } else {
            s_logger.debug(String.format("Quota tariff [%s] has no value to be calculated; therefore, it will be marked as value zero.", quotaTariff));
        }

        QuotaUsageDetailVO quotaUsageDetailVo = new QuotaUsageDetailVO();
        quotaUsageDetailVo.setTariffId(quotaTariff.getId());
        quotaUsageDetailVo.setQuotaUsed(quotaUsageValue);

        return quotaUsageDetailVo;
    }

    /**
     * Returns the quota tariff value according to the result of the activation rule.<br/>
     * <ul>
     *   <li>If the activation rule is null or empty, returns {@link QuotaTariffVO#getCurrencyValue()}.</li>
     *   <li>If the activation rule result in a number, returns it.</li>
     *   <li>If the activation rule result in a boolean and its is true, returns {@link QuotaTariffVO#getCurrencyValue()}.</li>
     *   <li>If the activation rule result in a boolean and its is false, returns {@link BigDecimal#ZERO}.</li>
     *   <li>If the activation rule result in something else, returns {@link BigDecimal#ZERO}.</li>
     * </ul>
     */
    protected BigDecimal getQuotaTariffValueToBeApplied(QuotaTariffVO quotaTariff, JsInterpreter jsInterpreter, PresetVariables presetVariables, List<Tariff> lastAppliedTariffsList) {
        String activationRule = quotaTariff.getActivationRule();
        BigDecimal quotaTariffValue = quotaTariff.getCurrencyValue();
        String quotaTariffToString = quotaTariff.toString(usageTimeZone);

        if (StringUtils.isEmpty(activationRule)) {
            s_logger.debug(String.format("Quota tariff [%s] does not have an activation rule, therefore we will use the quota tariff value [%s] in the calculation.",
                    quotaTariffToString, quotaTariffValue));
            return quotaTariffValue;
        }

        injectPresetVariablesIntoJsInterpreter(jsInterpreter, presetVariables);
        jsInterpreter.injectVariable("lastTariffs", lastAppliedTariffsList.toString());

        String scriptResult = jsInterpreter.executeScript(activationRule).toString();

        if (NumberUtils.isParsable(scriptResult)) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace(String.format("The activation rule [%s] of quota tariff [%s] resulted in a numeric value [%s], therefore we will use it in the calculation.",
                    activationRule, quotaTariffToString, scriptResult));
            } else {
                s_logger.debug(String.format("The activation rule of quota tariff [%s] resulted in a numeric value [%s], therefore we will use it in the calculation.",
                    quotaTariffToString, scriptResult));
            }

            return new BigDecimal(scriptResult);
        }

        if (BooleanUtils.toBoolean(scriptResult)) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace(String.format("The activation rule [%s] of quota tariff [%s] had a true boolean result, therefore we will use the quota tariff's value [%s] in " +
                        "the calculation.", activationRule, quotaTariffToString, quotaTariffValue));
            } else {
                s_logger.debug(String.format("The activation rule of quota tariff [%s] had a true boolean result, therefore we will use the quota tariff's value [%s] in the " +
                        "calculation.", quotaTariffToString, quotaTariffValue));
            }

            return quotaTariffValue;
        }

        if (s_logger.isTraceEnabled()) {
            s_logger.trace(String.format("The activation rule [%s] of quota tariff [%s] resulted in [%s], therefore we will not use this quota tariff in the calculation.",
                    activationRule, quotaTariffToString, quotaTariffValue));
        } else {
            s_logger.debug(String.format("The activation rule of quota tariff [%s] resulted in [%s], therefore we will not use this quota tariff in the calculation.",
                    quotaTariffToString, quotaTariffValue));
        }

        return BigDecimal.ZERO;
    }

    /**
     * Injects the preset variables into the JS interpreter.
     */
    protected void injectPresetVariablesIntoJsInterpreter(JsInterpreter jsInterpreter, PresetVariables presetVariables) {
        if (presetVariables == null) {
            s_logger.trace("Not injecting variables into the JS interpreter because the presetVariables is null.");
            return;
        }

        jsInterpreter.discardCurrentVariables();

        injectPresetVariableToStringIfItIsNotNull(jsInterpreter, "account", presetVariables.getAccount());
        injectPresetVariableToStringIfItIsNotNull(jsInterpreter, "domain", presetVariables.getDomain());
        injectPresetVariableToStringIfItIsNotNull(jsInterpreter, "project", presetVariables.getProject());
        jsInterpreter.injectStringVariable("resourceType", presetVariables.getResourceType());
        injectPresetVariableToStringIfItIsNotNull(jsInterpreter, "value", presetVariables.getValue());
        injectPresetVariableToStringIfItIsNotNull(jsInterpreter, "zone", presetVariables.getZone());
        if (presetVariables.getProcessedData() != null) {
            jsInterpreter.injectVariable("processedData", presetVariables.getProcessedData().toString());
        }
    }

    protected void injectPresetVariableToStringIfItIsNotNull(JsInterpreter jsInterpreter, String variableName, GenericPresetVariable presetVariable) {
        if (presetVariable == null) {
            s_logger.trace(String.format("Not injecting variable [%s] into the JS interpreter because it is null.", variableName));
            return;
        }

        jsInterpreter.injectVariable(variableName, presetVariable.toString());
    }

    /**
     * Verifies if the quota tariff should be applied on the usage record according to their respectively start and end date.<br/><br/>
     */
    protected boolean isQuotaTariffInPeriodToBeApplied(UsageVO usageRecord, QuotaTariffVO quotaTariff, String accountToString) {
        Date usageRecordStartDate = usageRecord.getStartDate();
        Date usageRecordEndDate = usageRecord.getEndDate();
        Date quotaTariffStartDate = quotaTariff.getEffectiveOn();
        Date quotaTariffEndDate = quotaTariff.getEndDate();

        if ((quotaTariffEndDate != null && usageRecordStartDate.after(quotaTariffEndDate)) || usageRecordEndDate.before(quotaTariffStartDate)) {
            s_logger.debug(String.format("Not applying quota tariff [%s] in usage record [%s] of %s due to it is out of the period to be applied. Period of the usage"
                            + " record [startDate: %s, endDate: %s], period of the quota tariff [startDate: %s, endDate: %s].", quotaTariff.toString(usageTimeZone),
                    usageRecord.toString(usageTimeZone), accountToString, DateUtil.displayDateInTimezone(usageTimeZone, usageRecordStartDate),
                    DateUtil.displayDateInTimezone(usageTimeZone, usageRecordEndDate), DateUtil.displayDateInTimezone(usageTimeZone, quotaTariffStartDate),
                    DateUtil.displayDateInTimezone(usageTimeZone, quotaTariffEndDate)));

            return false;
        }

        return true;
    }

    /**
     * Creates a map of Quota Tariffs per their respective usage types and whether there is any tariff in each list with an activation rule.
     * @param usageTypes
     * @return Map of UsageType to Pair of TariffList and hasAnyQuotaTariffWithActivationRule
     */
    @Override
    public Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> createMapQuotaTariffsPerUsageType (Set<Integer> usageTypes) {
        s_logger.trace(String.format("Retrieving active BY_ENTRY quota tariffs for [%s] usage types.", usageTypes == null ? "all" : usageTypes));

        List<QuotaTariffVO> quotaTariffs = _quotaTariffDao.listQuotaTariffs(null, null, usageTypes, null, null, false, false, null, null, null,
                ProcessingPeriod.BY_ENTRY, null).first();

        s_logger.trace(String.format("Retrieved [%s] quota tariffs [%s].", quotaTariffs.size(), quotaTariffs));

        Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> mapQuotaTariffsPerUsageType = new HashMap<>();

        for (Map.Entry<Integer, QuotaTypes> entry : QuotaTypes.listQuotaTypes().entrySet()) {
            int quotaType = entry.getKey();

            List<QuotaTariffVO> quotaTariffsFiltered = quotaTariffs.stream().filter(quotaTariff -> quotaTariff.getUsageType() == quotaType).collect(Collectors.toList());
            Boolean hasAnyQuotaTariffWithActivationRule = quotaTariffsFiltered.stream().anyMatch(quotaTariff -> StringUtils.isNotEmpty(quotaTariff.getActivationRule()));

            mapQuotaTariffsPerUsageType.put(quotaType, new Pair<>(quotaTariffsFiltered, hasAnyQuotaTariffWithActivationRule));
        }

        s_logger.trace(String.format("Created a Map of quota tariffs per usage type [%s].", mapQuotaTariffsPerUsageType));
        return mapQuotaTariffsPerUsageType;
    }

    protected QuotaUsageVO createQuotaUsageAccordingToUsageUnit(UsageVO usageRecord, BigDecimal aggregatedQuotaTariffsValue, String accountToString) {
        String usageRecordToString = usageRecord.toString(usageTimeZone);

        if (aggregatedQuotaTariffsValue.equals(BigDecimal.ZERO)) {
            s_logger.debug(String.format("No tariffs were applied to usage record [%s] of %s or they resulted in 0; We will only mark the usage record as calculated.",
                    usageRecordToString, accountToString));
            return null;
        }

        QuotaTypes quotaType = QuotaTypes.listQuotaTypes().get(usageRecord.getUsageType());
        String quotaUnit = quotaType.getQuotaUnit();

        s_logger.debug(String.format("Calculating value of usage record [%s] for account [%s] according to the aggregated quota tariffs value [%s] and its usage unit [%s].",
                usageRecordToString, accountToString, aggregatedQuotaTariffsValue, quotaUnit));

        BigDecimal usageValue = getUsageValueAccordingToUsageUnitType(usageRecord, aggregatedQuotaTariffsValue, quotaUnit);

        s_logger.debug(String.format("The calculation of the usage record [%s] for account [%s] according to the aggregated quota tariffs value [%s] and its usage unit [%s] "
                + "resulted in the value [%s].", usageRecordToString, accountToString, aggregatedQuotaTariffsValue, quotaUnit, usageValue));

        QuotaUsageVO quotaUsageVo = new QuotaUsageVO();
        quotaUsageVo.setUsageItemId(usageRecord.getId());
        quotaUsageVo.setZoneId(usageRecord.getZoneId());
        quotaUsageVo.setAccountId(usageRecord.getAccountId());
        quotaUsageVo.setDomainId(usageRecord.getDomainId());
        quotaUsageVo.setUsageType(quotaType.getQuotaType());
        quotaUsageVo.setQuotaUsed(usageValue);
        quotaUsageVo.setStartDate(usageRecord.getStartDate());
        quotaUsageVo.setEndDate(usageRecord.getEndDate());
        return quotaUsageVo;
    }

    protected BigDecimal getUsageValueAccordingToUsageUnitType(UsageVO usageRecord, BigDecimal aggregatedQuotaTariffsValue, String quotaUnit) {
        BigDecimal rawUsage = BigDecimal.valueOf(usageRecord.getRawUsage());
        BigDecimal costPerHour = getCostPerHour(aggregatedQuotaTariffsValue, usageRecord.getStartDate());

        switch (UsageUnitTypes.getByDescription(quotaUnit)) {
            case COMPUTE_MONTH:
            case IP_MONTH:
            case POLICY_MONTH:
                return rawUsage.multiply(costPerHour);

            case GB:
                BigDecimal rawUsageInGb = rawUsage.divide(GiB_DECIMAL, 8, RoundingMode.HALF_EVEN);
                return rawUsageInGb.multiply(aggregatedQuotaTariffsValue);

            case GB_MONTH:
                BigDecimal gbInUse = BigDecimal.valueOf(usageRecord.getSize()).divide(GiB_DECIMAL, 8, RoundingMode.HALF_EVEN);
                return rawUsage.multiply(costPerHour).multiply(gbInUse);

            case BYTES:
            case IOPS:
                return rawUsage.multiply(aggregatedQuotaTariffsValue);

            default:
                return BigDecimal.ZERO;
        }
    }

    protected BigDecimal getCostPerHour(BigDecimal costPerMonth, Date date) {
        BigDecimal hoursInCurrentMonth = BigDecimal.valueOf(DateUtil.getHoursInCurrentMonth(date));
        s_logger.trace(String.format("Dividing tariff cost per month [%s] by [%s] to get the tariffs cost per hour.", costPerMonth, hoursInCurrentMonth));
        return costPerMonth.divide(hoursInCurrentMonth, 8, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean isLockable(AccountVO account) {
        return lockablesAccountTypes.contains(account.getType());
    }

    /**
     * Calculate the resource's value according to the current Quota tariffs and volume to quote.
     */
    @Override
    public BigDecimal getResourceRating(JsInterpreter jsInterpreter, ResourcesToQuoteVO resourceToQuote, List<QuotaTariffVO> tariffs, QuotaTypes quotaTypeObject, Date now)
            throws IllegalAccessException {
        PresetVariables metadata = resourceToQuote.getMetadata();
        String quoteId = resourceToQuote.getId();
        s_logger.trace(String.format("Handling quoting [%s] metadata fields presence to guarantee they will be injected correctly into the JS interpreter.", quoteId));

        if (metadata == null) {
            s_logger.trace(String.format("Quoting [%s] metadata is null. Skipping field presence handling.", quoteId));
        } else {
            handleFieldsPresenceInPresetVariableClasses(metadata, quoteId, "metadata");
        }

        tariffs.sort(Comparator.comparing(QuotaTariffVO::getPosition));
        BigDecimal tariffsCost = BigDecimal.ZERO;
        List<Tariff> lastTariffs = new ArrayList<>();
        for (QuotaTariffVO tariff : tariffs) {
            s_logger.trace(String.format("Processing tariff [%s] for quoting [%s].", tariff, quoteId));
            BigDecimal tariffValue = getQuotaTariffValueToBeApplied(tariff, jsInterpreter, metadata, lastTariffs);

            s_logger.trace(String.format("Tariff [%s] for quoting [%s] resulted in the cost per month [%s]. Adding it to the tariffs cost aggregator.", tariff, quoteId,
                    tariffValue));
            tariffsCost = tariffsCost.add(tariffValue);

            s_logger.trace(String.format("Adding tariff [%s] and its cost [%s] to the preset variable of last applied tariffs [%s].", tariff, tariffValue, lastTariffs));
            Tariff tariffPresetVariable = new Tariff();
            tariffPresetVariable.setId(tariff.getUuid());
            tariffPresetVariable.setValue(tariffValue);
            lastTariffs.add(tariffPresetVariable);
        }

        BigDecimal volumeToQuote = new BigDecimal(resourceToQuote.getVolumeToQuote());

        if (UsageUnitTypes.getByDescription(quotaTypeObject.getQuotaUnit()) == UsageUnitTypes.GB) {
            s_logger.debug(String.format("Multiplying the final tariffs [%s] by the volume to be quoted [%s] for quoting [%s].", tariffsCost, volumeToQuote, quoteId));
            return tariffsCost.multiply(volumeToQuote);
        }

        BigDecimal tariffsCostPerHour = getCostPerHour(tariffsCost, now);
        s_logger.debug(String.format("Multiplying the final tariffs cost per hour [%s] by the volume to be quoted [%s] for quoting [%s].", tariffsCostPerHour, volumeToQuote,
                quoteId));

        return tariffsCostPerHour.multiply(volumeToQuote);
    }

    @Override
    public Map<Integer, List<QuotaTariffVO>> getValidTariffsForQuoting(Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> allTariffsOfTheInformedTypes) {
        Date now = new Date();
        String nowAsString = DateUtil.getOutputString(now);
        Map<Integer, List<QuotaTariffVO>> result = new HashMap<>();

        s_logger.debug("Filtering quota tariffs and creating a Map of valid tariffs per usage type.");
        for (Map.Entry<Integer, Pair<List<QuotaTariffVO>, Boolean>> entry : allTariffsOfTheInformedTypes.entrySet()) {
            Pair<QuotaTypes, List<QuotaTariffVO>> pairUsageTypeAndTariffs = getValidTariffsByUsageType(now, nowAsString, entry.getKey(), entry.getValue().first());
            if (pairUsageTypeAndTariffs != null) {
                QuotaTypes quotaType = pairUsageTypeAndTariffs.first();
                List<QuotaTariffVO> filteredTariffs = pairUsageTypeAndTariffs.second();

                s_logger.trace(String.format("Adding usage type [%s] and tariffs [%s] to the map of valid tariffs.", quotaType, filteredTariffs));
                result.put(quotaType.getQuotaType(), filteredTariffs);
            }
        }

        s_logger.debug(String.format("After filtering the tariffs we have the following map as result: [%s].", result));
        return result;
    }

    protected Pair<QuotaTypes, List<QuotaTariffVO>> getValidTariffsByUsageType(Date now, String nowAsString, Integer usageType, List<QuotaTariffVO> tariffs) {
        QuotaTypes quotaType = QuotaTypes.getQuotaType(usageType);

        if (CollectionUtils.isEmpty(tariffs)) {
            s_logger.debug(String.format("Usage type [%s] does not have quota tariffs to be processed. We will not put it in the tariffs map.",
                    quotaType));
            return null;
        }

        s_logger.debug(String.format("Filtering tariffs that have the start date before [%s] and the end date null or after [%s].", nowAsString, nowAsString));
        List<QuotaTariffVO> filteredTariffs = tariffs.stream().filter(tariff -> isTariffValidForTheCurrentDatetime(now, nowAsString, tariff)).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(filteredTariffs)) {
            s_logger.debug(String.format("After filtering tariffs that have the start date before [%s] and the end date null or after [%s], no tariff remained. Therefore, we" +
                    " will not put the usage type [%s] in the tariffs map.", nowAsString, nowAsString, quotaType));
            return null;
        }

        return new Pair<>(quotaType, filteredTariffs);
    }

    protected boolean isTariffValidForTheCurrentDatetime(Date now, String nowAsString, QuotaTariffVO tariff) {
        Date startDate = tariff.getEffectiveOn();
        if (startDate.after(now)) {
            s_logger.trace(String.format("Ignoring Quota tariff [%s] because it will start [%s] after [%s] (now).", tariff, DateUtil.getOutputString(startDate), nowAsString));
            return false;
        }

        Date endDate = tariff.getEndDate();
        if (endDate != null && endDate.before(now)) {
            s_logger.trace(String.format("Ignoring Quota tariff [%s] because it ended [%s] is before [%s] (now).", tariff, DateUtil.getOutputString(startDate), nowAsString));
            return false;
        }

        return true;
    }

    /**
     * When processing quota tariffs, a JS interpreter is instantiated and some variables are injected into it. These variables are defined via the methods "set" of the object's
     * attributes and built via object's "toString". When converting a String containing a JSON to an object, via {@link com.google.gson.Gson}, it does not use the methods "set",
     * consequently not defining the variables. As a workaround for this situation, we created this method to pass through all the object attributes and define the variables, when
     * appropriate. If the object's attribute is an extension of {@link GenericPresetVariable}, it will call the method
     * {@link GenericPresetVariable#includeAllNotNullAndNonTransientFieldsInToString()}.
     */
    protected void handleFieldsPresenceInPresetVariableClasses(Object presetVariable, String quoteId, String metadataField) throws IllegalAccessException {
        Field[] fields = presetVariable.getClass().getDeclaredFields();

        for (Field field : fields) {
            handleFieldPresenceInPresetVariableClasses(presetVariable, quoteId, metadataField, field);
        }
    }

    /**
     * When processing quota tariffs, a JS interpreter is instantiated and some variables are injected into it. These variables are defined via the methods "set" of the object's
     * attributes and built via object's "toString". When converting a String containing a JSON to an object, via {@link com.google.gson.Gson}, it does not use the methods "set",
     * consequently not defining the variables. As a workaround for this situation, we created this method to pass through all the object attributes and define the variables, when
     * appropriate. If the object's attribute is an extension of {@link GenericPresetVariable}, it will call the method
     * {@link GenericPresetVariable#includeAllNotNullAndNonTransientFieldsInToString()}.
     */
    protected void handleFieldPresenceInPresetVariableClasses(Object presetVariable, String quoteId, String metadataField, Field field) throws IllegalAccessException {
        String fieldNameDotNotation = String.format("%s.%s", metadataField, field.getName());
        Class<?> fieldClass = field.getType();

        if (!GenericPresetVariable.class.isAssignableFrom(fieldClass)) {
            s_logger.trace(String.format("Field [%s], in quoting [%s], is not an extension of GenericPresetVariable. Skipping field presence handling.", fieldNameDotNotation,
                    quoteId));
            return;
        }

        field.setAccessible(true);
        GenericPresetVariable fieldValue = (GenericPresetVariable) field.get(presetVariable);
        field.setAccessible(false);

        if (fieldValue == null) {
            s_logger.trace(String.format("Field [%s], in quoting [%s], is null. Skipping field presence handling.", fieldNameDotNotation, quoteId));
            return;
        }

        fieldValue.includeAllNotNullAndNonTransientFieldsInToString();
        handleFieldsPresenceInPresetVariableClasses(fieldValue, quoteId, fieldNameDotNotation);
    }
}
