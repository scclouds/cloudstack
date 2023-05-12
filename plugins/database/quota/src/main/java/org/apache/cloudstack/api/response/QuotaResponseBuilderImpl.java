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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import com.google.common.collect.Sets;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.QuotaBalanceCmd;
import org.apache.cloudstack.api.command.QuotaConfigureEmailCmd;
import org.apache.cloudstack.api.command.QuotaCreditsListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateUpdateCmd;
import org.apache.cloudstack.api.command.QuotaStatementCmd;
import org.apache.cloudstack.api.command.QuotaSummaryCmd;
import org.apache.cloudstack.api.command.QuotaTariffCreateCmd;
import org.apache.cloudstack.api.command.QuotaTariffListCmd;
import org.apache.cloudstack.api.command.QuotaTariffUpdateCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.discovery.ApiDiscoveryService;
import org.apache.cloudstack.quota.QuotaManager;
import org.apache.cloudstack.quota.QuotaService;
import org.apache.cloudstack.quota.QuotaStatement;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaCreditsDao;
import org.apache.cloudstack.quota.dao.QuotaEmailConfigurationDao;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.dao.QuotaSummaryDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaCreditsVO;
import org.apache.cloudstack.quota.vo.QuotaEmailConfigurationVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.apache.cloudstack.quota.vo.QuotaSummaryVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageJoinVO;
import org.apache.cloudstack.quota.vo.QuotaUsageResourceVO;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;

@Component
public class QuotaResponseBuilderImpl implements QuotaResponseBuilder {
    private static final Logger s_logger = Logger.getLogger(QuotaResponseBuilderImpl.class);

    @Inject
    private QuotaTariffDao _quotaTariffDao;
    @Inject
    private QuotaBalanceDao _quotaBalanceDao;
    @Inject
    private QuotaCreditsDao quotaCreditsDao;
    @Inject
    private QuotaUsageDao quotaUsageDao;

    @Inject
    private QuotaEmailTemplatesDao _quotaEmailTemplateDao;

    @Inject
    private UserDao _userDao;
    @Inject
    private QuotaService _quotaService;
    @Inject
    private AccountDao _accountDao;
    @Inject
    private DomainDao domainDao;
    @Inject
    private AccountManager _accountMgr;

    @Inject
    private QuotaStatement quotaStatement;

    @Inject
    private QuotaManager _quotaManager;

    @Inject
    private QuotaAccountDao quotaAccountDao;

    @Inject
    private QuotaEmailConfigurationDao quotaEmailConfigurationDao;

    @Inject
    private QuotaSummaryDao quotaSummaryDao;

    @Inject
    private ApiDiscoveryService apiDiscoveryService;

    @Inject
    private VMInstanceDao vmInstanceDao;

    @Inject
    private VolumeDao volumeDao;

    @Inject
    private SnapshotDao snapshotDao;

    @Inject
    private VMTemplateDao vmTemplateDao;

    @Inject
    private IPAddressDao ipAddressDao;

    @Inject
    private NetworkOfferingDao networkOfferingDao;

    @Inject
    private NetworkDao networkDao;

    private Set<Account.Type> accountTypesThatCanListAllQuotaSummaries = Sets.newHashSet(Account.Type.ADMIN, Account.Type.DOMAIN_ADMIN);

    @Override
    public QuotaTariffResponse createQuotaTariffResponse(QuotaTariffVO tariff, boolean returnActivationRule) {
        final QuotaTariffResponse response = new QuotaTariffResponse();
        response.setUsageType(tariff.getUsageType());
        response.setUsageName(tariff.getUsageName());
        response.setUsageUnit(tariff.getUsageUnit());
        response.setUsageDiscriminator(tariff.getUsageDiscriminator());
        response.setTariffValue(tariff.getCurrencyValue());
        response.setEffectiveOn(tariff.getEffectiveOn());
        response.setUsageTypeDescription(tariff.getUsageTypeDescription());
        response.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        if (returnActivationRule) {
            response.setActivationRule(tariff.getActivationRule());
        }
        response.setName(tariff.getName());
        response.setEndDate(tariff.getEndDate());
        response.setDescription(tariff.getDescription());
        response.setUuid(tariff.getUuid());
        response.setRemoved(tariff.getRemoved());
        return response;
    }

    public boolean isUserAllowedToSeeActivationRules(User user) {
        List<ApiDiscoveryResponse> apiList = (List<ApiDiscoveryResponse>) apiDiscoveryService.listApis(user, null).getResponses();
        return apiList.stream().anyMatch(response -> StringUtils.equalsAny(response.getName(), "quotaTariffCreate", "quotaTariffUpdate"));
    }

