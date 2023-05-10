// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.function.Consumer;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.google.common.collect.Sets;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.QuotaConfigureEmailCmd;
import org.apache.cloudstack.api.command.QuotaCreditsListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateUpdateCmd;
import org.apache.cloudstack.api.command.QuotaSummaryCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.quota.QuotaService;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaCreditsDao;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaCreditsVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;

import junit.framework.TestCase;

import javax.ws.rs.InternalServerErrorException;

@RunWith(PowerMockRunner.class)
public class QuotaResponseBuilderImplTest extends TestCase {

    @Mock
    QuotaTariffDao quotaTariffDaoMock;

    @Mock
    QuotaBalanceDao quotaBalanceDaoMock;

    @Mock
    QuotaCreditsDao quotaCreditsDaoMock;

    @Mock
    QuotaEmailTemplatesDao quotaEmailTemplateDaoMock;

    @Mock
    UserDao userDaoMock;

    @Mock
    QuotaService quotaServiceMock;

    @Mock
    AccountDao accountDaoMock;

    @Mock
    Consumer<String> consumerStringMock;

    @Mock
    QuotaTariffVO quotaTariffVoMock;

    @Mock
    DomainDao domainDaoMock;

    @InjectMocks
    QuotaResponseBuilderImpl quotaResponseBuilderSpy = Mockito.spy(QuotaResponseBuilderImpl.class);

    Date date = new Date();

    @Mock
    Account accountMock;

    @Mock
    QuotaConfigureEmailCmd quotaConfigureEmailCmdMock;

    @Mock
    QuotaAccountDao quotaAccountDaoMock;

    @Mock
    QuotaAccountVO quotaAccountVOMock;

    @Mock
    CallContext callContextMock;

    @Mock
    Map<Long, AccountVO> mapAccountMock;

    @Mock
    QuotaCreditsVO quotaCreditsVoMock;

    @Mock
    AccountVO accountVoMock;

    @Mock
    DomainVO domainVoMock;

    @Mock
    AccountManager accountManagerMock;

    @Mock
    Pair<List<QuotaSummaryResponse>, Integer> quotaSummaryResponseMock1, quotaSummaryResponseMock2;

    private QuotaTariffVO makeTariffTestData() {
        QuotaTariffVO tariffVO = new QuotaTariffVO();
        tariffVO.setUsageType(QuotaTypes.IP_ADDRESS);
        tariffVO.setUsageName("ip address");
        tariffVO.setUsageUnit("IP-Month");
        tariffVO.setCurrencyValue(BigDecimal.valueOf(100.19));
        tariffVO.setEffectiveOn(new Date());
        tariffVO.setUsageDiscriminator("");
        return tariffVO;
    }

    @Test
    public void testQuotaResponse() {
        QuotaTariffVO tariffVO = makeTariffTestData();
        QuotaTariffResponse response = quotaResponseBuilderSpy.createQuotaTariffResponse(tariffVO);
        assertTrue(tariffVO.getUsageType() == response.getUsageType());
        assertTrue(tariffVO.getCurrencyValue().equals(response.getTariffValue()));
    }

    @Test
    public void testAddQuotaCredits() {
        final long accountId = 2L;
        final long domainId = 1L;
        final double amount = 11.0;
        final long updatedBy = 2L;

        QuotaCreditsVO credit = new QuotaCreditsVO();
        credit.setCredit(new BigDecimal(amount));

        Mockito.when(quotaCreditsDaoMock.saveCredits(Mockito.any(QuotaCreditsVO.class))).thenReturn(credit);
        Mockito.when(quotaBalanceDaoMock.lastQuotaBalance(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(Date.class))).thenReturn(new BigDecimal(111));
        Mockito.when(quotaServiceMock.computeAdjustedTime(Mockito.any(Date.class))).thenReturn(new Date());

        AccountVO account = new AccountVO();
        account.setState(Account.State.LOCKED);
        Mockito.when(accountDaoMock.findById(Mockito.anyLong())).thenReturn(account);

        QuotaCreditsResponse resp = quotaResponseBuilderSpy.addQuotaCredits(accountId, domainId, amount, updatedBy, true);
        assertTrue(resp.getCredit().compareTo(credit.getCredit()) == 0);
    }

