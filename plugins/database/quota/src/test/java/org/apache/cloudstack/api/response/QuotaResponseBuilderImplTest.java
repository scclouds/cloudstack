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

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.serializer.GsonHelper;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.QuotaBalanceCmd;
import org.apache.cloudstack.api.command.QuotaConfigureEmailCmd;
import org.apache.cloudstack.api.command.QuotaCreditsListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateUpdateCmd;
import org.apache.cloudstack.api.command.QuotaStatementCmd;
import org.apache.cloudstack.api.command.QuotaSummaryCmd;
import org.apache.cloudstack.api.command.QuotaValidateActivationRuleCmd;
import org.apache.cloudstack.api.command.QuotaTariffStatementCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.discovery.ApiDiscoveryService;
import org.apache.cloudstack.jsinterpreter.JsInterpreterHelper;
import org.apache.cloudstack.quota.QuotaManager;
import org.apache.cloudstack.quota.QuotaService;
import org.apache.cloudstack.quota.activationrule.presetvariables.GenericPresetVariable;
import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariableDefinition;
import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariables;
import org.apache.cloudstack.quota.activationrule.presetvariables.Value;
import org.apache.cloudstack.quota.constant.ProcessingPeriod;
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
import org.apache.cloudstack.quota.vo.ResourcesToQuoteVO;
import org.apache.cloudstack.quota.vo.QuotaUsageDetailVO;
import org.apache.cloudstack.quota.vo.QuotaUsageJoinVO;
import org.apache.cloudstack.utils.jsinterpreter.JsInterpreter;
import org.apache.cloudstack.quota.vo.QuotaUsageResourceVO;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Stubber;
import org.mockito.verification.VerificationMode;
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
@PrepareForTest({QuotaResponseBuilderImpl.class, CallContext.class, GsonHelper.class, Gson.class})
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
    QuotaManager quotaManagerMock;

    @Mock
    Pair<List<QuotaSummaryResponse>, Integer> quotaSummaryResponseMock1, quotaSummaryResponseMock2;

    @Mock
    ApiDiscoveryService apiDiscoveryServiceMock;

    @Mock
    User userMock;

    List<QuotaTariffVO> listQuotaTariffs = List.of(new QuotaTariffVO(), new QuotaTariffVO());

    @Mock
    LinkedList<ResourcesToQuoteVO> linkedListResourcesToQuoteVoMock;

    @Mock
    QuotaStatementCmd QuotaStatementCmdMock;

    LinkedList<ResourcesToQuoteVO> linkedListResourcesToQuoteVo = new LinkedList<>(Arrays.asList(new ResourcesToQuoteVO(), new ResourcesToQuoteVO(), new ResourcesToQuoteVO()));

    @Mock
    QuotaValidateActivationRuleCmd quotaValidateActivationRuleCmdMock = Mockito.mock(QuotaValidateActivationRuleCmd.class);

    @Mock
    JsInterpreterHelper jsInterpreterHelperMock = Mockito.mock(JsInterpreterHelper.class);

    AccountVO accountVo = new AccountVO();
    DomainVO domainVo = new DomainVO();

    private QuotaTariffVO makeTariffTestData() {
        QuotaTariffVO tariffVO = new QuotaTariffVO();
        tariffVO.setUsageType(QuotaTypes.IP_ADDRESS);
        tariffVO.setUsageName("ip address");
        tariffVO.setUsageUnit("IP-Month");
        tariffVO.setCurrencyValue(BigDecimal.valueOf(100.19));
        tariffVO.setEffectiveOn(new Date());
        tariffVO.setUsageDiscriminator("");
        tariffVO.setProcessingPeriod(ProcessingPeriod.BY_ENTRY);
        return tariffVO;
    }

    @Test
    public void testQuotaResponse() {
        QuotaTariffVO tariffVO = makeTariffTestData();
        QuotaTariffResponse response = quotaResponseBuilderSpy.createQuotaTariffResponse(tariffVO, true);
        assertTrue(tariffVO.getUsageType() == response.getUsageType());
        assertTrue(tariffVO.getCurrencyValue().equals(response.getTariffValue()));
    }

    @Test
    public void createQuotaTariffResponseTestIfReturnsActivationRuleWithPermission() {
        QuotaTariffVO tariffVO = makeTariffTestData();
        tariffVO.setActivationRule("a = 10;");
        QuotaTariffResponse response = quotaResponseBuilderSpy.createQuotaTariffResponse(tariffVO, true);
        assertEquals("a = 10;", response.getActivationRule());
    }

    @Test
    public void createQuotaTariffResponseTestIfReturnsActivationRuleWithoutPermission() {
        QuotaTariffVO tariffVO = makeTariffTestData();
        tariffVO.setActivationRule("a = 10;");
        QuotaTariffResponse response = quotaResponseBuilderSpy.createQuotaTariffResponse(tariffVO, false);
        assertNull(response.getActivationRule());
    }

    @Test
    public void testAddQuotaCredits() {
        final long accountId = 2L;
        final long domainId = 1L;
        final double amount = 11.0;
        final long updatedBy = 2L;
        final Date postingDate = new Date();

        QuotaCreditsVO credit = new QuotaCreditsVO();
        credit.setCredit(new BigDecimal(amount));

        Mockito.when(quotaCreditsDaoMock.saveCredits(Mockito.any(QuotaCreditsVO.class))).thenReturn(credit);
        Mockito.when(quotaBalanceDaoMock.getLastQuotaBalance(Mockito.anyLong(), Mockito.anyLong())).thenReturn(new BigDecimal(111));

        AccountVO account = new AccountVO();
        account.setState(Account.State.LOCKED);
        Mockito.when(accountDaoMock.findById(Mockito.anyLong())).thenReturn(account);

        QuotaCreditsResponse resp = quotaResponseBuilderSpy.addQuotaCredits(accountId, domainId, amount, updatedBy, true, postingDate);
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

        quotaResponseBuilderSpy.validateEndDateOnCreatingNewQuotaTariff(quotaTariffVoMock, startDate, endDate);
    }

    @Test
    public void validateEndDateOnCreatingNewQuotaTariffTestSetValidEndDate() throws Exception {
        Date startDate = DateUtils.addDays(date, -100);
        Date endDate = DateUtils.addMilliseconds(date, 1);

        PowerMockito.whenNew(Date.class).withNoArguments().thenReturn(date);
        quotaResponseBuilderSpy.validateEndDateOnCreatingNewQuotaTariff(quotaTariffVoMock, startDate, endDate);
        Mockito.verify(quotaTariffVoMock).setEndDate(Mockito.any(Date.class));
    }

    @Test
    public void validatePositionOnCreatingNewQuotaTariffTestNullValueDoNothing() {
        quotaResponseBuilderSpy.validatePositionOnCreatingNewQuotaTariff(quotaTariffVoMock, null);
        Mockito.verify(quotaTariffVoMock, Mockito.never()).setPosition(Mockito.any());
    }

    @Test
    public void validatePositionOnCreatingNewQuotaTariffTestAnyValueIsSet() {
        Integer position = 1;
        quotaResponseBuilderSpy.validatePositionOnCreatingNewQuotaTariff(quotaTariffVoMock, position);
        Mockito.verify(quotaTariffVoMock).setPosition(position);
    }

    @Test
    @PrepareForTest(QuotaResponseBuilderImpl.class)
    public void getNewQuotaTariffObjectTestCreateFromCurrentQuotaTariff() throws Exception {
        PowerMockito.whenNew(QuotaTariffVO.class).withArguments(Mockito.any(QuotaTariffVO.class)).thenReturn(quotaTariffVoMock);

        quotaResponseBuilderSpy.getNewQuotaTariffObject(quotaTariffVoMock, "", 0, quotaTariffVoMock.getProcessingPeriod(), quotaTariffVoMock.getExecuteOn());
        PowerMockito.verifyNew(QuotaTariffVO.class).withArguments(Mockito.any(QuotaTariffVO.class));
    }

    @Test (expected = InvalidParameterValueException.class)
    public void getNewQuotaTariffObjectTestSetInvalidUsageTypeThrowsInvalidParameterValueException() throws InvalidParameterValueException {
        quotaResponseBuilderSpy.getNewQuotaTariffObject(null, "test", 0, ProcessingPeriod.BY_ENTRY, null);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void getNewQuotaTariffObjectTestSetInvalidExecuteOnThrowsInvalidParameterValueException() throws InvalidParameterValueException {
        quotaResponseBuilderSpy.getNewQuotaTariffObject(null, "test", 0, ProcessingPeriod.MONTHLY, 0);
    }
    @Test
    public void getNewQuotaTariffObjectTestReturnValidObject() throws InvalidParameterValueException {
        String name = "test";
        int usageType = 1;
        QuotaTariffVO result = quotaResponseBuilderSpy.getNewQuotaTariffObject(null, name, usageType, ProcessingPeriod.BY_ENTRY, null);

        Assert.assertEquals(name, result.getName());
        Assert.assertEquals(usageType, result.getUsageType());
    }

    @Test
    public void getNewQuotaTariffObjectTestReturnValidObjectWithMonthlyPeriod() throws InvalidParameterValueException {
        String name = "test";
        int usageType = 1;
        QuotaTariffVO result = quotaResponseBuilderSpy.getNewQuotaTariffObject(null, name, usageType, ProcessingPeriod.MONTHLY, 10);

        Assert.assertEquals(name, result.getName());
        Assert.assertEquals(usageType, result.getUsageType());
    }

    @Test
    public void persistNewQuotaTariffTestPersistNewQuotaTariff() {
        Mockito.doReturn(quotaTariffVoMock).when(quotaResponseBuilderSpy).getNewQuotaTariffObject(Mockito.any(QuotaTariffVO.class), Mockito.anyString(), Mockito.anyInt(), Mockito.any(ProcessingPeriod.class), Mockito.nullable(Integer.class));
        Mockito.doNothing().when(quotaResponseBuilderSpy).validateEndDateOnCreatingNewQuotaTariff(Mockito.any(QuotaTariffVO.class), Mockito.any(Date.class), Mockito.any(Date.class));
        Mockito.doNothing().when(quotaResponseBuilderSpy).validateValueOnCreatingNewQuotaTariff(Mockito.any(QuotaTariffVO.class), Mockito.anyDouble());
        Mockito.doNothing().when(quotaResponseBuilderSpy).validateStringsOnCreatingNewQuotaTariff(Mockito.any(Consumer.class), Mockito.anyString());
        Mockito.doNothing().when(quotaResponseBuilderSpy).validatePositionOnCreatingNewQuotaTariff(Mockito.any(QuotaTariffVO.class), Mockito.anyInt());
        Mockito.doReturn(quotaTariffVoMock).when(quotaTariffDaoMock).addQuotaTariff(Mockito.any(QuotaTariffVO.class));

        quotaResponseBuilderSpy.persistNewQuotaTariff(quotaTariffVoMock, "", 1, date, 1l, date, 1.0, "", "", 2, ProcessingPeriod.BY_ENTRY, null);

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
        expected.setPostingDate(new Date(1242421545757532L));
        expected.setObjectName("credit");

        Mockito.doReturn(accountVoMock).when(quotaResponseBuilderSpy).getAccountById(Mockito.any(), Mockito.any());
        Mockito.doReturn(expected.getAccountCreditorId()).when(accountVoMock).getUuid();
        Mockito.doReturn(expected.getAccountCreditorName()).when(accountVoMock).getAccountName();
        Mockito.doReturn(expected.getCredit()).when(quotaCreditsVoMock).getCredit();
        Mockito.doReturn(expected.getCreditedOn()).when(quotaCreditsVoMock).getUpdatedOn();
        Mockito.doReturn(expected.getPostingDate()).when(quotaCreditsVoMock).getPostingDate();

        QuotaCreditsResponse result = quotaResponseBuilderSpy.getQuotaCreditsResponse(mapAccountMock, quotaCreditsVoMock);

        Assert.assertEquals(expected.getAccountCreditorId(), result.getAccountCreditorId());
        Assert.assertEquals(expected.getAccountCreditorName(), result.getAccountCreditorName());
        Assert.assertEquals(expected.getCredit(), result.getCredit());
        Assert.assertEquals(expected.getCreditedOn(), result.getCreditedOn());
        Assert.assertEquals(expected.getCurrency(), result.getCurrency());
        Assert.assertEquals(expected.getPostingDate(), result.getPostingDate());
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

    @Test
    public void checkIfUserHasPermissionToSeeActivationRulesTestWithPermissionToCreateTariff() {
        ApiDiscoveryResponse response = new ApiDiscoveryResponse();
        response.setName("quotaTariffCreate");
        List<ApiDiscoveryResponse> cmdList = new ArrayList<>();
        cmdList.add(response);

        ListResponse<ApiDiscoveryResponse> responseList =  new ListResponse();
        responseList.setResponses(cmdList);

        Mockito.doReturn(responseList).when(apiDiscoveryServiceMock).listApis(userMock, null);

        assertTrue(quotaResponseBuilderSpy.isUserAllowedToSeeActivationRules(userMock));
    }

    @Test
    public void checkIfUserHasPermissionToSeeActivationRulesTestWithPermissionToUpdateTariff() {
        ApiDiscoveryResponse response = new ApiDiscoveryResponse();
        response.setName("quotaTariffUpdate");

        List<ApiDiscoveryResponse> cmdList = new ArrayList<>();
        cmdList.add(response);

        ListResponse<ApiDiscoveryResponse> responseList =  new ListResponse();
        responseList.setResponses(cmdList);

        Mockito.doReturn(responseList).when(apiDiscoveryServiceMock).listApis(userMock, null);

        assertTrue(quotaResponseBuilderSpy.isUserAllowedToSeeActivationRules(userMock));
    }

    @Test
    public void checkIfUserHasPermissionToSeeActivationRulesTestWithNoPermission() {
        ApiDiscoveryResponse response = new ApiDiscoveryResponse();
        response.setName("testCmd");

        List<ApiDiscoveryResponse> cmdList = new ArrayList<>();
        cmdList.add(response);

        ListResponse<ApiDiscoveryResponse> responseList =  new ListResponse();
        responseList.setResponses(cmdList);

        Mockito.doReturn(responseList).when(apiDiscoveryServiceMock).listApis(userMock, null);

        assertFalse(quotaResponseBuilderSpy.isUserAllowedToSeeActivationRules(userMock));
    }
    @Test (expected = InvalidParameterValueException.class)
    public void createQuotaBalanceResponseTestNullQuotaBalancesThrowsInvalidParameterValueException() {
        Mockito.doReturn(null).when(quotaServiceMock).listQuotaBalancesForAccount(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        quotaResponseBuilderSpy.createQuotaBalanceResponse(new QuotaBalanceCmd());
    }

    @Test (expected = InvalidParameterValueException.class)
    public void createQuotaBalanceResponseTestEmptyQuotaBalancesThrowsInvalidParameterValueException() {
        Mockito.doReturn(new ArrayList<>()).when(quotaServiceMock).listQuotaBalancesForAccount(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        quotaResponseBuilderSpy.createQuotaBalanceResponse(new QuotaBalanceCmd());
    }

    private List<QuotaBalanceVO> getQuotaBalancesForTest() {
        List<QuotaBalanceVO> balances = new ArrayList<>();

        QuotaBalanceVO balance = new QuotaBalanceVO();
        balance.setUpdatedOn(new Date());
        balance.setCreditBalance(new BigDecimal(-10.42));
        balances.add(balance);

        balance = new QuotaBalanceVO();
        balance.setUpdatedOn(new Date());
        balance.setCreditBalance(new BigDecimal(-18.94));
        balances.add(balance);

        balance = new QuotaBalanceVO();
        balance.setUpdatedOn(new Date());
        balance.setCreditBalance(new BigDecimal(-29.37));
        balances.add(balance);

        return balances;
    }

    @Test
    public void createQuotaBalancesResponseTestCreateResponse() {
        List<QuotaBalanceVO> balances = getQuotaBalancesForTest();

        QuotaBalanceResponse expected = new QuotaBalanceResponse();
        expected.setObjectName("balance");
        expected.setCurrency("$");

        Mockito.doReturn(balances).when(quotaServiceMock).listQuotaBalancesForAccount(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        QuotaBalanceResponse result = quotaResponseBuilderSpy.createQuotaBalanceResponse(new QuotaBalanceCmd());

        Assert.assertEquals(expected.getCurrency(), result.getCurrency());

        for (int i = 0; i < balances.size(); i++) {
            Assert.assertEquals(balances.get(i).getUpdatedOn(), result.getBalances().get(i).getDate());
            Assert.assertEquals(balances.get(i).getCreditBalance(), result.getBalances().get(i).getBalance());
        }
    }

    @Test
    public void quoteResourceTestReturnZeroWhenTariffsIsNull() {
        ResourcesToQuoteVO resourcesToQuoteVo = new ResourcesToQuoteVO();
        resourcesToQuoteVo.setVolumeToQuote(1);

        QuotaTypes.listQuotaTypes().values().forEach(type -> {
            resourcesToQuoteVo.setUsageType(type.getQuotaName());
            ResourcesQuotingResultResponse result;
            try {
                result = quotaResponseBuilderSpy.quoteResource(new HashMap<>(), resourcesToQuoteVo, null, new Date());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            Assert.assertEquals(0, result.getQuote().doubleValue(), 0);
        });
    }

    @Test
    public void quoteResourceTestReturnZeroWhenVolumeToQuoteIsZero() {
        ResourcesToQuoteVO resourcesToQuoteVo = new ResourcesToQuoteVO();
        resourcesToQuoteVo.setVolumeToQuote(0);

        Map<Integer, List<QuotaTariffVO>> mapTariffs = new HashMap<>();

        QuotaTypes.listQuotaTypes().values().forEach(type -> {
            resourcesToQuoteVo.setUsageType(type.getQuotaName());
            ResourcesQuotingResultResponse result;
            mapTariffs.put(type.getQuotaType(), listQuotaTariffs);

            try {
                result = quotaResponseBuilderSpy.quoteResource(mapTariffs, resourcesToQuoteVo, null, new Date());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            Assert.assertEquals(0, result.getQuote().doubleValue(), 0);
        });
    }

    @Test
    public void quoteResourceTestReturnValueWhenVolumeToQuoteIsNotZeroAndTariffsIsNotNull() throws IllegalAccessException {
        double expected = 42;
        ResourcesToQuoteVO resourcesToQuoteVo = new ResourcesToQuoteVO();
        resourcesToQuoteVo.setVolumeToQuote(1);

        Map<Integer, List<QuotaTariffVO>> mapTariffs = new HashMap<>();

        Mockito.doReturn(new BigDecimal(expected)).when(quotaManagerMock).getResourceRating(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        QuotaTypes.listQuotaTypes().values().forEach(type -> {
            resourcesToQuoteVo.setUsageType(type.getQuotaName());
            ResourcesQuotingResultResponse result;
            mapTariffs.put(type.getQuotaType(), listQuotaTariffs);

            try {
                result = quotaResponseBuilderSpy.quoteResource(mapTariffs, resourcesToQuoteVo, null, new Date());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            Assert.assertEquals(expected, result.getQuote().doubleValue(), 0);
        });
    }

    @Test(expected = InvalidParameterValueException.class)
    public void quoteResourcesTestThrowJsonSyntaxExceptionWhenPassingAnInvalidJsonAsParameter() {
        Gson gsonMock = PowerMockito.mock(Gson.class);

        PowerMockito.mockStatic(GsonHelper.class);
        PowerMockito.when(GsonHelper.getGson()).thenReturn(gsonMock);

        Mockito.doThrow(JsonSyntaxException.class).when(gsonMock).fromJson(Mockito.anyString(), Mockito.any(Type.class));

        quotaResponseBuilderSpy.quoteResources("");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void quoteResourcesTestThrowInvalidParameterValueExceptionWhenResourcesToQuoteIsNull() {
        Gson gsonMock = PowerMockito.mock(Gson.class);

        PowerMockito.mockStatic(GsonHelper.class);
        PowerMockito.when(GsonHelper.getGson()).thenReturn(gsonMock);

        Mockito.doReturn(null).when(gsonMock).fromJson(Mockito.anyString(), Mockito.any(Type.class));

        quotaResponseBuilderSpy.quoteResources("");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void quoteResourcesTestThrowInvalidParameterValueExceptionWhenResourcesToQuoteIsEmpty() {
        Gson gsonMock = PowerMockito.mock(Gson.class);

        PowerMockito.mockStatic(GsonHelper.class);
        PowerMockito.when(GsonHelper.getGson()).thenReturn(gsonMock);

        Mockito.doReturn(null).when(gsonMock).fromJson(Mockito.anyString(), Mockito.any(Type.class));

        quotaResponseBuilderSpy.quoteResources("");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void quoteResourcesTestThrowInvalidParameterValueExceptionWhenPassingAnInvalidArgumentInTheJson() {
        Gson gsonMock = PowerMockito.mock(Gson.class);

        PowerMockito.mockStatic(GsonHelper.class);
        PowerMockito.when(GsonHelper.getGson()).thenReturn(gsonMock);

        Mockito.doReturn(linkedListResourcesToQuoteVo).when(gsonMock).fromJson(Mockito.anyString(), Mockito.any(Type.class));
        Mockito.doThrow(InvalidParameterValueException.class).when(quotaResponseBuilderSpy).validateResourcesToQuoteFieldsAndReturnUsageTypes(Mockito.any());

        quotaResponseBuilderSpy.quoteResources("");
    }

    @Test(expected = PermissionDeniedException.class)
    public void quoteResourcesTestThrowPermissionDeniedExceptionWhenCallerDoesNotHaveAccessToAccountsOrDomainsPassedAsParameter() {
        Gson gsonMock = PowerMockito.mock(Gson.class);

        PowerMockito.mockStatic(GsonHelper.class);
        PowerMockito.when(GsonHelper.getGson()).thenReturn(gsonMock);

        Set<Integer> setIntegerMock = Mockito.mock(Set.class);

        Mockito.doReturn(linkedListResourcesToQuoteVo).when(gsonMock).fromJson(Mockito.anyString(), Mockito.any(Type.class));
        Mockito.doReturn(setIntegerMock).when(quotaResponseBuilderSpy).validateResourcesToQuoteFieldsAndReturnUsageTypes(Mockito.any());
        Mockito.doThrow(PermissionDeniedException.class).when(quotaResponseBuilderSpy).validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadata(Mockito.any());

        quotaResponseBuilderSpy.quoteResources("");
    }

    @Test
    public void quoteResourcesTestReturnListOfResourcesQuotingResultResponse() {
        Gson gsonMock = PowerMockito.mock(Gson.class);

        PowerMockito.mockStatic(GsonHelper.class);
        PowerMockito.when(GsonHelper.getGson()).thenReturn(gsonMock);

        Set<Integer> setIntegerMock = Mockito.mock(Set.class);
        List<ResourcesQuotingResultResponse> expected = new ArrayList<>();

        Mockito.doReturn(linkedListResourcesToQuoteVoMock).when(gsonMock).fromJson(Mockito.anyString(), Mockito.any(Type.class));
        Mockito.doReturn(setIntegerMock).when(quotaResponseBuilderSpy).validateResourcesToQuoteFieldsAndReturnUsageTypes(Mockito.any());
        Mockito.doNothing().when(quotaResponseBuilderSpy).validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadata(Mockito.any());
        Mockito.doReturn(expected).when(quotaResponseBuilderSpy).quoteResources(Mockito.any(), Mockito.any());

        List<ResourcesQuotingResultResponse> result = quotaResponseBuilderSpy.quoteResources("");

        Assert.assertEquals(expected, result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateResourcesToQuoteFieldsAndReturnUsageTypesTestThrowInvalidParameterValueExceptionWhenUsageTypeIsInvalid() {
        Mockito.doThrow(InvalidParameterValueException.class).when(quotaResponseBuilderSpy).validateResourceToQuoteUsageTypeAndReturnsItsId(Mockito.anyInt(), Mockito.any());
        quotaResponseBuilderSpy.validateResourcesToQuoteFieldsAndReturnUsageTypes(List.of(new ResourcesToQuoteVO()));
    }

    @Test
    public void validateResourcesToQuoteFieldsAndReturnUsageTypesTestReturnSetOfInteger() {
        List<ResourcesToQuoteVO> resourcesToQuoteVos = new ArrayList<>();
        QuotaTypes.listQuotaTypes().values().forEach(type -> {
            ResourcesToQuoteVO resourcesToQuoteVo = new ResourcesToQuoteVO();
            resourcesToQuoteVo.setUsageType(type.getQuotaName());
            resourcesToQuoteVos.add(resourcesToQuoteVo);
        });

        Set<Integer> expected = QuotaTypes.listQuotaTypes().values().stream().map(QuotaTypes::getQuotaType).collect(Collectors.toSet());
        Stubber stubber = null;

        for (Integer type : expected) {
            if (stubber == null) {
                stubber = Mockito.doReturn(type);
                continue;
            }

            stubber = stubber.doReturn(type);
        }

        stubber.when(quotaResponseBuilderSpy).validateResourceToQuoteUsageTypeAndReturnsItsId(Mockito.anyInt(), Mockito.any());
        Mockito.doNothing().when(quotaResponseBuilderSpy).addIdToResourceToQuoteIfNotSet(Mockito.anyInt(), Mockito.any());

        Set<Integer> result = quotaResponseBuilderSpy.validateResourcesToQuoteFieldsAndReturnUsageTypes(resourcesToQuoteVos);
        Assert.assertArrayEquals(expected.toArray(), result.toArray());
    }

    @Test
    public void addIdToResourceToQuoteIfNotSetTestIdIsSomethingThenDoNothing() {
        ResourcesToQuoteVO resourcesToQuoteVo = new ResourcesToQuoteVO();
        String expected = "test";
        resourcesToQuoteVo.setId(expected);

        quotaResponseBuilderSpy.addIdToResourceToQuoteIfNotSet(1, resourcesToQuoteVo);

        Assert.assertEquals(expected, resourcesToQuoteVo.getId());
    }

    @Test
    public void addIdToResourceToQuoteIfNotSetTestIdIsEmptyThenSetAsIndex() {
        ResourcesToQuoteVO resourcesToQuoteVo = new ResourcesToQuoteVO();
        String expected = "2";
        resourcesToQuoteVo.setId("");

        quotaResponseBuilderSpy.addIdToResourceToQuoteIfNotSet(Integer.parseInt(expected), resourcesToQuoteVo);

        Assert.assertEquals(expected, resourcesToQuoteVo.getId());
    }

    @Test
    public void addIdToResourceToQuoteIfNotSetTestIdIsWhitespaceThenSetAsIndex() {
        ResourcesToQuoteVO resourcesToQuoteVo = new ResourcesToQuoteVO();
        String expected = "3";
        resourcesToQuoteVo.setId("       ");

        quotaResponseBuilderSpy.addIdToResourceToQuoteIfNotSet(Integer.parseInt(expected), resourcesToQuoteVo);

        Assert.assertEquals(expected, resourcesToQuoteVo.getId());
    }

    @Test
    public void addIdToResourceToQuoteIfNotSetTestIdIsNullThenSetAsIndex() {
        ResourcesToQuoteVO resourcesToQuoteVo = new ResourcesToQuoteVO();
        String expected = "4";
        resourcesToQuoteVo.setId(null);

        quotaResponseBuilderSpy.addIdToResourceToQuoteIfNotSet(Integer.parseInt(expected), resourcesToQuoteVo);

        Assert.assertEquals(expected, resourcesToQuoteVo.getId());
    }

    @Test
    public void getPresetVariableIdIfItIsNotNullTestReturnTheIdWhenThePresetVariableIsNotNull() {
        String expected = "test";
        GenericPresetVariable gpv = new GenericPresetVariable();
        gpv.setId(expected);

        String result = quotaResponseBuilderSpy.getPresetVariableIdIfItIsNotNull(gpv);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getPresetVariableIdIfItIsNotNullTestReturnTheNullWhenThePresetVariableIsnull() {
        String result = quotaResponseBuilderSpy.getPresetVariableIdIfItIsNotNull(null);
        Assert.assertNull(result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateResourceToQuoteUsageTypeAndReturnsItsIdTestThrowInvalidParameterValueExceptionWhenUsageTypeIsNull() {
        quotaResponseBuilderSpy.validateResourceToQuoteUsageTypeAndReturnsItsId(0, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateResourceToQuoteUsageTypeAndReturnsItsIdTestThrowInvalidParameterValueExceptionWhenUsageTypeIsEmpty() {
        quotaResponseBuilderSpy.validateResourceToQuoteUsageTypeAndReturnsItsId(0, "");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateResourceToQuoteUsageTypeAndReturnsItsIdTestThrowInvalidParameterValueExceptionWhenUsageTypeIsWhitespace() {
        quotaResponseBuilderSpy.validateResourceToQuoteUsageTypeAndReturnsItsId(0, "   ");
    }

    @Test(expected = CloudRuntimeException.class)
    public void validateResourceToQuoteUsageTypeAndReturnsItsIdTestThrowCloudRuntimeExceptionWhenUsageTypeIsInvalid() {
        quotaResponseBuilderSpy.validateResourceToQuoteUsageTypeAndReturnsItsId(0, "anything");
    }

    @Test
    public void validateResourceToQuoteUsageTypeAndReturnsItsIdTestAllTypesReturnItsId() {
        QuotaTypes.listQuotaTypes().forEach((key, value) -> {
            int expected = key;
            int result = quotaResponseBuilderSpy.validateResourceToQuoteUsageTypeAndReturnsItsId(0, value.getQuotaName());
            Assert.assertEquals(expected, result);
        });
    }

    @Test
    @PrepareForTest(CallContext.class)
    public void validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadataTestDoNothingWhenMetadataIsNull() {
        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(accountMock).when(callContextMock).getCallingAccount();
        quotaResponseBuilderSpy.validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadata(linkedListResourcesToQuoteVo);

        Mockito.verify(quotaResponseBuilderSpy, Mockito.never()).validateCallerAccessToAccountSetInQuotingMetadata(Mockito.any(), Mockito.any(), Mockito.anyInt());
        Mockito.verify(quotaResponseBuilderSpy, Mockito.never()).validateCallerAccessToDomainSetInQuotingMetadata(Mockito.any(), Mockito.any(), Mockito.anyInt());
    }

    @Test(expected = PermissionDeniedException.class)
    @PrepareForTest(CallContext.class)
    public void validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadataTestThrowPermissionDeniedExceptionOnAccountAccessValidation() {
        linkedListResourcesToQuoteVo.get(0).setMetadata(new PresetVariables());

        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(accountMock).when(callContextMock).getCallingAccount();
        Mockito.doThrow(PermissionDeniedException.class).when(quotaResponseBuilderSpy).validateCallerAccessToAccountSetInQuotingMetadata(Mockito.any(), Mockito.any(),
                Mockito.anyInt());
        quotaResponseBuilderSpy.validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadata(linkedListResourcesToQuoteVo);
    }

    @Test(expected = PermissionDeniedException.class)
    @PrepareForTest(CallContext.class)
    public void validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadataTestThrowPermissionDeniedExceptionOnDomainAccessValidation() {
        linkedListResourcesToQuoteVo.get(0).setMetadata(new PresetVariables());

        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(accountMock).when(callContextMock).getCallingAccount();
        Mockito.doNothing().when(quotaResponseBuilderSpy).validateCallerAccessToAccountSetInQuotingMetadata(Mockito.any(), Mockito.any(), Mockito.anyInt());
        Mockito.doThrow(PermissionDeniedException.class).when(quotaResponseBuilderSpy).validateCallerAccessToDomainSetInQuotingMetadata(Mockito.any(), Mockito.any(),
                Mockito.anyInt());
        quotaResponseBuilderSpy.validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadata(linkedListResourcesToQuoteVo);
    }

    @Test
    @PrepareForTest(CallContext.class)
    public void validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadataTestDoNothingWhenCallerHasAccessToAccountAndDomain() {
        linkedListResourcesToQuoteVo.get(0).setMetadata(new PresetVariables());
        linkedListResourcesToQuoteVo.get(2).setMetadata(new PresetVariables());

        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(accountMock).when(callContextMock).getCallingAccount();
        Mockito.doNothing().when(quotaResponseBuilderSpy).validateCallerAccessToAccountSetInQuotingMetadata(Mockito.any(), Mockito.any(), Mockito.anyInt());
        Mockito.doNothing().when(quotaResponseBuilderSpy).validateCallerAccessToDomainSetInQuotingMetadata(Mockito.any(), Mockito.any(), Mockito.anyInt());

        quotaResponseBuilderSpy.validateCallerAccessToAccountsAndDomainsPassedAsParameterInQuotingMetadata(linkedListResourcesToQuoteVo);

        VerificationMode times = Mockito.times((int) linkedListResourcesToQuoteVo.stream().filter(item -> item.getMetadata() != null).count());
        Mockito.verify(quotaResponseBuilderSpy, times).validateCallerAccessToAccountSetInQuotingMetadata(Mockito.any(), Mockito.any(), Mockito.anyInt());
        Mockito.verify(quotaResponseBuilderSpy, times).validateCallerAccessToDomainSetInQuotingMetadata(Mockito.any(), Mockito.any(), Mockito.anyInt());
    }

    @Test
    public void validateCallerAccessToAccountSetInQuotingMetadataTestDoNothingWhenAccountIdIsNull() {
        Mockito.doReturn(null).when(quotaResponseBuilderSpy).getPresetVariableIdIfItIsNotNull(Mockito.any());
        quotaResponseBuilderSpy.validateCallerAccessToAccountSetInQuotingMetadata(accountMock, new PresetVariables(), 0);
    }

    @Test
    public void validateCallerAccessToAccountSetInQuotingMetadataTestDoNothingWhenAccountDoesNotExist() {
        Mockito.doReturn("something").when(quotaResponseBuilderSpy).getPresetVariableIdIfItIsNotNull(Mockito.any());
        Mockito.doReturn(null).when(accountDaoMock).findByUuidIncludingRemoved(Mockito.anyString());

        quotaResponseBuilderSpy.validateCallerAccessToAccountSetInQuotingMetadata(accountMock, new PresetVariables(), 0);
    }

    @Test(expected = PermissionDeniedException.class)
    public void validateCallerAccessToAccountSetInQuotingMetadataTestThrowPermissionDeniedExceptionOnCheckAccess() {
        Mockito.doReturn("something").when(quotaResponseBuilderSpy).getPresetVariableIdIfItIsNotNull(Mockito.any());
        Mockito.doReturn(accountVo).when(accountDaoMock).findByUuidIncludingRemoved(Mockito.anyString());
        Mockito.doThrow(PermissionDeniedException.class).when(accountManagerMock).checkAccess(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any());

        quotaResponseBuilderSpy.validateCallerAccessToAccountSetInQuotingMetadata(accountMock, new PresetVariables(), 0);
    }

    @Test
    public void validateCallerAccessToAccountSetInQuotingMetadataTestDoNothingWhenCallerHasAccessToAccount() {
        Mockito.doReturn("something").when(quotaResponseBuilderSpy).getPresetVariableIdIfItIsNotNull(Mockito.any());
        Mockito.doReturn(accountVo).when(accountDaoMock).findByUuidIncludingRemoved(Mockito.anyString());
        Mockito.doNothing().when(accountManagerMock).checkAccess(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any());

        quotaResponseBuilderSpy.validateCallerAccessToAccountSetInQuotingMetadata(accountMock, new PresetVariables(), 0);
    }

    @Test
    public void validateCallerAccessToDomainSetInQuotingMetadataTestDoNothingWhenDomainIdIsNull() {
        Mockito.doReturn(null).when(quotaResponseBuilderSpy).getPresetVariableIdIfItIsNotNull(Mockito.any());
        quotaResponseBuilderSpy.validateCallerAccessToDomainSetInQuotingMetadata(accountMock, new PresetVariables(), 0);
    }

    @Test
    public void validateCallerAccessToDomainSetInQuotingMetadataTestDoNothingWhenDomainDoesNotExist() {
        Mockito.doReturn("something").when(quotaResponseBuilderSpy).getPresetVariableIdIfItIsNotNull(Mockito.any());
        Mockito.doReturn(null).when(domainDaoMock).findByUuidIncludingRemoved(Mockito.anyString());

        quotaResponseBuilderSpy.validateCallerAccessToDomainSetInQuotingMetadata(accountMock, new PresetVariables(), 0);
    }

    @Test(expected = PermissionDeniedException.class)
    public void validateCallerAccessToDomainSetInQuotingMetadataThrowPermissionDeniedExceptionOnCheckAccess() {
        Mockito.doReturn("something").when(quotaResponseBuilderSpy).getPresetVariableIdIfItIsNotNull(Mockito.any());
        Mockito.doReturn(domainVo).when(domainDaoMock).findByUuidIncludingRemoved(Mockito.anyString());
        Mockito.doThrow(PermissionDeniedException.class).when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));

        quotaResponseBuilderSpy.validateCallerAccessToDomainSetInQuotingMetadata(accountMock, new PresetVariables(), 0);
    }

    @Test
    public void validateCallerAccessToDomainSetInQuotingMetadataDoNothingWhenCallerHasAccessToDomain() {
        Mockito.doReturn("something").when(quotaResponseBuilderSpy).getPresetVariableIdIfItIsNotNull(Mockito.any());
        Mockito.doReturn(domainVo).when(domainDaoMock).findByUuidIncludingRemoved(Mockito.anyString());
        Mockito.doNothing().when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));

        quotaResponseBuilderSpy.validateCallerAccessToDomainSetInQuotingMetadata(accountMock, new PresetVariables(), 0);
    }

    public void createDummyRecordForEachQuotaTypeIfUsageTypeIsNotInformedTestUsageTypeDifferentFromNullDoNothing() {
        List<QuotaUsageJoinVO> listUsage = new ArrayList<>();

        quotaResponseBuilderSpy.createDummyRecordForEachQuotaTypeIfUsageTypeIsNotInformed(listUsage, 1);

        Assert.assertTrue(listUsage.isEmpty());
    }

    @Test
    public void createDummyRecordForEachQuotaTypeIfUsageTypeIsNotInformedTestUsageTypeIsNullAddDummyForAllQuotaTypes() {
        List<QuotaUsageJoinVO> listUsage = new ArrayList<>();
        listUsage.add(new QuotaUsageJoinVO());

        quotaResponseBuilderSpy.createDummyRecordForEachQuotaTypeIfUsageTypeIsNotInformed(listUsage, null);

        Assert.assertEquals(QuotaTypes.listQuotaTypes().size() + 1, listUsage.size());

        QuotaTypes.listQuotaTypes().entrySet().forEach(entry -> {
            Assert.assertTrue(listUsage.stream().anyMatch(usage -> usage.getUsageType() == entry.getKey() && usage.getQuotaUsed().equals(BigDecimal.ZERO)));
        });
    }

    private List<QuotaUsageJoinVO> getQuotaUsagesForTest() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

        List<QuotaUsageJoinVO> quotaUsages = new ArrayList<>();

        QuotaUsageJoinVO quotaUsage = new QuotaUsageJoinVO();
        quotaUsage.setAccountId(1l);
        quotaUsage.setDomainId(2l);
        quotaUsage.setUsageType(3);
        quotaUsage.setQuotaUsed(BigDecimal.valueOf(10));
        try {
            quotaUsage.setStartDate(sdf.parse("2022-01-01T00:00:00+0000"));
            quotaUsage.setEndDate(sdf.parse("2022-01-02T00:00:00+0000"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        quotaUsages.add(quotaUsage);

        quotaUsage = new QuotaUsageJoinVO();
        quotaUsage.setAccountId(4l);
        quotaUsage.setDomainId(5l);
        quotaUsage.setUsageType(3);
        quotaUsage.setQuotaUsed(null);
        try {
            quotaUsage.setStartDate(sdf.parse("2022-01-03T00:00:00+0000"));
            quotaUsage.setEndDate(sdf.parse("2022-01-04T00:00:00+0000"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        quotaUsages.add(quotaUsage);

        quotaUsage = new QuotaUsageJoinVO();
        quotaUsage.setAccountId(6l);
        quotaUsage.setDomainId(7l);
        quotaUsage.setUsageType(3);
        quotaUsage.setQuotaUsed(BigDecimal.valueOf(5));
        try {
            quotaUsage.setStartDate(sdf.parse("2022-01-05T00:00:00+0000"));
            quotaUsage.setEndDate(sdf.parse("2022-01-06T00:00:00+0000"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        quotaUsages.add(quotaUsage);

        return quotaUsages;
    }

    @Test
    public void createStatementItemTestReturnItem() {
        List<QuotaUsageJoinVO> quotaUsages = getQuotaUsagesForTest();
        QuotaStatementCmd cmd =  new QuotaStatementCmd();
        Mockito.doNothing().when(quotaResponseBuilderSpy).setStatementItemResources(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());

        QuotaStatementItemResponse result = quotaResponseBuilderSpy.createStatementItem(0, quotaUsages, cmd);

        QuotaUsageJoinVO expected = quotaUsages.get(0);
        QuotaTypes quotaTypeExpected = QuotaTypes.listQuotaTypes().get(expected.getUsageType());
        Assert.assertEquals(BigDecimal.valueOf(15), result.getQuotaUsed());
        Assert.assertEquals(quotaTypeExpected.getQuotaUnit(), result.getUsageUnit());
        Assert.assertEquals(quotaTypeExpected.getQuotaName(), result.getUsageName());
    }

    @Test
    public void createQuotaDateMapTestWithoutSegregation() {
        List<QuotaUsageJoinVO> quotaUsages = getQuotaUsagesForTest();

        QuotaStatementCmd cmd =  new QuotaStatementCmd();
        cmd.setAggregationInterval("none");

        Map<String, BigDecimal> result = quotaResponseBuilderSpy.createQuotaDateMap(quotaUsages, cmd, BigDecimal.ONE);

        assertNull(result);
    }

    @Test
    public void createQuotaDateMapTestWithDailySegregation() {
        List<QuotaUsageJoinVO> quotaUsages = getQuotaUsagesForTest();

        QuotaStatementCmd cmd =  new QuotaStatementCmd();

        cmd.setAggregationInterval("daily");

        String startDate = "2022-01-01T00:00:00+0000";
        String endDate = "2022-01-05T00:00:00+0000";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        try {
            cmd.setStartDate(sdf.parse(startDate));
            cmd.setEndDate(sdf.parse(endDate));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        Map<String, BigDecimal> result = quotaResponseBuilderSpy.createQuotaDateMap(quotaUsages, cmd, BigDecimal.ONE);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals(BigDecimal.valueOf(10), result.get(startDate));
        Assert.assertEquals(BigDecimal.valueOf(5), result.get(endDate));
    }


    @Test
    public void createQuotaDateMapTestWithHourlySegregation() {
        List<QuotaUsageJoinVO> quotaUsages = getQuotaUsagesForTest();

        QuotaStatementCmd cmd =  new QuotaStatementCmd();
        cmd.setAggregationInterval("hourly");

        String startDate = "2022-01-01T00:00:00+0000";
        String endDate = "2022-01-05T00:00:00+0000";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        try {
            cmd.setStartDate(sdf.parse(startDate));
            cmd.setEndDate(sdf.parse(endDate));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        Map<String, BigDecimal> result = quotaResponseBuilderSpy.createQuotaDateMap(quotaUsages, cmd, BigDecimal.ONE);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals(BigDecimal.valueOf(10), result.get(startDate));
        Assert.assertEquals(BigDecimal.valueOf(5), result.get(endDate));
    }

    @Test
    public void setStatementItemResourcesTestDoNotShowResourcesDoNothing() {
        QuotaStatementItemResponse item = new QuotaStatementItemResponse(1);

        QuotaStatementCmd cmd =  new QuotaStatementCmd();
        Mockito.doReturn(false).when(QuotaStatementCmdMock).isShowResources();
        quotaResponseBuilderSpy.setStatementItemResources(item, 0, getQuotaUsagesForTest(), cmd);

        Assert.assertNull(item.getResources());
    }

    @Test
    public void filterSupportedTypesTestReturnWhenQuotaTypeDoesNotMatch() throws NoSuchFieldException {
        List<Pair<String, String>> variables = new ArrayList<>();
        Class<?> clazz = Value.class;
        PresetVariableDefinition presetVariableDefinitionAnnotation = clazz.getDeclaredField("host").getAnnotation(PresetVariableDefinition.class);
        QuotaTypes quotaType = QuotaTypes.getQuotaType(QuotaTypes.NETWORK_OFFERING);
        int expectedVariablesSize = 0;

        Mockito.doCallRealMethod().when(quotaResponseBuilderSpy).filterSupportedTypes(Mockito.anyList(), Mockito.any(QuotaTypes.class), Mockito.any(PresetVariableDefinition.class),
                Mockito.any(), Mockito.anyString());
        quotaResponseBuilderSpy.filterSupportedTypes(variables, quotaType, presetVariableDefinitionAnnotation, clazz, null);

        assertEquals(expectedVariablesSize, variables.size());
    }

    @Test
    public void filterSupportedTypesTestAddPresetVariableWhenClassIsNotInstanceOfGenericPresetVariableAndComputingResource() throws NoSuchFieldException {
        List<Pair<String, String>> variables = new ArrayList<>();
        Class<?> clazz = PresetVariables.class;
        PresetVariableDefinition presetVariableDefinitionAnnotation = clazz.getDeclaredField("resourceType").getAnnotation(PresetVariableDefinition.class);
        QuotaTypes quotaType = QuotaTypes.getQuotaType(QuotaTypes.NETWORK_OFFERING);
        int expectedVariablesSize = 1;
        String expectedVariableName = "variable.name";

        Mockito.doCallRealMethod().when(quotaResponseBuilderSpy).filterSupportedTypes(Mockito.anyList(), Mockito.any(QuotaTypes.class), Mockito.any(PresetVariableDefinition.class),
                Mockito.any(), Mockito.anyString());
        quotaResponseBuilderSpy.filterSupportedTypes(variables, quotaType, presetVariableDefinitionAnnotation, clazz, expectedVariableName);

        assertEquals(expectedVariablesSize, variables.size());
        assertEquals(expectedVariableName, variables.get(0).first());
    }

    @Test
    public void filterSupportedTypesTestCallRecursiveMethodWhenIsGenericPresetVariableClassOrComputingResourceClass() throws NoSuchFieldException {
        List<Pair<String, String>> variables = new ArrayList<>();
        Class<?> clazz = Value.class;
        PresetVariableDefinition presetVariableDefinitionAnnotation = clazz.getDeclaredField("storage").getAnnotation(PresetVariableDefinition.class);
        QuotaTypes quotaType = QuotaTypes.getQuotaType(QuotaTypes.VOLUME);

        Mockito.doCallRealMethod().when(quotaResponseBuilderSpy).filterSupportedTypes(Mockito.anyList(), Mockito.any(QuotaTypes.class), Mockito.any(PresetVariableDefinition.class),
                Mockito.any(), Mockito.anyString());
        quotaResponseBuilderSpy.filterSupportedTypes(variables, quotaType, presetVariableDefinitionAnnotation, clazz, "variable.name");

        Mockito.verify(quotaResponseBuilderSpy, Mockito.atLeastOnce()).addAllPresetVariables(Mockito.any(), Mockito.any(QuotaTypes.class), Mockito.anyList(),
                Mockito.anyString());
    }

    public void createQuotaTariffStatementItemResponseTestReturnsObject() {
        Mockito.doReturn("uuid").when(quotaTariffVoMock).getUuid();
        Mockito.doReturn("name").when(quotaTariffVoMock).getName();
        Mockito.doReturn(1).when(quotaTariffVoMock).getUsageType();
        Mockito.doReturn("usagename").when(quotaTariffVoMock).getUuid();
        Mockito.doReturn("usageunit").when(quotaTariffVoMock).getUuid();
        List<QuotaUsageDetailVO> quotaUsageDetailList = new ArrayList<>();
        BigDecimal totalQuotaUsed = BigDecimal.ZERO;
        for (int i = 0; i < 10; i++) {
            QuotaUsageDetailVO quotaUsageDetail = new QuotaUsageDetailVO();
            BigDecimal quotaUsed = BigDecimal.valueOf(i);
            quotaUsageDetail.setQuotaUsed(quotaUsed);
            totalQuotaUsed = totalQuotaUsed.add(quotaUsed);
            quotaUsageDetailList.add(quotaUsageDetail);
        }

        QuotaTariffStatementItemResponse response = quotaResponseBuilderSpy.createQuotaTariffStatementItemResponse(quotaTariffVoMock, quotaUsageDetailList, new ArrayList<>(), false);

        Assert.assertEquals(quotaTariffVoMock.getUuid(), response.getTariffId());
        Assert.assertEquals(quotaTariffVoMock.getName(), response.getTariffName());
        Assert.assertEquals(quotaTariffVoMock.getUsageType(), response.getUsageType());
        Assert.assertEquals(quotaTariffVoMock.getUsageName(), response.getUsageName());
        Assert.assertEquals(quotaTariffVoMock.getUsageUnit(), response.getUsageUnit());
        Assert.assertEquals(totalQuotaUsed, response.getQuotaUsed());
    }

    @Test
    public void createQuotaTariffStatementItemResponseTestSetsResourcesWhenShowResourcesIsTrue() {
        quotaResponseBuilderSpy.createQuotaTariffStatementItemResponse(quotaTariffVoMock, new ArrayList<>(), new ArrayList<>(), true);

        Mockito.verify(quotaResponseBuilderSpy).setTariffStatementItemResources(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());
    }

    @Test
    public void createQuotaTariffStatementItemResponseTestDoesNotSetResourcesWhenShowResourcesIsFalse() {
        quotaResponseBuilderSpy.createQuotaTariffStatementItemResponse(quotaTariffVoMock, new ArrayList<>(), new ArrayList<>(), false);

        Mockito.verify(quotaResponseBuilderSpy, Mockito.never()).setTariffStatementItemResources(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any());
    }

    @Test
    public void setTariffStatementItemResourcesTestSetsExpectedValues() {
        QuotaTariffStatementItemResponse statementItem = new QuotaTariffStatementItemResponse();

        QuotaUsageResourceVO resource = new QuotaUsageResourceVO("uuid", "name", new Date());
        List<QuotaUsageDetailVO> quotaUsageDetailList = new ArrayList<>();
        QuotaUsageDetailVO detail = new QuotaUsageDetailVO();
        detail.setQuotaUsageId(1L);
        quotaUsageDetailList.add(detail);

        List<QuotaUsageJoinVO> quotaUsageRecords = new ArrayList<>();
        QuotaUsageJoinVO quotaUsage = new QuotaUsageJoinVO();
        quotaUsage.setId(1L);
        quotaUsage.setUsageType(1);
        quotaUsage.setQuotaUsed(BigDecimal.ZERO);
        quotaUsageRecords.add(quotaUsage);

        Mockito.doReturn(resource).when(quotaResponseBuilderSpy).getResourceFromIdAndType(Mockito.anyLong(), Mockito.anyInt());
        Mockito.doReturn(1L).when(quotaResponseBuilderSpy).getResourceIdByUsageType(Mockito.any(), Mockito.anyInt());

        QuotaStatementItemResourceResponse expectedDetail = new QuotaStatementItemResourceResponse();
        Mockito.doReturn(expectedDetail).when(quotaResponseBuilderSpy).createQuotaStatementDetail(Mockito.any());

        quotaResponseBuilderSpy.setTariffStatementItemResources(statementItem, 1, quotaUsageDetailList, quotaUsageRecords);

        Assert.assertEquals(statementItem.getResources().size() , 1);
        Assert.assertEquals(expectedDetail, statementItem.getResources().get(0));
    }

    @Test
    public void createQuotaStatementDetailTestReturnsDetailWithExpectedValues() {
        QuotaUsageResourceVO resource = new QuotaUsageResourceVO("uuid", "name", new Date());
        QuotaStatementItemResourceResponse detail = quotaResponseBuilderSpy.createQuotaStatementDetail(resource);
        Assert.assertEquals(detail.getResourceId(), "uuid");
        Assert.assertEquals(resource.getName(), "name");
        Assert.assertTrue(detail.isRemoved());
    }

    @Test
    public void createQuotaTariffStatementResponseTestReturnsObject() {
        QuotaTariffStatementResponse expected = new QuotaTariffStatementResponse();
        expected.setAccountId("account_uuid");
        expected.setAccountName("account_name");
        expected.setDomainId("domain_uuid");
        expected.setDomain("domain_path");
        BigDecimal totalQuotaUsed = BigDecimal.valueOf(10);
        expected.setTotalQuotaUsed(totalQuotaUsed);
        List<QuotaTariffStatementItemResponse> quotaTariffStatementItemResponseList = List.of(new QuotaTariffStatementItemResponse(),new QuotaTariffStatementItemResponse());
        expected.setQuotaUsage(quotaTariffStatementItemResponseList);
        Date startDate = new Date();
        expected.setStartDate(startDate);
        Date endDate = new Date();
        expected.setEndDate(endDate);
        expected.setObjectName(ApiConstants.TARIFF_STATEMENT);

        Mockito.doReturn(expected.getAccountId()).when(accountVoMock).getUuid();
        Mockito.doReturn(expected.getAccountName()).when(accountVoMock).getAccountName();
        QuotaTariffStatementCmd cmd = Mockito.mock(QuotaTariffStatementCmd.class);
        Mockito.doReturn(accountVoMock.getAccountId()).when(cmd).getEntityOwnerId();
        Mockito.doReturn(startDate).when(cmd).getStartDate();
        Mockito.doReturn(endDate).when(cmd).getEndDate();
        Mockito.doReturn(accountVoMock).when(accountManagerMock).getActiveAccountById(Mockito.anyLong());
        Mockito.doReturn(Account.Type.NORMAL).when(accountVoMock).getType();
        Mockito.doReturn(1L).when(accountVoMock).getDomainId();
        Mockito.doReturn(expected.getDomainId()).when(domainVoMock).getUuid();
        Mockito.doReturn(expected.getDomain()).when(domainVoMock).getName();
        Mockito.doReturn(domainVoMock).when(domainDaoMock).findByIdIncludingRemoved(Mockito.anyLong());

        QuotaTariffStatementResponse response = quotaResponseBuilderSpy.createQuotaTariffStatementResponse(cmd, quotaTariffStatementItemResponseList, totalQuotaUsed);

        Assert.assertEquals(expected.getAccountId(), response.getAccountId());
        Assert.assertEquals(expected.getAccountName(), response.getAccountName());
        Assert.assertEquals(expected.getDomainId(), response.getDomainId());
        Assert.assertEquals(expected.getDomain(), response.getDomain());
        Assert.assertEquals(expected.getTotalQuotaUsed(), response.getTotalQuotaUsed());
        Assert.assertEquals(expected.getQuotaUsage(), response.getQuotaUsage());
        Assert.assertEquals(expected.getStartDate(), response.getStartDate());
        Assert.assertEquals(expected.getEndDate(), response.getEndDate());
        Assert.assertEquals(expected.getObjectName(), response.getObjectName());
    }

    public void validateActivationRuleTestValidateActivationRuleReturnValidScriptResponse() {
        Mockito.doReturn("if (account.name == 'test') { true } else { false }").when(quotaValidateActivationRuleCmdMock).getActivationRule();
        Mockito.doReturn("NETWORK").when(quotaValidateActivationRuleCmdMock).getQuotaType();
        Mockito.doReturn(quotaValidateActivationRuleCmdMock.getActivationRule()).when(jsInterpreterHelperMock).replaceScriptVariables(Mockito.anyString(), Mockito.any());

        QuotaValidateActivationRuleResponse response = quotaResponseBuilderSpy.validateActivationRule(quotaValidateActivationRuleCmdMock);

        Assert.assertTrue(response.isValid());
    }

    @Test
    public void validateActivationRuleTestUsageTypeIncompatibleVariableReturnInvalidScriptResponse() {
        Mockito.doReturn("if (invalid.variable == 'test') { true } else { false }").when(quotaValidateActivationRuleCmdMock).getActivationRule();
        Mockito.doReturn(QuotaTypes.getQuotaType(1)).when(quotaValidateActivationRuleCmdMock).getQuotaType();
        Mockito.doReturn(quotaValidateActivationRuleCmdMock.getActivationRule()).when(jsInterpreterHelperMock).replaceScriptVariables(Mockito.anyString(), Mockito.any());
        Mockito.when(jsInterpreterHelperMock.getScriptVariables(quotaValidateActivationRuleCmdMock.getActivationRule())).thenReturn(Set.of("invalid.variable"));

        QuotaValidateActivationRuleResponse response = quotaResponseBuilderSpy.validateActivationRule(quotaValidateActivationRuleCmdMock);

        Assert.assertFalse(response.isValid());
    }


    @Test
    public void validateActivationRuleTestActivationRuleWithSyntaxErrorsReturnInvalidScriptResponse() {
        Mockito.doReturn("{ if (account.name == 'test') { true } else { false } }}").when(quotaValidateActivationRuleCmdMock).getActivationRule();
        Mockito.doReturn(QuotaTypes.getQuotaType(1)).when(quotaValidateActivationRuleCmdMock).getQuotaType();
        Mockito.doReturn(quotaValidateActivationRuleCmdMock.getActivationRule()).when(jsInterpreterHelperMock).replaceScriptVariables(Mockito.anyString(), Mockito.any());

        QuotaValidateActivationRuleResponse response = quotaResponseBuilderSpy.validateActivationRule(quotaValidateActivationRuleCmdMock);

        Assert.assertFalse(response.isValid());
    }


    @Test
    public void injectUsageTypeVariablesTestReturnInjectedVariables() {
        JsInterpreter interpreter = Mockito.mock(JsInterpreter.class);

        Map<String, String> formattedVariables = quotaResponseBuilderSpy.injectUsageTypeVariables(interpreter, List.of("account.name", "zone.name"));

        Assert.assertTrue(formattedVariables.containsValue("accountname"));
        Assert.assertTrue(formattedVariables.containsValue("zonename"));
    }
}