    @Override
    public QuotaBalanceResponse createQuotaBalanceResponse(QuotaBalanceCmd cmd) {
        List<QuotaBalanceVO> quotaBalances = _quotaService.listQuotaBalancesForAccount(cmd.getAccountId(), cmd.getAccountName(), cmd.getDomainId(), cmd.getStartDate(), cmd.getEndDate());

        if (CollectionUtils.isEmpty(quotaBalances)) {
            throw new InvalidParameterValueException(String.format("There are no quota balances for the parameters [%s].",
                    ReflectionToStringBuilderUtils.reflectOnlySelectedFields(cmd, "accountId", "accountName", "domainId", "startDate", "endDate")));
        }

        List<QuotaBalanceResponse> balances =
            quotaBalances
                .stream()
                .map(balance -> new QuotaBalanceResponse(balance.getUpdatedOn(), balance.getCreditBalance()))
                .collect(Collectors.toList());

        QuotaBalanceResponse response = new QuotaBalanceResponse();
        response.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        response.setBalances(balances);

        return response;
    }

    @Override
    public Pair<List<QuotaSummaryResponse>, Integer> createQuotaSummaryResponse(QuotaSummaryCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();

        if (!accountTypesThatCanListAllQuotaSummaries.contains(caller.getType()) || !cmd.isListAll()) {
            return getQuotaSummaryResponse(caller.getAccountId(), null, null, null, cmd);
        }

        return getQuotaSummaryResponseWithListAll(cmd, caller);
    }

    protected Pair<List<QuotaSummaryResponse>, Integer> getQuotaSummaryResponseWithListAll(QuotaSummaryCmd cmd, Account caller) {
        String accountName = cmd.getAccountName();
        String domainUuid = cmd.getDomainId();

        Long domainId = null;
        if (accountName != null && domainUuid == null) {
            domainId = caller.getDomainId();
        } else if (domainUuid != null) {
            DomainVO domain = domainDao.findByUuidIncludingRemoved(domainUuid);

            if (domain == null) {
                throw new InvalidParameterValueException(String.format("Domain [%s] does not exist.", domainUuid));
            }

            domainId = domain.getId();
        }

        Long accountId = getAccountIdByAccountName(accountName, domainId, caller);
        String domainPath = getDomainPathByDomainIdForDomainAdmin(caller);

        String keyword = null;
        if (Account.Type.ADMIN.equals(caller.getType())) {
            keyword = cmd.getKeyword();
        }

        return getQuotaSummaryResponse(accountId, keyword, domainId, domainPath, cmd);
    }

    protected Long getAccountIdByAccountName(String accountName, Long domainId, Account caller) {
        if (ObjectUtils.anyNull(accountName, domainId)) {
            return null;
        }

        Domain domain = domainDao.findByIdIncludingRemoved(domainId);
        _accountMgr.checkAccess(caller, domain);

        Account account = _accountDao.findAccountIncludingRemoved(accountName, domainId);

        if (account == null) {
            throw new InvalidParameterValueException(String.format("Account name [%s] or domain id [%s] is invalid.", accountName, domainId));
        }

        return account.getAccountId();
    }

    /**
     * Retrieves the domain path of the caller's domain (if the caller is Domain Admin) for filtering in the quota summary query.
     * @return null if the caller is an Admin or the domain path of the caller's domain if the caller is a Domain Admin.
     * @throws InvalidParameterValueException if it cannot find the domain.
     */
    protected String getDomainPathByDomainIdForDomainAdmin(Account caller) {
        if (caller.getType() != Account.Type.DOMAIN_ADMIN) {
            return null;
        }

        Long domainId = caller.getDomainId();
        Domain domain = domainDao.findById(domainId);
        _accountMgr.checkAccess(caller, domain);

        if (domain == null) {
            throw new InvalidParameterValueException(String.format("Domain id [%s] is invalid.", domainId));
        }

        return domain.getPath();
    }

    protected Pair<List<QuotaSummaryResponse>, Integer> getQuotaSummaryResponse(Long accountId, String accountName, Long domainId, String domainPath, QuotaSummaryCmd cmd) {
        Pair<List<QuotaSummaryVO>, Integer> pairSummaries = quotaSummaryDao.listQuotaSummariesForAccountAndOrDomain(accountId, accountName, domainId, domainPath,
            cmd.getAccountStateToShow(), cmd.getStartIndex(), cmd.getPageSizeVal());
        List<QuotaSummaryVO> summaries = pairSummaries.first();

        if (CollectionUtils.isEmpty(summaries)) {
            s_logger.info(String.format("There are no summaries to list for parameters [%s].",
                    ReflectionToStringBuilderUtils.reflectOnlySelectedFields(cmd, "accountName", "domainId", "listAll", "page", "pageSize")));
            return new Pair<>(new ArrayList<>(), 0);
        }

        List<QuotaSummaryResponse> responses = summaries.stream().map(this::getQuotaSummaryResponse).collect(Collectors.toList());

        return new Pair<>(responses, pairSummaries.second());
    }