    @Test
    public void testListQuotaEmailTemplates() {
        QuotaEmailTemplateListCmd cmd = new QuotaEmailTemplateListCmd();
        cmd.setTemplateName("some name");
        List<QuotaEmailTemplatesVO> templates = new ArrayList<>();
        QuotaEmailTemplatesVO template = new QuotaEmailTemplatesVO();
        template.setTemplateName("template");
        templates.add(template);
        Mockito.when(quotaEmailTemplateDaoMock.listAllQuotaEmailTemplates(Mockito.anyString())).thenReturn(templates);

        Assert.assertEquals(1, quotaResponseBuilderSpy.listQuotaEmailTemplates(cmd).size());
    }

    @Test
    public void testUpdateQuotaEmailTemplate() {
        QuotaEmailTemplateUpdateCmd cmd = new QuotaEmailTemplateUpdateCmd();
        cmd.setTemplateBody("some body");
        cmd.setTemplateName("some name");
        cmd.setTemplateSubject("some subject");

        List<QuotaEmailTemplatesVO> templates = new ArrayList<>();

        Mockito.when(quotaEmailTemplateDaoMock.listAllQuotaEmailTemplates(Mockito.anyString())).thenReturn(templates);
        Mockito.when(quotaEmailTemplateDaoMock.updateQuotaEmailTemplate(Mockito.any(QuotaEmailTemplatesVO.class))).thenReturn(true);

        // invalid template test
        assertFalse(quotaResponseBuilderSpy.updateQuotaEmailTemplate(cmd));

        // valid template test
        QuotaEmailTemplatesVO template = new QuotaEmailTemplatesVO();
        template.setTemplateName("template");
        templates.add(template);
        assertTrue(quotaResponseBuilderSpy.updateQuotaEmailTemplate(cmd));
    }

    @Test
    public void testCreateQuotaLastBalanceResponse() {
        List<QuotaBalanceVO> quotaBalance = new ArrayList<>();
        // null balance test
        try {
            quotaResponseBuilderSpy.createQuotaLastBalanceResponse(null, new Date());
        } catch (InvalidParameterValueException e) {
            assertTrue(e.getMessage().equals("There are no balance entries on or before the requested date."));
        }

        // empty balance test
        try {
            quotaResponseBuilderSpy.createQuotaLastBalanceResponse(quotaBalance, new Date());
        } catch (InvalidParameterValueException e) {
            assertTrue(e.getMessage().equals("There are no balance entries on or before the requested date."));
        }

        // valid balance test
        QuotaBalanceVO entry = new QuotaBalanceVO();
        entry.setAccountId(2L);
        entry.setCreditBalance(new BigDecimal(100));
        quotaBalance.add(entry);
        quotaBalance.add(entry);
        Mockito.lenient().when(quotaServiceMock.computeAdjustedTime(Mockito.any(Date.class))).thenReturn(new Date());
        QuotaBalanceResponse resp = quotaResponseBuilderSpy.createQuotaLastBalanceResponse(quotaBalance, null);
        assertTrue(resp.getStartQuota().compareTo(new BigDecimal(200)) == 0);
    }

    @Test
    public void testStartOfNextDayWithoutParameters() {
        Date nextDate = quotaResponseBuilderSpy.startOfNextDay();

        LocalDateTime tomorrowAtStartOfTheDay = LocalDate.now().atStartOfDay().plusDays(1);
        Date expectedNextDate = Date.from(tomorrowAtStartOfTheDay.atZone(ZoneId.systemDefault()).toInstant());

        Assert.assertEquals(expectedNextDate, nextDate);
    }

    @Test
    public void testStartOfNextDayWithParameter() {
        Date anyDate = new Date(1242421545757532l);

        Date nextDayDate = quotaResponseBuilderSpy.startOfNextDay(anyDate);

        LocalDateTime nextDayLocalDateTimeAtStartOfTheDay = anyDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1).atStartOfDay();
        Date expectedNextDate = Date.from(nextDayLocalDateTimeAtStartOfTheDay.atZone(ZoneId.systemDefault()).toInstant());