    protected QuotaSummaryResponse getQuotaSummaryResponse(QuotaSummaryVO summary) {
        QuotaSummaryResponse response = new QuotaSummaryResponse();
        Account account = _accountDao.findByUuidIncludingRemoved(summary.getAccountUuid());

        Calendar[] period = quotaStatement.getCurrentStatementTime();
        Date startDate = period[0].getTime();
        Date endDate = period[1].getTime();
        BigDecimal quotaUsage = quotaUsageDao.findTotalQuotaUsage(account.getAccountId(), account.getDomainId(), null, startDate, endDate);

        response.setQuotaUsage(quotaUsage);
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        response.setAccountId(summary.getAccountUuid());
        response.setAccountName(summary.getAccountName());
        response.setDomainId(summary.getDomainUuid());
        response.setDomainPath(summary.getDomainPath());
        response.setBalance(summary.getQuotaBalance());
        response.setState(summary.getAccountState());
        response.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        response.setQuotaEnabled(QuotaConfig.QuotaAccountEnabled.valueIn(account.getId()));
        response.setDomainRemoved(summary.getDomainRemoved() != null);
        response.setAccountRemoved(summary.getAccountRemoved() != null);
        response.setObjectName("summary");

        if (summary.getProjectUuid() != null) {
            response.setProjectId(summary.getProjectUuid());
            response.setProjectName(summary.getProjectName());
            response.setProjectRemoved(summary.getProjectRemoved() != null);
        }

        return response;
    }

    @Override
    public QuotaStatementResponse createQuotaStatementResponse(final List<QuotaUsageJoinVO> quotaUsages, QuotaStatementCmd cmd) {
        if (CollectionUtils.isEmpty(quotaUsages)) {
            throw new InvalidParameterValueException(String.format("There is no usage data for parameters [%s].", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(cmd,
                    "accountName", "accountId", "domainId", "startDate", "endDate", "type", "showDetails")));
        }

        s_logger.debug(String.format("Creating quota statement from [%s] usage records for parameters [%s].", quotaUsages.size(),
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(cmd, "accountName", "accountId", "domainId", "startDate", "endDate", "type", "showDetails")));

        createDummyRecordForEachQuotaTypeIfUsageTypeIsNotInformed(quotaUsages, cmd.getUsageType());

        Map<Integer, List<QuotaUsageJoinVO>> recordsPerUsageTypes = quotaUsages
                .stream()
                .sorted(Comparator.comparingInt(QuotaUsageJoinVO::getUsageType))
                .collect(Collectors.groupingBy(QuotaUsageJoinVO::getUsageType));

        List<QuotaStatementItemResponse> items = new ArrayList<>();

        recordsPerUsageTypes.forEach((key, value) -> items.add(createStatementItem(key, value, cmd.isShowResources())));

        QuotaStatementResponse statement = new QuotaStatementResponse();
        statement.setLineItem(items);
        statement.setTotalQuota(items.stream().map(QuotaStatementItemResponse::getQuotaUsed).reduce(BigDecimal.ZERO, BigDecimal::add));
        statement.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        statement.setObjectName("statement");

        AccountVO account = _accountDao.findAccountByNameAndDomainIncludingRemoved(cmd.getAccountName(), cmd.getDomainId());
        DomainVO domain = domainDao.findByIdIncludingRemoved(cmd.getDomainId());

        statement.setAccountId(account.getUuid());
        statement.setAccountName(account.getAccountName());
        statement.setDomainId(domain.getUuid());

        return statement;
    }

    protected void createDummyRecordForEachQuotaTypeIfUsageTypeIsNotInformed(List<QuotaUsageJoinVO> quotaUsages, Integer usageType) {
        if (usageType != null) {
            s_logger.debug(String.format("As the usage type [%s] was informed as parameter of the API quotaStatement, we will not create dummy records.", usageType));
            return;
        }

        QuotaUsageJoinVO quotaUsage = quotaUsages.get(0);
        for (Integer quotaType : QuotaTypes.listQuotaTypes().keySet()) {
            QuotaUsageJoinVO dummy = new QuotaUsageJoinVO(quotaUsage);
            dummy.setUsageType(quotaType);
            dummy.setQuotaUsed(BigDecimal.ZERO);
            quotaUsages.add(dummy);
        }
    }

    protected QuotaStatementItemResponse createStatementItem(int usageType, List<QuotaUsageJoinVO> usageRecords, boolean showResources) {
        QuotaUsageJoinVO firstRecord = usageRecords.get(0);
        int type = firstRecord.getUsageType();

        QuotaTypes quotaType = QuotaTypes.listQuotaTypes().get(type);

        QuotaStatementItemResponse item = new QuotaStatementItemResponse(type);
        item.setQuotaUsed(usageRecords.stream().map(QuotaUsageJoinVO::getQuotaUsed).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
        item.setUsageUnit(quotaType.getQuotaUnit());
        item.setUsageName(quotaType.getQuotaName());

        setStatementItemResources(item, usageType, usageRecords, showResources);
        return item;
    }

    protected void setStatementItemResources(QuotaStatementItemResponse statementItem, int usageType, List<QuotaUsageJoinVO> quotaUsageRecords, boolean showResources) {
        if (!showResources) {
            return;
        }

        List<QuotaStatementItemResourceResponse> itemDetails = new ArrayList<>();

        Map<Long, BigDecimal> quotaUsagesValuesAggregatedById = quotaUsageRecords
                .stream()
                .filter(quotaUsageJoinVo -> getResourceIdByUsageType(quotaUsageJoinVo, usageType) != null)
                .collect(Collectors.groupingBy(
                        quotaUsageJoinVo -> getResourceIdByUsageType(quotaUsageJoinVo, usageType),
                        Collectors.reducing(new BigDecimal(0), QuotaUsageJoinVO::getQuotaUsed, BigDecimal::add)
                ));

        for (Map.Entry<Long, BigDecimal> entry : quotaUsagesValuesAggregatedById.entrySet()) {
            QuotaStatementItemResourceResponse detail = new QuotaStatementItemResourceResponse();

            detail.setQuotaUsed(entry.getValue());

            QuotaUsageResourceVO resource = getResourceFromIdAndType(entry.getKey(), usageType);
            if (resource != null) {
                detail.setResourceId(resource.getUuid());
                detail.setDisplayName(resource.getName());
                detail.setRemoved(resource.isRemoved());
            } else {
                detail.setDisplayName("<untraceable>");
            }

            itemDetails.add(detail);
        }

        statementItem.setResources(itemDetails);
    }

    protected Long getResourceIdByUsageType(QuotaUsageJoinVO quotaUsageJoinVo, int usageType) {
        switch (usageType) {
            case QuotaTypes.NETWORK_BYTES_SENT:
            case QuotaTypes.NETWORK_BYTES_RECEIVED:
                return quotaUsageJoinVo.getNetworkId();
            case QuotaTypes.NETWORK_OFFERING:
                return quotaUsageJoinVo.getOfferingId();
            default:
                return quotaUsageJoinVo.getResourceId();
        }
    }

    protected QuotaUsageResourceVO getResourceFromIdAndType(long resourceId, int usageType) {
        switch (usageType) {
            case QuotaTypes.ALLOCATED_VM:
            case QuotaTypes.RUNNING_VM:
                VMInstanceVO vmInstance = vmInstanceDao.findByIdIncludingRemoved(resourceId);
                if (vmInstance != null) {
                    return new QuotaUsageResourceVO(vmInstance.getUuid(), vmInstance.getHostName(), vmInstance.getRemoved());
                }
                break;
            case QuotaTypes.VOLUME:
            case QuotaTypes.VOLUME_SECONDARY:
            case QuotaTypes.VM_DISK_BYTES_READ:
            case QuotaTypes.VM_DISK_BYTES_WRITE:
            case QuotaTypes.VM_DISK_IO_READ:
            case QuotaTypes.VM_DISK_IO_WRITE:
                VolumeVO volume = volumeDao.findByIdIncludingRemoved(resourceId);
                if (volume != null) {
                    return new QuotaUsageResourceVO(volume.getUuid(), volume.getName(), volume.getRemoved());
                }
                break;
            case QuotaTypes.VM_SNAPSHOT_ON_PRIMARY:
            case QuotaTypes.VM_SNAPSHOT:
            case QuotaTypes.SNAPSHOT:
                SnapshotVO snapshot = snapshotDao.findByIdIncludingRemoved(resourceId);
                if (snapshot != null) {
                    return new QuotaUsageResourceVO(snapshot.getUuid(), snapshot.getName(), snapshot.getRemoved());
                }
                break;
            case QuotaTypes.NETWORK_BYTES_SENT:
            case QuotaTypes.NETWORK_BYTES_RECEIVED:
                NetworkVO network = networkDao.findByIdIncludingRemoved(resourceId);
                if (network != null) {
                    return new QuotaUsageResourceVO(network.getUuid(), network.getName(), network.getRemoved());
                }
                break;
            case QuotaTypes.TEMPLATE:
            case QuotaTypes.ISO:
                VMTemplateVO vmTemplate = vmTemplateDao.findByIdIncludingRemoved(resourceId);
                if (vmTemplate != null) {
                    return new QuotaUsageResourceVO(vmTemplate.getUuid(), vmTemplate.getName(), vmTemplate.getRemoved());
                }
                break;
            case QuotaTypes.NETWORK_OFFERING:
                NetworkOfferingVO networkOffering = networkOfferingDao.findByIdIncludingRemoved(resourceId);
                if (networkOffering != null) {
                    return new QuotaUsageResourceVO(networkOffering.getUuid(), networkOffering.getName(), networkOffering.getRemoved());
                }
                break;
            case QuotaTypes.IP_ADDRESS:
                IPAddressVO ipAddress = ipAddressDao.findByIdIncludingRemoved(resourceId);
                if (ipAddress != null) {
                    return new QuotaUsageResourceVO(ipAddress.getUuid(), ipAddress.getName(), ipAddress.getRemoved());
                }
                break;
        }

        return null;
    }

    @Override
    public Pair<List<QuotaTariffVO>, Integer> listQuotaTariffPlans(final QuotaTariffListCmd cmd) {
        Date startDate = _quotaService.computeAdjustedTime(cmd.getEffectiveDate());
        Date endDate = _quotaService.computeAdjustedTime(cmd.getEndDate());
        Integer usageType = cmd.getUsageType();
        String name = cmd.getName();
        boolean listAll = cmd.isListAll();
        Long startIndex = cmd.getStartIndex();
        Long pageSize = cmd.getPageSizeVal();
        String uuid = cmd.getUuid();
        boolean listOnlyRemoved = cmd.isListOnlyRemoved();

        s_logger.debug(String.format("Listing quota tariffs for parameters [%s].", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(cmd, "effectiveDate",
                "endDate", "listAll", "name", "page", "pageSize", "usageType", "uuid", "listOnlyRemoved")));

        return _quotaTariffDao.listQuotaTariffs(startDate, endDate, usageType, name, uuid, listAll, listOnlyRemoved, startIndex, pageSize);
    }

    @Override
    public QuotaTariffVO updateQuotaTariffPlan(QuotaTariffUpdateCmd cmd) {
        String name = cmd.getName();
        Double value = cmd.getValue();
        Date endDate = _quotaService.computeAdjustedTime(cmd.getEndDate());
        String description = cmd.getDescription();
        String activationRule = cmd.getActivationRule();
        Date now = _quotaService.computeAdjustedTime(new Date());

        warnQuotaTariffUpdateDeprecatedFields(cmd);

        QuotaTariffVO currentQuotaTariff = _quotaTariffDao.findByName(name);

        if (currentQuotaTariff == null) {
            throw new InvalidParameterValueException(String.format("There is no quota tariffs with name [%s].", name));
        }

        Date currentQuotaTariffStartDate = currentQuotaTariff.getEffectiveOn();

        currentQuotaTariff.setRemoved(now);

        QuotaTariffVO newQuotaTariff = persistNewQuotaTariff(currentQuotaTariff, name, 0, currentQuotaTariffStartDate, cmd.getEntityOwnerId(), endDate, value, description,
                activationRule);
        _quotaTariffDao.updateQuotaTariff(currentQuotaTariff);
        return newQuotaTariff;
    }

    protected void warnQuotaTariffUpdateDeprecatedFields(QuotaTariffUpdateCmd cmd) {
        String warnMessage = "The parameter 's%s' for API 'quotaTariffUpdate' is no longer needed and it will be removed in future releases.";

        if (cmd.getStartDate() != null) {
            s_logger.warn(String.format(warnMessage, "startdate"));
        }

        if (cmd.getUsageType() != null) {
            s_logger.warn(String.format(warnMessage, "usagetype"));
        }
    }

    protected QuotaTariffVO persistNewQuotaTariff(QuotaTariffVO currentQuotaTariff, String name, int usageType, Date startDate, Long entityOwnerId, Date endDate, Double value,
            String description, String activationRule) {

        QuotaTariffVO newQuotaTariff = getNewQuotaTariffObject(currentQuotaTariff, name, usageType);

        newQuotaTariff.setEffectiveOn(startDate);
        newQuotaTariff.setUpdatedOn(startDate);
        newQuotaTariff.setUpdatedBy(entityOwnerId);

        validateEndDateOnCreatingNewQuotaTariff(newQuotaTariff, startDate, endDate);
        validateValueOnCreatingNewQuotaTariff(newQuotaTariff, value);
        validateStringsOnCreatingNewQuotaTariff(newQuotaTariff::setDescription, description);
        validateStringsOnCreatingNewQuotaTariff(newQuotaTariff::setActivationRule, activationRule);

        _quotaTariffDao.addQuotaTariff(newQuotaTariff);
        return newQuotaTariff;
    }

    protected QuotaTariffVO getNewQuotaTariffObject(QuotaTariffVO currentQuotaTariff, String name, int usageType) {
        if (currentQuotaTariff != null) {
            return new QuotaTariffVO(currentQuotaTariff);
        }

        QuotaTariffVO newQuotaTariff = new QuotaTariffVO();

        if (!newQuotaTariff.setUsageTypeData(usageType)) {
            throw new InvalidParameterValueException(String.format("There is no usage type with value [%s].", usageType));
        }

        newQuotaTariff.setName(name);
        return newQuotaTariff;
    }

    protected void validateStringsOnCreatingNewQuotaTariff(Consumer<String> method, String value){
        if (value != null) {
            method.accept(value.isBlank() ? null : value);
        }
    }

    protected void validateValueOnCreatingNewQuotaTariff(QuotaTariffVO newQuotaTariff, Double value) {
        if (value != null) {
            newQuotaTariff.setCurrencyValue(BigDecimal.valueOf(value));
        }
    }

    protected void validateEndDateOnCreatingNewQuotaTariff(QuotaTariffVO newQuotaTariff, Date startDate, Date endDate) {
        if (endDate == null) {
            return;
        }

        if (endDate.compareTo(startDate) < 0) {
            throw new InvalidParameterValueException(String.format("The quota tariff's end date [%s] cannot be less than the start date [%s]", endDate, startDate));
        }

        Date now = _quotaService.computeAdjustedTime(new Date());
        if (endDate.compareTo(now) < 0) {
            throw new InvalidParameterValueException(String.format("The quota tariff's end date [%s] cannot be less than now [%s].", endDate, now));
        }

        newQuotaTariff.setEndDate(endDate);
    }

    @Override
    public QuotaCreditsResponse addQuotaCredits(Long accountId, Long domainId, Double amount, Long updatedBy, Boolean enforce) {
        Date despositedOn = _quotaService.computeAdjustedTime(new Date());
        QuotaBalanceVO qb = _quotaBalanceDao.findLaterBalanceEntry(accountId, domainId, despositedOn);

        if (qb != null) {
            throw new InvalidParameterValueException("Incorrect deposit date: " + despositedOn + " there are balance entries after this date");
        }

        QuotaCreditsVO credits = new QuotaCreditsVO(accountId, domainId, new BigDecimal(amount), updatedBy);
        credits.setUpdatedOn(despositedOn);
        QuotaCreditsVO result = quotaCreditsDao.saveCredits(credits);

        final AccountVO account = _accountDao.findById(accountId);
        if (account == null) {
            throw new InvalidParameterValueException("Account does not exist with account id " + accountId);
        }
        final boolean lockAccountEnforcement = "true".equalsIgnoreCase(QuotaConfig.QuotaEnableEnforcement.value());
        final BigDecimal currentAccountBalance = _quotaBalanceDao.getLastQuotaBalance(accountId, domainId);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("AddQuotaCredits: Depositing " + amount + " on adjusted date " + despositedOn + ", current balance " + currentAccountBalance);
        }
        // update quota account with the balance
        _quotaService.saveQuotaAccount(account, currentAccountBalance, despositedOn);
        if (lockAccountEnforcement) {
            if (currentAccountBalance.compareTo(new BigDecimal(0)) >= 0) {
                if (account.getState() == Account.State.LOCKED) {
                    s_logger.info("UnLocking account " + account.getAccountName() + " , due to positive balance " + currentAccountBalance);
                    _accountMgr.enableAccount(account.getAccountName(), domainId, accountId);
                }
            } else { // currentAccountBalance < 0 then lock the account
                if (_quotaManager.isLockable(account) && account.getState() == Account.State.ENABLED && enforce) {
                    s_logger.info("Locking account " + account.getAccountName() + " , due to negative balance " + currentAccountBalance);
                    _accountMgr.lockAccount(account.getAccountName(), domainId, accountId);
                }
            }
        }

        String creditor = String.valueOf(Account.ACCOUNT_ID_SYSTEM);
        User creditorUser = _userDao.getUser(updatedBy);
        if (creditorUser != null) {
            creditor = creditorUser.getUsername();
        }
        QuotaCreditsResponse response = new QuotaCreditsResponse();

        if (result != null) {
            response.setCredit(result.getCredit());
            response.setCreditedOn(new Date());
            response.setAccountCreditorId(creditor);
        }
        response.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        return response;
    }