        Assert.assertEquals(expectedNextDate, nextDayDate);
    }

    @Test
    public void validateStringsOnCreatingNewQuotaTariffTestNullValueDoNothing() {
        quotaResponseBuilderSpy.validateStringsOnCreatingNewQuotaTariff(consumerStringMock, null);
        Mockito.verify(consumerStringMock, Mockito.never()).accept(Mockito.anyString());
    }

    @Test
    public void validateStringsOnCreatingNewQuotaTariffTestEmptyValueCallMethodWithNull() {
        quotaResponseBuilderSpy.validateStringsOnCreatingNewQuotaTariff(consumerStringMock, "");
        Mockito.verify(consumerStringMock).accept(null);
    }

    @Test
    public void validateStringsOnCreatingNewQuotaTariffTestValueCallMethodWithValue() {
        String value = "test";
        quotaResponseBuilderSpy.validateStringsOnCreatingNewQuotaTariff(consumerStringMock, value);
        Mockito.verify(consumerStringMock).accept(value);
    }

    @Test
    public void validateValueOnCreatingNewQuotaTariffTestNullValueDoNothing() {
        quotaResponseBuilderSpy.validateValueOnCreatingNewQuotaTariff(quotaTariffVoMock, null);
        Mockito.verify(quotaTariffVoMock, Mockito.never()).setCurrencyValue(Mockito.any(BigDecimal.class));
    }

    @Test
    public void validateValueOnCreatingNewQuotaTariffTestAnyValueIsSet() {
        Double value = 0.0;
        quotaResponseBuilderSpy.validateValueOnCreatingNewQuotaTariff(quotaTariffVoMock, value);
        Mockito.verify(quotaTariffVoMock).setCurrencyValue(BigDecimal.valueOf(value));
    }

    @Test
    public void validateEndDateOnCreatingNewQuotaTariffTestNullEndDateDoNothing() {
        Date startDate = null;
        Date endDate = null;

        quotaResponseBuilderSpy.validateEndDateOnCreatingNewQuotaTariff(quotaTariffVoMock, startDate, endDate);
        Mockito.verify(quotaTariffVoMock, Mockito.never()).setEndDate(Mockito.any(Date.class));
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateEndDateOnCreatingNewQuotaTariffTestEndDateLessThanStartDateThrowInvalidParameterValueException() {
        Date startDate = date;
        Date endDate = DateUtils.addSeconds(startDate, -1);

        quotaResponseBuilderSpy.validateEndDateOnCreatingNewQuotaTariff(quotaTariffVoMock, startDate, endDate);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateEndDateOnCreatingNewQuotaTariffTestEndDateLessThanNowThrowInvalidParameterValueException() {
        Date startDate = DateUtils.addDays(date, -100);
        Date endDate = DateUtils.addDays(new Date(), -1);

        Mockito.doReturn(date).when(quotaServiceMock).computeAdjustedTime(Mockito.any(Date.class));
        quotaResponseBuilderSpy.validateEndDateOnCreatingNewQuotaTariff(quotaTariffVoMock, startDate, endDate);
    }

    @Test
    public void validateEndDateOnCreatingNewQuotaTariffTestSetValidEndDate() {
        Date startDate = DateUtils.addDays(date, -100);
        Date endDate = date;

        Mockito.doReturn(DateUtils.addDays(date, -10)).when(quotaServiceMock).computeAdjustedTime(Mockito.any(Date.class));
        quotaResponseBuilderSpy.validateEndDateOnCreatingNewQuotaTariff(quotaTariffVoMock, startDate, endDate);
        Mockito.verify(quotaTariffVoMock).setEndDate(Mockito.any(Date.class));
    }

    @Test
    @PrepareForTest(QuotaResponseBuilderImpl.class)
    public void getNewQuotaTariffObjectTestCreateFromCurrentQuotaTariff() throws Exception {
        PowerMockito.whenNew(QuotaTariffVO.class).withArguments(Mockito.any(QuotaTariffVO.class)).thenReturn(quotaTariffVoMock);

        quotaResponseBuilderSpy.getNewQuotaTariffObject(quotaTariffVoMock, "", 0);
        PowerMockito.verifyNew(QuotaTariffVO.class).withArguments(Mockito.any(QuotaTariffVO.class));
    }

    @Test (expected = InvalidParameterValueException.class)
    public void getNewQuotaTariffObjectTestSetInvalidUsageTypeThrowsInvalidParameterValueException() throws InvalidParameterValueException {
        quotaResponseBuilderSpy.getNewQuotaTariffObject(null, "test", 0);
    }

    @Test
    public void getNewQuotaTariffObjectTestReturnValidObject() throws InvalidParameterValueException {
        String name = "test";
        int usageType = 1;
        QuotaTariffVO result = quotaResponseBuilderSpy.getNewQuotaTariffObject(null, name, usageType);

        Assert.assertEquals(name, result.getName());
        Assert.assertEquals(usageType, result.getUsageType());
    }

    @Test
    public void persistNewQuotaTariffTestPersistNewQuotaTariff() {
        Mockito.doReturn(quotaTariffVoMock).when(quotaResponseBuilderSpy).getNewQuotaTariffObject(Mockito.any(QuotaTariffVO.class), Mockito.anyString(), Mockito.anyInt());
        Mockito.doNothing().when(quotaResponseBuilderSpy).validateEndDateOnCreatingNewQuotaTariff(Mockito.any(QuotaTariffVO.class), Mockito.any(Date.class), Mockito.any(Date.class));
        Mockito.doNothing().when(quotaResponseBuilderSpy).validateValueOnCreatingNewQuotaTariff(Mockito.any(QuotaTariffVO.class), Mockito.anyDouble());
        Mockito.doNothing().when(quotaResponseBuilderSpy).validateStringsOnCreatingNewQuotaTariff(Mockito.any(Consumer.class), Mockito.anyString());
        Mockito.doReturn(quotaTariffVoMock).when(quotaTariffDaoMock).addQuotaTariff(Mockito.any(QuotaTariffVO.class));

        quotaResponseBuilderSpy.persistNewQuotaTariff(quotaTariffVoMock, "", 1, date, 1l, date, 1.0, "", "");

        Mockito.verify(quotaTariffDaoMock).addQuotaTariff(Mockito.any(QuotaTariffVO.class));
    }

    @Test (expected = ServerApiException.class)
    public void deleteQuotaTariffTestQuotaDoesNotExistThrowsServerApiException() {
        Mockito.doReturn(null).when(quotaTariffDaoMock).findById(Mockito.anyLong());
        quotaResponseBuilderSpy.deleteQuotaTariff("");
    }

    @Test
    public void deleteQuotaTariffTestUpdateRemoved() {
        Mockito.doReturn(quotaTariffVoMock).when(quotaTariffDaoMock).findByUuid(Mockito.anyString());
        Mockito.doReturn(true).when(quotaTariffDaoMock).updateQuotaTariff(Mockito.any(QuotaTariffVO.class));
        Mockito.doReturn(new Date()).when(quotaServiceMock).computeAdjustedTime(Mockito.any(Date.class));

        Assert.assertTrue(quotaResponseBuilderSpy.deleteQuotaTariff(""));

        Mockito.verify(quotaTariffVoMock).setRemoved(Mockito.any(Date.class));
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateQuotaConfigureEmailCmdParametersTestNullQuotaAccount() {
        Mockito.doReturn(null).when(quotaAccountDaoMock).findByIdQuotaAccount(Mockito.any());
        quotaResponseBuilderSpy.validateQuotaConfigureEmailCmdParameters(quotaConfigureEmailCmdMock);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateQuotaConfigureEmailCmdParametersTestNullTemplateNameAndMinBalance() {
        Mockito.doReturn(quotaAccountVOMock).when(quotaAccountDaoMock).findByIdQuotaAccount(Mockito.any());
        Mockito.doReturn(null).when(quotaConfigureEmailCmdMock).getTemplateName();
        Mockito.doReturn(null).when(quotaConfigureEmailCmdMock).getMinBalance();
        quotaResponseBuilderSpy.validateQuotaConfigureEmailCmdParameters(quotaConfigureEmailCmdMock);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void validateQuotaConfigureEmailCmdParametersTestEnableNullAndTemplateNameNotNull() {
        Mockito.doReturn(quotaAccountVOMock).when(quotaAccountDaoMock).findByIdQuotaAccount(Mockito.any());
        Mockito.doReturn(QuotaConfig.QuotaEmailTemplateTypes.QUOTA_LOW.toString()).when(quotaConfigureEmailCmdMock).getTemplateName();
        Mockito.doReturn(null).when(quotaConfigureEmailCmdMock).getEnable();
        quotaResponseBuilderSpy.validateQuotaConfigureEmailCmdParameters(quotaConfigureEmailCmdMock);
    }


    @Test
    public void validateQuotaConfigureEmailCmdParametersTestNullTemplateName() {
        Mockito.doReturn(quotaAccountVOMock).when(quotaAccountDaoMock).findByIdQuotaAccount(Mockito.any());
        Mockito.doReturn(null).when(quotaConfigureEmailCmdMock).getTemplateName();
        Mockito.doReturn(null).when(quotaConfigureEmailCmdMock).getEnable();
        Mockito.doReturn(100D).when(quotaConfigureEmailCmdMock).getMinBalance();
        quotaResponseBuilderSpy.validateQuotaConfigureEmailCmdParameters(quotaConfigureEmailCmdMock);
    }

    @Test
    public void validateQuotaConfigureEmailCmdParametersTestWithTemplateNameAndEnable() {
        Mockito.doReturn(quotaAccountVOMock).when(quotaAccountDaoMock).findByIdQuotaAccount(Mockito.any());
        Mockito.doReturn(QuotaConfig.QuotaEmailTemplateTypes.QUOTA_LOW.toString()).when(quotaConfigureEmailCmdMock).getTemplateName();
        Mockito.doReturn(true).when(quotaConfigureEmailCmdMock).getEnable();
        quotaResponseBuilderSpy.validateQuotaConfigureEmailCmdParameters(quotaConfigureEmailCmdMock);
    }

    @Test
    @PrepareForTest(CallContext.class)
    public void createQuotaSummaryResponseTestNotListAllAndAllAccountTypesReturnsSingleRecord() {
        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setListAll(false);

        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(accountMock).when(callContextMock).getCallingAccount();

        Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        for (Account.Type type : Account.Type.values()) {
            Mockito.doReturn(type).when(accountMock).getType();

            Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderSpy.createQuotaSummaryResponse(cmd);
            Assert.assertEquals(quotaSummaryResponseMock1, result);
        }

        Mockito.verify(quotaResponseBuilderSpy, Mockito.times(Account.Type.values().length)).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any());
    }

    @Test
    @PrepareForTest(CallContext.class)
    public void createQuotaSummaryResponseTestListAllAndAccountTypesAdminReturnsAllAndTheRestReturnsSingleRecord() {
        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setListAll(true);

        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(accountMock).when(callContextMock).getCallingAccount();

        Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(quotaSummaryResponseMock2).when(quotaResponseBuilderSpy).getQuotaSummaryResponseWithListAll(Mockito.any(), Mockito.any());

        Set<Account.Type> accountTypesThatCanListAllQuotaSummaries = Sets.newHashSet(Account.Type.ADMIN, Account.Type.DOMAIN_ADMIN);

        for (Account.Type type : Account.Type.values()) {
            Mockito.doReturn(type).when(accountMock).getType();

            Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderSpy.createQuotaSummaryResponse(cmd);

            if (accountTypesThatCanListAllQuotaSummaries.contains(type)) {
                Assert.assertEquals(quotaSummaryResponseMock2, result);
            } else {
                Assert.assertEquals(quotaSummaryResponseMock1, result);
            }
        }

        Mockito.verify(quotaResponseBuilderSpy, Mockito.times(Account.Type.values().length - accountTypesThatCanListAllQuotaSummaries.size()))
            .getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.verify(quotaResponseBuilderSpy, Mockito.times(accountTypesThatCanListAllQuotaSummaries.size())).getQuotaSummaryResponseWithListAll(Mockito.any(), Mockito.any());
    }

    @Test
    public void getDomainPathByDomainIdForDomainAdminTestAccountNotDomainAdminReturnsNull() {
        for (Account.Type type : Account.Type.values()) {
            if (Account.Type.DOMAIN_ADMIN.equals(type)) {
                continue;
            }

            Mockito.doReturn(type).when(accountMock).getType();
            Assert.assertNull(quotaResponseBuilderSpy.getDomainPathByDomainIdForDomainAdmin(accountMock));
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getDomainPathByDomainIdForDomainAdminTestDomainFromCallerIsNullThrowsInvalidParameterValueException() {
        Mockito.doReturn(Account.Type.DOMAIN_ADMIN).when(accountMock).getType();
        Mockito.doReturn(null).when(domainDaoMock).findById(Mockito.anyLong());
        Mockito.doNothing().when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));

        quotaResponseBuilderSpy.getDomainPathByDomainIdForDomainAdmin(accountMock);
    }

    @Test
    public void getDomainPathByDomainIdForDomainAdminTestDomainFromCallerIsNotNullReturnsPath() {
        String expected = "/test/";

        Mockito.doReturn(Account.Type.DOMAIN_ADMIN).when(accountMock).getType();
        Mockito.doReturn(domainVoMock).when(domainDaoMock).findById(Mockito.anyLong());
        Mockito.doNothing().when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));
        Mockito.doReturn(expected).when(domainVoMock).getPath();

        String result = quotaResponseBuilderSpy.getDomainPathByDomainIdForDomainAdmin(accountMock);
        Assert.assertEquals(expected, result);
    }

    public void getAccountByIdTestMapHasAccountReturnIt() {
        Mockito.doReturn(1l).when(quotaCreditsVoMock).getUpdatedBy();
        Mockito.doReturn(accountVoMock).when(mapAccountMock).get(Mockito.any());
        AccountVO result = quotaResponseBuilderSpy.getAccountById(quotaCreditsVoMock, mapAccountMock);

        Assert.assertEquals(accountVoMock, result);
    }

    @Test(expected = InternalServerErrorException.class)
    public void getAccountByIdTestFindByIdIncludingRemovedReturnsNullThrowInternalServerErrorException() {
        Mockito.doReturn(1l).when(quotaCreditsVoMock).getUpdatedBy();
        Mockito.doReturn(null).when(mapAccountMock).get(Mockito.any());
        Mockito.doReturn(null).when(accountDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        quotaResponseBuilderSpy.getAccountById(quotaCreditsVoMock, mapAccountMock);
    }

    @Test
    public void getAccountByIdTestFindByIdIncludingRemovedReturnsAccountAddToMapAndReturnIt() {
        Map<Long, AccountVO> mapAccount = new HashMap<>();

        long updatedBy = 1l;
        Mockito.doReturn(updatedBy).when(quotaCreditsVoMock).getUpdatedBy();
        Mockito.doReturn(null).when(mapAccountMock).get(Mockito.any());
        Mockito.doReturn(accountVoMock).when(accountDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        AccountVO result = quotaResponseBuilderSpy.getAccountById(quotaCreditsVoMock, mapAccount);

        Assert.assertEquals(accountVoMock, result);
        Assert.assertEquals(accountVoMock, mapAccount.get(updatedBy));
    }

    @Test
    public void getQuotaCreditsResponseTestReturnsObject() {
        QuotaCreditsResponse expected = new QuotaCreditsResponse();

        expected.setAccountCreditorId("test_uuid");
        expected.setAccountCreditorName("test_name");
        expected.setCredit(new BigDecimal("41.5"));
        expected.setCreditedOn(new Date());
        expected.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        expected.setObjectName("credit");

        Mockito.doReturn(accountVoMock).when(quotaResponseBuilderSpy).getAccountById(Mockito.any(), Mockito.any());
        Mockito.doReturn(expected.getAccountCreditorId()).when(accountVoMock).getUuid();
        Mockito.doReturn(expected.getAccountCreditorName()).when(accountVoMock).getAccountName();
        Mockito.doReturn(expected.getCredit()).when(quotaCreditsVoMock).getCredit();
        Mockito.doReturn(expected.getCreditedOn()).when(quotaCreditsVoMock).getUpdatedOn();

        QuotaCreditsResponse result = quotaResponseBuilderSpy.getQuotaCreditsResponse(mapAccountMock, quotaCreditsVoMock);

        Assert.assertEquals(expected.getAccountCreditorId(), result.getAccountCreditorId());
        Assert.assertEquals(expected.getAccountCreditorName(), result.getAccountCreditorName());
        Assert.assertEquals(expected.getCredit(), result.getCredit());
        Assert.assertEquals(expected.getCreditedOn(), result.getCreditedOn());
        Assert.assertEquals(expected.getCurrency(), result.getCurrency());
        Assert.assertEquals(expected.getObjectName(), result.getObjectName());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getCreditsForQuotaCreditsListTestStartDateIsAfterEndDateThrowsInvalidParameterValueException() {
        QuotaCreditsListCmd cmd = getQuotaCreditsListCmdForTests();
        cmd.setStartDate(new Date());
        cmd.setEndDate(DateUtils.addDays(new Date(), -1));

        quotaResponseBuilderSpy.getCreditsForQuotaCreditsList(cmd);
    }

    @Test
    public void getCreditsForQuotaCreditsListTestFindCreditsReturnsData() {
        List<QuotaCreditsVO> expected = new ArrayList<>();
        expected.add(new QuotaCreditsVO());

        QuotaCreditsListCmd cmd = getQuotaCreditsListCmdForTests();

        Mockito.doReturn(expected).when(quotaCreditsDaoMock).findCredits(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any());
        List<QuotaCreditsVO> result = quotaResponseBuilderSpy.getCreditsForQuotaCreditsList(cmd);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getAccountIdByAccountNameTestAccountNameIsNullReturnsNull() {
        Assert.assertNull(quotaResponseBuilderSpy.getAccountIdByAccountName(null, 1l, accountMock));
    }

    @Test
    public void getAccountIdByAccountNameTestDomainIdIsNullReturnsNull() {
        Assert.assertNull(quotaResponseBuilderSpy.getAccountIdByAccountName("test", null, accountMock));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getAccountIdByAccountNameTestAccountIsNullThrowsInvalidParameterValueException() {
        Mockito.doNothing().when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));
        Mockito.doReturn(null).when(accountDaoMock).findAccountIncludingRemoved(Mockito.anyString(), Mockito.anyLong());

        quotaResponseBuilderSpy.getAccountIdByAccountName("test", 1l, accountMock);
    }

    @Test
    public void getAccountIdByAccountNameTestAccountIsNotNullReturnsAccountId() {
        Long expected = 61l;

        Mockito.doNothing().when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));
        Mockito.doReturn(accountMock).when(accountDaoMock).findAccountIncludingRemoved(Mockito.anyString(), Mockito.anyLong());
        Mockito.doReturn(expected).when(accountMock).getAccountId();

        Long result = quotaResponseBuilderSpy.getAccountIdByAccountName("test", 1l, accountMock);

        Assert.assertEquals(expected, result);
    }

    public void getQuotaSummaryResponseWithListAllTestAccountNameIsNotNullAndDomainIdIsNullGetsDomainIdFromCaller() {
        Long expectedDomainId = 78l;

        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setAccountName("test");
        cmd.setDomainId(null);

        Mockito.doReturn(null).when(quotaResponseBuilderSpy).getAccountIdByAccountName(Mockito.anyString(), Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(expectedDomainId).when(accountMock).getDomainId();
        Mockito.doReturn(null).when(quotaResponseBuilderSpy).getDomainPathByDomainIdForDomainAdmin(Mockito.any());
        Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderSpy.getQuotaSummaryResponseWithListAll(cmd, accountMock);

        Assert.assertEquals(quotaSummaryResponseMock1, result);
        Mockito.verify(quotaResponseBuilderSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.eq(expectedDomainId), Mockito.any(), Mockito.any());
    }

    public void getQuotaSummaryResponseWithListAllTestAccountNameAndDomainIdAreNullPassDomainIdAsNull() {
        Long expectedDomainId = null;

        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setAccountName(null);
        cmd.setDomainId(null);

        Mockito.doReturn(null).when(quotaResponseBuilderSpy).getAccountIdByAccountName(Mockito.anyString(), Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(null).when(quotaResponseBuilderSpy).getDomainPathByDomainIdForDomainAdmin(Mockito.any());
        Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderSpy.getQuotaSummaryResponseWithListAll(cmd, accountMock);

        Assert.assertEquals(quotaSummaryResponseMock1, result);
        Mockito.verify(quotaResponseBuilderSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.eq(expectedDomainId), Mockito.any(), Mockito.any());
    }

    public void getQuotaSummaryResponseWithListAllTestAccountNameIsNullAndDomainIdIsNotNullPassDomainId() {
        Long expectedDomainId = 26l;

        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setAccountName(null);
        cmd.setDomainId("test");

        Mockito.doReturn(domainVoMock).when(domainDaoMock).findByUuidIncludingRemoved(Mockito.anyString());
        Mockito.doReturn(expectedDomainId).when(domainVoMock).getId();

        Mockito.doReturn(null).when(quotaResponseBuilderSpy).getAccountIdByAccountName(Mockito.anyString(), Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(null).when(quotaResponseBuilderSpy).getDomainPathByDomainIdForDomainAdmin(Mockito.any());
        Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderSpy.getQuotaSummaryResponseWithListAll(cmd, accountMock);

        Assert.assertEquals(quotaSummaryResponseMock1, result);
        Mockito.verify(quotaResponseBuilderSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.eq(expectedDomainId), Mockito.any(), Mockito.any());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getQuotaSummaryResponseWithListAllTestAccountNameIsNullAndDomainIdIsNotNullButDomainDoesNotExistThrowInvalidParameterValueException() {
        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setAccountName(null);
        cmd.setDomainId("test");

        Mockito.doReturn(null).when(domainDaoMock).findByUuidIncludingRemoved(Mockito.anyString());
        quotaResponseBuilderSpy.getQuotaSummaryResponseWithListAll(cmd, accountMock);
    }

    @Test
    public void getQuotaSummaryResponseWithListAllTestAccountNameAndDomainIdAreNotNullPassDomainId() {
        Long expectedDomainId = 9837l;

        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setAccountName("test");
        cmd.setDomainId("test");

        Mockito.doReturn(domainVoMock).when(domainDaoMock).findByUuidIncludingRemoved(Mockito.anyString());
        Mockito.doReturn(expectedDomainId).when(domainVoMock).getId();

        Mockito.doReturn(null).when(quotaResponseBuilderSpy).getAccountIdByAccountName(Mockito.anyString(), Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(null).when(quotaResponseBuilderSpy).getDomainPathByDomainIdForDomainAdmin(Mockito.any());
        Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderSpy.getQuotaSummaryResponseWithListAll(cmd, accountMock);

        Assert.assertEquals(quotaSummaryResponseMock1, result);
        Mockito.verify(quotaResponseBuilderSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.eq(expectedDomainId), Mockito.any(), Mockito.any());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getCreditsForQuotaCreditsListTestFindCreditsReturnsEmptyThrowsInvalidParameterValueException() {
        QuotaCreditsListCmd cmd = getQuotaCreditsListCmdForTests();

        Mockito.doReturn(new ArrayList<>()).when(quotaCreditsDaoMock).findCredits(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any());
        quotaResponseBuilderSpy.getCreditsForQuotaCreditsList(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getCreditsForQuotaCreditsListTestFindCreditsReturnsNullThrowsInvalidParameterValueException() {
        QuotaCreditsListCmd cmd = getQuotaCreditsListCmdForTests();

        Mockito.doReturn(null).when(quotaCreditsDaoMock).findCredits(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any());
        quotaResponseBuilderSpy.getCreditsForQuotaCreditsList(cmd);
    }

    protected QuotaCreditsListCmd getQuotaCreditsListCmdForTests() {
        QuotaCreditsListCmd cmd = new QuotaCreditsListCmd();
        cmd.setAccountId(1l);
        cmd.setDomainId(2l);
        return cmd;
    }
}