    private QuotaEmailTemplateResponse createQuotaEmailResponse(QuotaEmailTemplatesVO template) {
        QuotaEmailTemplateResponse response = new QuotaEmailTemplateResponse();
        response.setTemplateType(template.getTemplateName());
        response.setTemplateSubject(template.getTemplateSubject());
        response.setTemplateText(template.getTemplateBody());
        response.setLocale(template.getLocale());
        response.setLastUpdatedOn(template.getLastUpdated());
        return response;
    }

    @Override
    public List<QuotaEmailTemplateResponse> listQuotaEmailTemplates(QuotaEmailTemplateListCmd cmd) {
        final String templateName = cmd.getTemplateName();
        List<QuotaEmailTemplatesVO> templates = _quotaEmailTemplateDao.listAllQuotaEmailTemplates(templateName);
        final List<QuotaEmailTemplateResponse> responses = new ArrayList<QuotaEmailTemplateResponse>();
        for (final QuotaEmailTemplatesVO template : templates) {
            responses.add(createQuotaEmailResponse(template));
        }
        return responses;
    }

    @Override
    public boolean updateQuotaEmailTemplate(QuotaEmailTemplateUpdateCmd cmd) {
        final String templateName = cmd.getTemplateName();
        final String templateSubject = cmd.getTemplateSubject();
        final String templateBody = cmd.getTemplateBody();
        final String locale = cmd.getLocale();

        final List<QuotaEmailTemplatesVO> templates = _quotaEmailTemplateDao.listAllQuotaEmailTemplates(templateName);
        if (templates.size() == 1) {
            final QuotaEmailTemplatesVO template = templates.get(0);
            template.setTemplateSubject(templateSubject);
            template.setTemplateBody(templateBody);
            if (locale != null) {
                template.setLocale(locale);
            }
            return _quotaEmailTemplateDao.updateQuotaEmailTemplate(template);
        }
        return false;
    }

    @Override
    public List<QuotaUsageJoinVO> getQuotaUsage(QuotaStatementCmd cmd) {
        return _quotaService.getQuotaUsage(cmd.getAccountId(), cmd.getAccountName(), cmd.getDomainId(), cmd.getUsageType(), cmd.getStartDate(), cmd.getEndDate());
    }

    @Override
    public Date startOfNextDay(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return createDateAtTheStartOfNextDay(localDate);
    }

    @Override
    public Date startOfNextDay() {
        LocalDate localDate = LocalDate.now();
        return createDateAtTheStartOfNextDay(localDate);
    }

    private Date createDateAtTheStartOfNextDay(LocalDate localDate) {
        LocalDate nextDayLocalDate = localDate.plusDays(1);
        return Date.from(nextDayLocalDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    @Override
    public QuotaTariffVO createQuotaTariff(QuotaTariffCreateCmd cmd) {
        String name = cmd.getName();
        int usageType = cmd.getUsageType();
        Date startDate = cmd.getStartDate();
        Date now = new Date();
        startDate = _quotaService.computeAdjustedTime(startDate == null ? now : startDate);
        Date endDate = _quotaService.computeAdjustedTime(cmd.getEndDate());
        Double value = cmd.getValue();
        String description = cmd.getDescription();
        String activationRule = cmd.getActivationRule();

        QuotaTariffVO currentQuotaTariff = _quotaTariffDao.findByName(name);

        if (currentQuotaTariff != null) {
            throw new InvalidParameterValueException(String.format("A quota tariff with name [%s] already exist.", name));
        }

        if (startDate.compareTo(now) < 0) {
            throw new InvalidParameterValueException(String.format("The quota tariff's start date [%s] cannot be less than now [%s]", startDate, now));
        }

        return persistNewQuotaTariff(null, name, usageType, startDate, cmd.getEntityOwnerId(), endDate, value, description, activationRule);
    }

    public boolean deleteQuotaTariff(String quotaTariffUuid) {
        QuotaTariffVO quotaTariff = _quotaTariffDao.findByUuid(quotaTariffUuid);

        if (quotaTariff == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Quota tariff with the provided UUID does not exist.");
        }

        quotaTariff.setRemoved(_quotaService.computeAdjustedTime(new Date()));
        return _quotaTariffDao.updateQuotaTariff(quotaTariff);
    }

    @Override
    public Pair<QuotaEmailConfigurationVO, Double> configureQuotaEmail(QuotaConfigureEmailCmd cmd) {
        validateQuotaConfigureEmailCmdParameters(cmd);

        Double minBalance = cmd.getMinBalance();

        if (minBalance != null) {
            _quotaService.setMinBalance(cmd.getAccountId(), cmd.getMinBalance());
        }

        if (cmd.getTemplateName() != null) {
            List<QuotaEmailTemplatesVO> templateVO = _quotaEmailTemplateDao.listAllQuotaEmailTemplates(cmd.getTemplateName());
            if (templateVO.isEmpty()) {
                throw new InvalidParameterValueException(String.format("Could not find template with name [%s].", cmd.getTemplateName()));
            }
            long templateId = templateVO.get(0).getId();
            QuotaEmailConfigurationVO configurationVO = quotaEmailConfigurationDao.findByAccountIdAndEmailTemplateId(cmd.getAccountId(), templateId);

            if (configurationVO == null) {
                configurationVO = new QuotaEmailConfigurationVO(cmd.getAccountId(), templateId, cmd.getEnable());
                quotaEmailConfigurationDao.persistQuotaEmailConfiguration(configurationVO);
                return new Pair<>(configurationVO, minBalance);
            }

            configurationVO.setEnabled(cmd.getEnable());
            return new Pair<>(quotaEmailConfigurationDao.updateQuotaEmailConfiguration(configurationVO), minBalance);
        }
        return new Pair<>(null, minBalance);
    }

    protected void validateQuotaConfigureEmailCmdParameters(QuotaConfigureEmailCmd cmd) {
        if (quotaAccountDao.findByIdQuotaAccount(cmd.getAccountId()) == null) {
            throw new InvalidParameterValueException("You must have the quota enabled for this account to configure quota emails.");
        }

        if (cmd.getTemplateName() == null && cmd.getMinBalance() == null) {
            throw new InvalidParameterValueException("You should inform at least the 'minbalance' or both the 'templatename' and 'enable' parameters.");
        }

        if ((cmd.getTemplateName() != null && cmd.getEnable() == null) || (cmd.getTemplateName() == null && cmd.getEnable() != null)) {
            throw new InvalidParameterValueException("Parameter 'enable' must be informed along with 'templatename'.");
        }
    }

    public QuotaConfigureEmailResponse createQuotaConfigureEmailResponse(QuotaEmailConfigurationVO quotaEmailConfigurationVO, Double minBalance, long accountId) {
        QuotaConfigureEmailResponse quotaConfigureEmailResponse = new QuotaConfigureEmailResponse();

        Account account = _accountDao.findByIdIncludingRemoved(accountId);
        if (quotaEmailConfigurationVO != null) {
            QuotaEmailTemplatesVO templateVO = _quotaEmailTemplateDao.findById(quotaEmailConfigurationVO.getEmailTemplateId());

            quotaConfigureEmailResponse.setAccountId(account.getUuid());
            quotaConfigureEmailResponse.setTemplateName(templateVO.getTemplateName());
            quotaConfigureEmailResponse.setEnabled(quotaEmailConfigurationVO.isEnabled());
        }

        quotaConfigureEmailResponse.setMinBalance(minBalance);

        return quotaConfigureEmailResponse;
    }

    @Override
    public List<QuotaConfigureEmailResponse> listEmailConfiguration(long accountId) {
        List<QuotaEmailConfigurationVO> emailConfigurationVOList = quotaEmailConfigurationDao.listByAccount(accountId);
        Account account = _accountDao.findById(accountId);
        QuotaAccountVO quotaAccountVO = quotaAccountDao.findByIdQuotaAccount(accountId);

        List<QuotaConfigureEmailResponse> quotaConfigureEmailResponseList = new ArrayList<>();
        for (QuotaEmailConfigurationVO quotaEmailConfigurationVO : emailConfigurationVOList) {
            quotaConfigureEmailResponseList.add(createQuotaConfigureEmailResponse(quotaEmailConfigurationVO, account, quotaAccountVO));
        }

        return quotaConfigureEmailResponseList;
    }

    @Override
    public Pair<List<QuotaCreditsResponse>, Integer> createQuotaCreditsListResponse(QuotaCreditsListCmd cmd) {
        List<QuotaCreditsVO> credits = getCreditsForQuotaCreditsList(cmd);

        Map<Long, AccountVO> mapAccount = new HashMap<>();
        List<QuotaCreditsResponse> creditResponses = credits.stream().map(credit -> getQuotaCreditsResponse(mapAccount, credit)).collect(Collectors.toList());

        return new Pair<>(creditResponses, creditResponses.size());
    }

    protected QuotaCreditsResponse getQuotaCreditsResponse(Map<Long, AccountVO> mapAccount, QuotaCreditsVO credit) {
        QuotaCreditsResponse response = new QuotaCreditsResponse();

        AccountVO account = getAccountById(credit, mapAccount);

        response.setAccountCreditorId(account.getUuid());
        response.setAccountCreditorName(account.getAccountName());
        response.setCredit(credit.getCredit());
        response.setCreditedOn(credit.getUpdatedOn());
        response.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        response.setObjectName("credit");

        return response;
    }

    protected List<QuotaCreditsVO> getCreditsForQuotaCreditsList(QuotaCreditsListCmd cmd) {
        Long accountId = cmd.getAccountId();
        Long domainId = cmd.getDomainId();
        Date startDate = cmd.getStartDate();
        Date endDate = cmd.getEndDate();

        if (startDate.after(endDate)) {
            throw new InvalidParameterValueException("The start date must be before the end date.");
        }

        List<QuotaCreditsVO> credits = quotaCreditsDao.findCredits(accountId, domainId, startDate, endDate);

        if (CollectionUtils.isNotEmpty(credits)) {
            return credits;
        }

        String message = String.format("There are no credit statements for parameters [%s].", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(cmd, "accountName",
                "domainId", "startDate", "endDate"));
        s_logger.debug(message);
        throw new InvalidParameterValueException(message);
    }

    protected AccountVO getAccountById(QuotaCreditsVO credit, Map<Long, AccountVO> mapAccount) {
        Long accountId = credit.getUpdatedBy();
        AccountVO accountVo = mapAccount.get(accountId);

        if (accountVo != null) {
            return accountVo;
        }

        accountVo = _accountDao.findByIdIncludingRemoved(accountId);

        if (accountVo == null) {
            s_logger.error(String.format("Could not find creditor account with ID [%s] for credit [%s].", accountId, credit.toString()));
            throw new InternalServerErrorException("Could not find creditor's account.");
        }

        mapAccount.put(accountId, accountVo);
        return accountVo;
    }

    protected QuotaConfigureEmailResponse createQuotaConfigureEmailResponse(QuotaEmailConfigurationVO quotaEmailConfigurationVO, Account account, QuotaAccountVO quotaAccountVO) {
        QuotaConfigureEmailResponse quotaConfigureEmailResponse = new QuotaConfigureEmailResponse();

        QuotaEmailTemplatesVO templateVO = _quotaEmailTemplateDao.findById(quotaEmailConfigurationVO.getEmailTemplateId());

        quotaConfigureEmailResponse.setAccountId(account.getUuid());
        quotaConfigureEmailResponse.setTemplateName(templateVO.getTemplateName());
        quotaConfigureEmailResponse.setEnabled(quotaEmailConfigurationVO.isEnabled());

        quotaConfigureEmailResponse.setMinBalance(quotaAccountVO.getQuotaMinBalance().doubleValue());

        return quotaConfigureEmailResponse;
    }
}
