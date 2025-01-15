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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.AccountManager;
import com.cloud.user.UserVO;
import com.cloud.utils.Pair;
import com.google.common.collect.Sets;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.QuotaBalanceCmd;
import org.apache.cloudstack.api.command.QuotaConfigureEmailCmd;
import org.apache.cloudstack.api.command.QuotaCreditsListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateUpdateCmd;
import org.apache.cloudstack.api.command.QuotaTariffStatementCmd;
import org.apache.cloudstack.api.command.QuotaValidateActivationRuleCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.discovery.ApiDiscoveryService;
import org.apache.cloudstack.api.command.QuotaSummaryCmd;
import org.apache.cloudstack.jsinterpreter.JsInterpreterHelper;
import org.apache.cloudstack.quota.QuotaService;
import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariableDefinition;
import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariables;
import org.apache.cloudstack.quota.activationrule.presetvariables.Value;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaCreditsDao;
import org.apache.cloudstack.quota.dao.QuotaEmailConfigurationDao;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaCreditsVO;
import org.apache.cloudstack.quota.vo.QuotaEmailConfigurationVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageDetailVO;
import org.apache.cloudstack.quota.vo.QuotaUsageJoinVO;
import org.apache.cloudstack.quota.vo.QuotaUsageResourceVO;
import org.apache.cloudstack.utils.jsinterpreter.JsInterpreter;

import org.apache.commons.lang3.time.DateUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.User;

import junit.framework.TestCase;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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
    User userMock;

    @Mock
    ApiDiscoveryService discoveryServiceMock;

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

    @Mock
    QuotaAccountDao quotaAccountDaoMock;

    @Mock
    QuotaEmailConfigurationDao quotaEmailConfigurationDaoMock;

    @InjectMocks
    @Spy
    QuotaResponseBuilderImpl quotaResponseBuilderSpy;

    Date date = new Date();

    @Mock
    Account accountMock;

    @Mock
    QuotaConfigureEmailCmd quotaConfigureEmailCmdMock;

    @Mock
    QuotaAccountVO quotaAccountVOMock;

    @Mock
    CallContext callContextMock;

    @Mock
    QuotaEmailTemplatesVO quotaEmailTemplatesVoMock;

    @Mock
    DomainVO domainVoMock;
    @Mock
    QuotaCreditsVO quotaCreditsVoMock;

    @Mock
    AccountVO accountVoMock;

    @Mock
    UserVO userVoMock;

    @Mock
    AccountManager accountManagerMock;

    @Mock
    Account callerAccountMock;

    @Mock
    User callerUserMock;

    @Before
    public void setup() {
        CallContext.register(callerUserMock, callerAccountMock);
    }

    @Mock
    Pair<List<QuotaSummaryResponse>, Integer> quotaSummaryResponseMock1, quotaSummaryResponseMock2;

    @Mock
    QuotaValidateActivationRuleCmd quotaValidateActivationRuleCmdMock = Mockito.mock(QuotaValidateActivationRuleCmd.class);

    @Mock
    JsInterpreterHelper jsInterpreterHelperMock = Mockito.mock(JsInterpreterHelper.class);

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
        QuotaTariffResponse response = quotaResponseBuilderSpy.createQuotaTariffResponse(tariffVO, true);
        assertTrue(tariffVO.getUsageType() == response.getUsageType());
        assertTrue(tariffVO.getCurrencyValue().equals(response.getTariffValue()));
    }

    @Test
    public void createQuotaTariffResponseTestIfReturnsActivationRuleWithPermission() {
        QuotaTariffVO tariff = makeTariffTestData();
        tariff.setActivationRule("x === 10");

        QuotaTariffResponse tariffResponse = quotaResponseBuilderSpy.createQuotaTariffResponse(tariff, true);
        assertEquals("x === 10", tariffResponse.getActivationRule());
    }

    @Test
    public void createQuotaTariffResponseTestIfReturnsActivationRuleWithoutPermission() {
        QuotaTariffVO tariff = makeTariffTestData();
        tariff.setActivationRule("x === 10");

        QuotaTariffResponse tariffResponse = quotaResponseBuilderSpy.createQuotaTariffResponse(tariff, false);
        assertNull(tariffResponse.getActivationRule());
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
        Mockito.doReturn(userVoMock).when(quotaResponseBuilderSpy).getCreditorForQuotaCredits(credit);

        AccountVO account = new AccountVO();
        account.setState(Account.State.LOCKED);
        Mockito.when(accountDaoMock.findById(Mockito.anyLong())).thenReturn(account);

        QuotaCreditsResponse resp = quotaResponseBuilderSpy.addQuotaCredits(accountId, amount, updatedBy, true, postingDate);
        assertEquals(0, resp.getCredit().compareTo(credit.getCredit()));
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
    public void validateEndDateOnCreatingNewQuotaTariffTestSetValidEndDate() {
        Date startDate = DateUtils.addDays(date, -100);
        Date endDate = DateUtils.addMinutes(new Date(), 1);

        quotaResponseBuilderSpy.validateEndDateOnCreatingNewQuotaTariff(quotaTariffVoMock, startDate, endDate);
        Mockito.verify(quotaTariffVoMock).setEndDate(Mockito.any(Date.class));
    }

    @Test
    public void getNewQuotaTariffObjectTestCreateFromCurrentQuotaTariff() throws Exception {
        try (MockedConstruction<QuotaTariffVO> quotaTariffVOMockedConstruction = Mockito.mockConstruction(QuotaTariffVO.class, (mock,
                                                                                                                                context) -> {
        })) {
            QuotaTariffVO result = quotaResponseBuilderSpy.getNewQuotaTariffObject(quotaTariffVoMock, "", 0);
            Assert.assertEquals(quotaTariffVOMockedConstruction.constructed().get(0), result);
        }
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
    public void persistNewQuotaTariffTestpersistNewQuotaTariff() {
        Mockito.doReturn(quotaTariffVoMock).when(quotaResponseBuilderSpy).getNewQuotaTariffObject(Mockito.any(QuotaTariffVO.class), Mockito.anyString(), Mockito.anyInt());
        Mockito.doNothing().when(quotaResponseBuilderSpy).validateEndDateOnCreatingNewQuotaTariff(Mockito.any(QuotaTariffVO.class), Mockito.any(Date.class), Mockito.any(Date.class));
        Mockito.doNothing().when(quotaResponseBuilderSpy).validateValueOnCreatingNewQuotaTariff(Mockito.any(QuotaTariffVO.class), Mockito.anyDouble());
        Mockito.doNothing().when(quotaResponseBuilderSpy).validateStringsOnCreatingNewQuotaTariff(Mockito.any(Consumer.class), Mockito.anyString());
        Mockito.doReturn(quotaTariffVoMock).when(quotaTariffDaoMock).addQuotaTariff(Mockito.any(QuotaTariffVO.class));
        Mockito.doNothing().when(quotaResponseBuilderSpy).validatePositionOnCreatingNewQuotaTariff(Mockito.any(QuotaTariffVO.class), Mockito.anyInt());


        quotaResponseBuilderSpy.persistNewQuotaTariff(quotaTariffVoMock, "", 1, date, 1l, date, 1.0, "", "", 2);

        Mockito.verify(quotaTariffDaoMock).addQuotaTariff(Mockito.any(QuotaTariffVO.class));
    }

    @Test (expected = ServerApiException.class)
    public void deleteQuotaTariffTestQuotaDoesNotExistThrowsServerApiException() {
        quotaResponseBuilderSpy.deleteQuotaTariff("");
    }

    @Test
    public void deleteQuotaTariffTestUpdateRemoved() {
        Mockito.doReturn(quotaTariffVoMock).when(quotaTariffDaoMock).findByUuid(Mockito.anyString());
        Mockito.doReturn(true).when(quotaTariffDaoMock).updateQuotaTariff(Mockito.any(QuotaTariffVO.class));

        Assert.assertTrue(quotaResponseBuilderSpy.deleteQuotaTariff(""));

        Mockito.verify(quotaTariffVoMock).setRemoved(Mockito.any(Date.class));
    }

    @Test
    public void filterSupportedTypesTestReturnWhenQuotaTypeDoesNotMatch() throws NoSuchFieldException {
        List<Pair<String, String>> variables = new ArrayList<>();
        Class<?> clazz = Value.class;
        PresetVariableDefinition presetVariableDefinitionAnnotation = clazz.getDeclaredField("host").getAnnotation(PresetVariableDefinition.class);
        QuotaTypes quotaType = QuotaTypes.getQuotaType(QuotaTypes.NETWORK_OFFERING);
        int expectedVariablesSize = 0;

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

        quotaResponseBuilderSpy.filterSupportedTypes(variables, quotaType, presetVariableDefinitionAnnotation, clazz, "variable.name");

        assertEquals(expectedVariablesSize, variables.size());
        assertEquals(expectedVariableName, variables.get(0).first());
    }

    @Test
    public void filterSupportedTypesTestCallRecursiveMethodWhenIsGenericPresetVariableClassOrComputingResourceClass() throws NoSuchFieldException {
        List<Pair<String, String>> variables = new ArrayList<>();
        Class<?> clazz = Value.class;
        PresetVariableDefinition presetVariableDefinitionAnnotation = clazz.getDeclaredField("storage").getAnnotation(PresetVariableDefinition.class);
        QuotaTypes quotaType = QuotaTypes.getQuotaType(QuotaTypes.VOLUME);

        quotaResponseBuilderSpy.filterSupportedTypes(variables, quotaType, presetVariableDefinitionAnnotation, clazz, "variable.name");

        Mockito.verify(quotaResponseBuilderSpy, Mockito.atLeastOnce()).addAllPresetVariables(Mockito.any(), Mockito.any(QuotaTypes.class), Mockito.anyList(),
                Mockito.anyString());
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

        try(MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);

            Mockito.doReturn(accountMock).when(callContextMock).getCallingAccount();

            Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

            for (Account.Type type : Account.Type.values()) {
                Mockito.doReturn(type).when(accountMock).getType();

                Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderSpy.createQuotaSummaryResponse(cmd);
                Assert.assertEquals(quotaSummaryResponseMock1, result);
            }

            Mockito.verify(quotaResponseBuilderSpy, Mockito.times(Account.Type.values().length)).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                    Mockito.any());
        };
    }

    @Test
    public void createQuotaSummaryResponseTestListAllAndAccountTypesAdminReturnsAllAndTheRestReturnsSingleRecord() {
        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setListAll(true);

        try(MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);

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
        Mockito.lenient().doNothing().when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));

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


    @Test
    public void getQuotaEmailConfigurationVoTestTemplateNameIsNull() {
        Mockito.doReturn(null).when(quotaConfigureEmailCmdMock).getTemplateName();

        QuotaEmailConfigurationVO result = quotaResponseBuilderSpy.getQuotaEmailConfigurationVo(quotaConfigureEmailCmdMock);

        Assert.assertNull(result);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void getQuotaEmailConfigurationVoTestNoTemplateFound() {
        Mockito.doReturn("name").when(quotaConfigureEmailCmdMock).getTemplateName();
        Mockito.doReturn(new ArrayList<QuotaEmailTemplatesVO>()).when(quotaEmailTemplateDaoMock).listAllQuotaEmailTemplates(Mockito.any());

        quotaResponseBuilderSpy.getQuotaEmailConfigurationVo(quotaConfigureEmailCmdMock);
    }

    @Test
    public void getQuotaEmailConfigurationVoTestNewConfiguration() {
        Mockito.doReturn("name").when(quotaConfigureEmailCmdMock).getTemplateName();
        List<QuotaEmailTemplatesVO> templatesVOArrayList = List.of(quotaEmailTemplatesVoMock);
        Mockito.doReturn(templatesVOArrayList).when(quotaEmailTemplateDaoMock).listAllQuotaEmailTemplates(Mockito.any());
        Mockito.doReturn(null).when(quotaEmailConfigurationDaoMock).findByAccountIdAndEmailTemplateId(Mockito.anyLong(), Mockito.anyLong());

        QuotaEmailConfigurationVO result = quotaResponseBuilderSpy.getQuotaEmailConfigurationVo(quotaConfigureEmailCmdMock);

        Mockito.verify(quotaEmailConfigurationDaoMock).persistQuotaEmailConfiguration(Mockito.any());
        assertEquals(0, result.getAccountId());
        assertEquals(0, result.getEmailTemplateId());
        assertFalse(result.isEnabled());
    }

    @Test
    public void getQuotaEmailConfigurationVoTestExistingConfiguration() {
        Mockito.doReturn("name").when(quotaConfigureEmailCmdMock).getTemplateName();
        List<QuotaEmailTemplatesVO> templatesVOArrayList = List.of(quotaEmailTemplatesVoMock);
        Mockito.doReturn(templatesVOArrayList).when(quotaEmailTemplateDaoMock).listAllQuotaEmailTemplates(Mockito.any());

        QuotaEmailConfigurationVO quotaEmailConfigurationVO = new QuotaEmailConfigurationVO(1, 2, true);
        Mockito.doReturn(quotaEmailConfigurationVO).when(quotaEmailConfigurationDaoMock).findByAccountIdAndEmailTemplateId(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(quotaEmailConfigurationVO).when(quotaEmailConfigurationDaoMock).updateQuotaEmailConfiguration(Mockito.any());

        QuotaEmailConfigurationVO result = quotaResponseBuilderSpy.getQuotaEmailConfigurationVo(quotaConfigureEmailCmdMock);

        Mockito.verify(quotaEmailConfigurationDaoMock).updateQuotaEmailConfiguration(Mockito.any());

        assertEquals(1, result.getAccountId());
        assertEquals(2, result.getEmailTemplateId());
        assertFalse(result.isEnabled());
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
        Mockito.lenient().doNothing().when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));
        Mockito.doReturn(null).when(accountDaoMock).findAccountIncludingRemoved(Mockito.anyString(), Mockito.anyLong());

        quotaResponseBuilderSpy.getAccountIdByAccountName("test", 1l, accountMock);
    }

    @Test
    public void getAccountIdByAccountNameTestAccountIsNotNullReturnsAccountId() {
        Long expected = 61l;

        Mockito.lenient().doNothing().when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));
        Mockito.doReturn(accountMock).when(accountDaoMock).findAccountIncludingRemoved(Mockito.anyString(), Mockito.anyLong());
        Mockito.doReturn(expected).when(accountMock).getAccountId();

        Long result = quotaResponseBuilderSpy.getAccountIdByAccountName("test", 1l, accountMock);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getQuotaSummaryResponseWithListAllTestDomainIdIsNullPassDomainIdAsNull() {
        Long expectedDomainId = null;

        QuotaSummaryCmd cmd = Mockito.mock(QuotaSummaryCmd.class);
        Mockito.doReturn(null).when(cmd).getDomainId();
        Mockito.doReturn(-1L).when(cmd).getEntityOwnerId();

        Mockito.doReturn(null).when(quotaResponseBuilderSpy).getDomainPathByDomainIdForDomainAdmin(Mockito.any());
        Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderSpy.getQuotaSummaryResponseWithListAll(cmd, accountMock);

        Assert.assertEquals(quotaSummaryResponseMock1, result);
        Mockito.verify(quotaResponseBuilderSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.eq(expectedDomainId), Mockito.any(), Mockito.any());
    }

    @Test
    public void getQuotaSummaryResponseWithListAllTestAccountNameIsNullAndDomainIdIsNotNullPassDomainId() {
        Long expectedDomainId = 26l;

        QuotaSummaryCmd cmd = new QuotaSummaryCmd();
        cmd.setAccountName(null);
        cmd.setDomainId(expectedDomainId);

        Mockito.doReturn(domainVoMock).when(domainDaoMock).findByIdIncludingRemoved(Mockito.anyLong());

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
        cmd.setDomainId(1L);

        Mockito.doReturn(null).when(domainDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        quotaResponseBuilderSpy.getQuotaSummaryResponseWithListAll(cmd, accountMock);
    }

    @Test
    public void getQuotaSummaryResponseWithListAllTestDomainIdIsNotNullPassDomainId() {
        Long expectedDomainId = 9837l;

        QuotaSummaryCmd cmd = Mockito.mock(QuotaSummaryCmd.class);
        Mockito.doReturn(expectedDomainId).when(cmd).getDomainId();
        Mockito.doReturn(-1L).when(cmd).getEntityOwnerId();

        Mockito.doReturn(domainVoMock).when(domainDaoMock).findByIdIncludingRemoved(Mockito.anyLong());

        Mockito.doReturn(null).when(quotaResponseBuilderSpy).getDomainPathByDomainIdForDomainAdmin(Mockito.any());
        Mockito.doReturn(quotaSummaryResponseMock1).when(quotaResponseBuilderSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Pair<List<QuotaSummaryResponse>, Integer> result = quotaResponseBuilderSpy.getQuotaSummaryResponseWithListAll(cmd, accountMock);

        Assert.assertEquals(quotaSummaryResponseMock1, result);
        Mockito.verify(quotaResponseBuilderSpy).getQuotaSummaryResponse(Mockito.any(), Mockito.any(), Mockito.eq(expectedDomainId), Mockito.any(), Mockito.any());
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
    public void isUserAllowedToSeeActivationRulesTestWithPermissionToCreateTariff() {
        ApiDiscoveryResponse response = new ApiDiscoveryResponse();
        response.setName("quotaTariffCreate");

        List<ApiDiscoveryResponse> cmdList = new ArrayList<>();
        cmdList.add(response);

        ListResponse<ApiDiscoveryResponse> responseList = new ListResponse<>();
        responseList.setResponses(cmdList);

        Mockito.doReturn(responseList).when(discoveryServiceMock).listApis(userMock, null, null);

        assertTrue(quotaResponseBuilderSpy.isUserAllowedToSeeActivationRules(userMock));
    }

    @Test
    public void isUserAllowedToSeeActivationRulesTestWithPermissionToUpdateTariff() {
        ApiDiscoveryResponse response = new ApiDiscoveryResponse();
        response.setName("quotaTariffUpdate");

        List<ApiDiscoveryResponse> cmdList = new ArrayList<>();
        cmdList.add(response);

        ListResponse<ApiDiscoveryResponse> responseList = new ListResponse<>();
        responseList.setResponses(cmdList);

        Mockito.doReturn(responseList).when(discoveryServiceMock).listApis(userMock, null, null);

        assertTrue(quotaResponseBuilderSpy.isUserAllowedToSeeActivationRules(userMock));
    }

    @Test
    public void isUserAllowedToSeeActivationRulesTestWithNoPermission() {
        ApiDiscoveryResponse response = new ApiDiscoveryResponse();
        response.setName("testCmd");

        List<ApiDiscoveryResponse> cmdList = new ArrayList<>();
        cmdList.add(response);

        ListResponse<ApiDiscoveryResponse> responseList = new ListResponse<>();
        responseList.setResponses(cmdList);

        Mockito.doReturn(responseList).when(discoveryServiceMock).listApis(userMock, null, null);

        assertFalse(quotaResponseBuilderSpy.isUserAllowedToSeeActivationRules(userMock));
    }

    @Test
    public void createQuotaTariffStatementItemResponseTestReturnsObject() {
        Mockito.doReturn("uuid").when(quotaTariffVoMock).getUuid();
        Mockito.doReturn("name").when(quotaTariffVoMock).getName();
        Mockito.doReturn(1).when(quotaTariffVoMock).getUsageType();
        Mockito.doReturn("usagename").when(quotaTariffVoMock).getUsageName();
        Mockito.doReturn("usageunit").when(quotaTariffVoMock).getUsageUnit();
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
        List<QuotaStatementItemResourceResponse> expectedResources = List.of(new QuotaStatementItemResourceResponse(), new QuotaStatementItemResourceResponse());

        Mockito.doReturn(expectedResources).when(quotaResponseBuilderSpy).createQuotaStatementItemResourceResponsesFromUsageValuesAggregatedByResourceId(Mockito.any(), Mockito.anyInt());

        quotaResponseBuilderSpy.setTariffStatementItemResources(statementItem, 1, new ArrayList<>(), new ArrayList<>());

        Assert.assertEquals(expectedResources, statementItem.getResources());
    }

    @Test
    public void createQuotaStatementItemResourceResponsesFromUsageValuesAggregatedByResourceIdTestReturnsListWithExpectedValues() {
        QuotaUsageResourceVO resource = new QuotaUsageResourceVO("uuid", "name", new Date());
        Mockito.doReturn(resource).when(quotaResponseBuilderSpy).getResourceFromIdAndType(Mockito.anyLong(), Mockito.anyInt());
        Map<Long, BigDecimal> resourceIdAndQuotaUsage = new HashMap<>();
        BigDecimal resourceQuotaUsage = BigDecimal.ONE;
        resourceIdAndQuotaUsage.put(1L, resourceQuotaUsage);

        List<QuotaStatementItemResourceResponse> response = quotaResponseBuilderSpy.createQuotaStatementItemResourceResponsesFromUsageValuesAggregatedByResourceId(resourceIdAndQuotaUsage, 1);

        Assert.assertEquals(response.size(), 1);
        Assert.assertEquals(resource.getUuid(), response.get(0).getResourceId());
        Assert.assertEquals(resource.getName(), response.get(0).getDisplayName());
        Assert.assertTrue(response.get(0).isRemoved());
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
        balance.setCreditBalance(BigDecimal.valueOf(-10.42));
        balances.add(balance);

        balance = new QuotaBalanceVO();
        balance.setUpdatedOn(new Date());
        balance.setCreditBalance(BigDecimal.valueOf(-18.94));
        balances.add(balance);

        balance = new QuotaBalanceVO();
        balance.setUpdatedOn(new Date());
        balance.setCreditBalance(BigDecimal.valueOf(-29.37));
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
    public void createQuotaCreditsListResponseTestReturnsObject() {
        List<QuotaCreditsVO> credits = new ArrayList<>();
        credits.add(new QuotaCreditsVO());
        QuotaCreditsResponse expectedQuotaCreditsResponse = new QuotaCreditsResponse();

        Mockito.doReturn(credits).when(quotaResponseBuilderSpy).getCreditsForQuotaCreditsList(Mockito.any());
        Mockito.doReturn(userVoMock).when(quotaResponseBuilderSpy).getCreditorForQuotaCreditsList(Mockito.any(), Mockito.any());
        Mockito.doReturn(expectedQuotaCreditsResponse).when(quotaResponseBuilderSpy).createQuotaCreditsResponse(credits.get(0), userVoMock);

        Pair<List<QuotaCreditsResponse>, Integer> result = quotaResponseBuilderSpy.createQuotaCreditsListResponse(createQuotaCreditsListCmdForTests());

        Assert.assertEquals(expectedQuotaCreditsResponse, result.first().get(0));
        Assert.assertEquals(1, (int) result.second());
    }

    private QuotaCreditsListCmd createQuotaCreditsListCmdForTests() {
        Mockito.doReturn(false).when(accountManagerMock).isNormalUser(Mockito.anyLong());
        QuotaCreditsListCmd cmd = new QuotaCreditsListCmd();
        cmd.setAccountId(1L);
        cmd.setDomainId(2L);
        return cmd;
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getCreditsForQuotaCreditsListTestThrowsInvalidParameterValueExceptionWhenStartDateIsAfterEndDate() {
        QuotaCreditsListCmd cmd = createQuotaCreditsListCmdForTests();
        cmd.setStartDate(new Date());
        cmd.setEndDate(DateUtils.addDays(new Date(), -1));

        quotaResponseBuilderSpy.getCreditsForQuotaCreditsList(cmd);
    }

    @Test(expected = PermissionDeniedException.class)
    public void getCreditsForQuotaCreditsListTestThrowsPermissionDeniedExceptionWhenDomainIdIsProvidedAndCallerIsNormalUser() {
        QuotaCreditsListCmd cmd = createQuotaCreditsListCmdForTests();
        Mockito.doReturn(true).when(accountManagerMock).isNormalUser(Mockito.anyLong());

        quotaResponseBuilderSpy.getCreditsForQuotaCreditsList(cmd);
    }

    @Test
    public void getCreditsForQuotaCreditsListTestReturnsData() {
        QuotaCreditsListCmd cmd = createQuotaCreditsListCmdForTests();
        List<QuotaCreditsVO> expected = new ArrayList<>();
        expected.add(new QuotaCreditsVO());

        Mockito.doReturn(expected).when(quotaCreditsDaoMock).findCredits(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.anyBoolean());

        List<QuotaCreditsVO> result = quotaResponseBuilderSpy.getCreditsForQuotaCreditsList(cmd);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getCreditorForQuotaCreditsListTestReturnsUserFromMapWhenMapHasCreditor() {
        Long creditorId = 1L;
        Map<Long, UserVO> userMap = new HashMap<>();

        userMap.put(creditorId, userVoMock);
        Mockito.doReturn(creditorId).when(quotaCreditsVoMock).getUpdatedBy();

        UserVO result = quotaResponseBuilderSpy.getCreditorForQuotaCreditsList(quotaCreditsVoMock, userMap);

        Assert.assertEquals(userVoMock, result);
    }

    @Test
    public void getCreditorForQuotaCreditsListTestGetsCreditorFromDatabaseAndAddsItToMapWhenMapDoesNotHaveCreditor() {
        Long creditorId = 1L;
        Map<Long, UserVO> userMap = new HashMap<>();

        Mockito.doReturn(creditorId).when(quotaCreditsVoMock).getUpdatedBy();
        Mockito.doReturn(userVoMock).when(userDaoMock).findByIdIncludingRemoved(creditorId);

        UserVO result = quotaResponseBuilderSpy.getCreditorForQuotaCreditsList(quotaCreditsVoMock, userMap);

        Assert.assertEquals(userVoMock, result);
        Assert.assertEquals(userVoMock, userMap.get(creditorId));
    }

    @Test
    public void getCreditorForQuotaCreditsTestReturnsCreditorWhenCreditorExists() {
        Long creditorId = 1L;

        Mockito.when(quotaCreditsVoMock.getUpdatedBy()).thenReturn(creditorId);
        Mockito.doReturn(userVoMock).when(userDaoMock).findByIdIncludingRemoved(creditorId);

        UserVO result = quotaResponseBuilderSpy.getCreditorForQuotaCredits(quotaCreditsVoMock);

        Assert.assertEquals(userVoMock, result);
    }

    @Test(expected = CloudRuntimeException.class)
    public void getCreditorForQuotaCreditsTestThrowsCloudRuntimeExceptionWhenCreditorDoesNotExist() {
        quotaResponseBuilderSpy.getCreditorForQuotaCredits(quotaCreditsVoMock);
    }

    @Test
    public void createQuotaCreditsResponseTestReturnsObject() {
        QuotaCreditsResponse expected = new QuotaCreditsResponse();
        expected.setCreditorUserId("test_uuid");
        expected.setCreditorUsername("test_name");
        expected.setCredit(new BigDecimal(41.5));
        expected.setCreditedOn(new Date());
        expected.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        expected.setPostingDate(new Date(1242421545757532L));
        expected.setObjectName("credit");

        Mockito.when(userVoMock.getUuid()).thenReturn(expected.getCreditorUserId());
        Mockito.when(userVoMock.getUsername()).thenReturn(expected.getCreditorUsername());
        Mockito.when(quotaCreditsVoMock.getCredit()).thenReturn(expected.getCredit());
        Mockito.when(quotaCreditsVoMock.getUpdatedOn()).thenReturn(expected.getCreditedOn());
        Mockito.when(quotaCreditsVoMock.getPostingDate()).thenReturn(expected.getPostingDate());

        QuotaCreditsResponse result = quotaResponseBuilderSpy.createQuotaCreditsResponse(quotaCreditsVoMock, userVoMock);

        Assert.assertEquals(expected.getCreditorUserId(), result.getCreditorUserId());
        Assert.assertEquals(expected.getCreditorUsername(), result.getCreditorUsername());
        Assert.assertEquals(expected.getCredit(), result.getCredit());
        Assert.assertEquals(expected.getCreditedOn(), result.getCreditedOn());
        Assert.assertEquals(expected.getPostingDate(), result.getPostingDate());
        Assert.assertEquals(expected.getCurrency(), result.getCurrency());
        Assert.assertEquals(expected.getObjectName(), result.getObjectName());
    }

    @Test
    public void validateActivationRuleTestValidateActivationRuleReturnValidScriptResponse() {
        Mockito.doReturn("if (account.name == 'test') { true } else { false }").when(quotaValidateActivationRuleCmdMock).getActivationRule();
        Mockito.doReturn(QuotaTypes.getQuotaType(30)).when(quotaValidateActivationRuleCmdMock).getQuotaType();
        Mockito.doReturn(quotaValidateActivationRuleCmdMock.getActivationRule()).when(jsInterpreterHelperMock).replaceScriptVariables(Mockito.anyString(), Mockito.any());

        QuotaValidateActivationRuleResponse response = quotaResponseBuilderSpy.validateActivationRule(quotaValidateActivationRuleCmdMock);

        Assert.assertTrue(response.isValid());
    }

    @Test
    public void validateActivationRuleTestUsageTypeIncompatibleVariableReturnInvalidScriptResponse() {
        Mockito.doReturn("if (value.osName == 'test') { true } else { false }").when(quotaValidateActivationRuleCmdMock).getActivationRule();
        Mockito.doReturn(QuotaTypes.getQuotaType(30)).when(quotaValidateActivationRuleCmdMock).getQuotaType();
        Mockito.doReturn(quotaValidateActivationRuleCmdMock.getActivationRule()).when(jsInterpreterHelperMock).replaceScriptVariables(Mockito.anyString(), Mockito.any());
        Mockito.when(jsInterpreterHelperMock.getScriptVariables(quotaValidateActivationRuleCmdMock.getActivationRule())).thenReturn(Set.of("value.osName"));

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
    public void isScriptVariablesValidTestUnsupportedUsageTypeVariablesReturnFalse() {
        Set<String> scriptVariables = new HashSet<>(List.of("value.computingResources.cpuNumber", "account.name", "zone.id"));
        List<String> usageTypeVariables = List.of("value.virtualSize", "account.name", "zone.id");

        boolean isScriptVariablesValid = quotaResponseBuilderSpy.isScriptVariablesValid(scriptVariables, usageTypeVariables);

        Assert.assertFalse(isScriptVariablesValid);
    }

    @Test
    public void isScriptVariablesValidTestSupportedUsageTypeVariablesReturnTrue() {
        Set<String> scriptVariables = new HashSet<>(List.of("value.computingResources.cpuNumber", "account.name", "zone.id"));
        List<String> usageTypeVariables = List.of("value.computingResources.cpuNumber", "account.name", "zone.id");

        boolean isScriptVariablesValid = quotaResponseBuilderSpy.isScriptVariablesValid(scriptVariables, usageTypeVariables);

        Assert.assertTrue(isScriptVariablesValid);
    }

    @Test
    public void isScriptVariablesValidTestVariablesUnrelatedToUsageTypeReturnTrue() {
        Set<String> scriptVariables = new HashSet<>(List.of("variable1.valid", "variable2.valid.", "variable3.valid"));
        List<String> usageTypeVariables = List.of("project.name", "account.id", "domain.path");

        boolean isScriptVariablesValid = quotaResponseBuilderSpy.isScriptVariablesValid(scriptVariables, usageTypeVariables);

        Assert.assertTrue(isScriptVariablesValid);
    }

    @Test
    public void injectUsageTypeVariablesTestReturnInjectedVariables() {
        JsInterpreter interpreter = Mockito.mock(JsInterpreter.class);

        Map<String, String> formattedVariables = quotaResponseBuilderSpy.injectUsageTypeVariables(interpreter, List.of("account.name", "zone.name"));

        Assert.assertTrue(formattedVariables.containsValue("accountname"));
        Assert.assertTrue(formattedVariables.containsValue("zonename"));
    }

    @Test
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        List<QuotaUsageJoinVO> quotaUsages = new ArrayList<>();

        QuotaUsageJoinVO quotaUsage = new QuotaUsageJoinVO();
        quotaUsage.setAccountId(1l);
        quotaUsage.setDomainId(2l);
        quotaUsage.setUsageType(3);
        quotaUsage.setQuotaUsed(BigDecimal.valueOf(10));
        try {
            quotaUsage.setStartDate(sdf.parse("2022-01-01"));
            quotaUsage.setEndDate(sdf.parse("2022-01-02"));
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
            quotaUsage.setStartDate(sdf.parse("2022-01-03"));
            quotaUsage.setEndDate(sdf.parse("2022-01-04"));
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
            quotaUsage.setStartDate(sdf.parse("2022-01-05"));
            quotaUsage.setEndDate(sdf.parse("2022-01-06"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        quotaUsages.add(quotaUsage);

        return quotaUsages;
    }

    @Test
    public void createStatementItemTestReturnItem() {
        List<QuotaUsageJoinVO> quotaUsages = getQuotaUsagesForTest();
        Mockito.doNothing().when(quotaResponseBuilderSpy).setStatementItemResources(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.anyBoolean());

        QuotaStatementItemResponse result = quotaResponseBuilderSpy.createStatementItem(0, quotaUsages, false);

        QuotaUsageJoinVO expected = quotaUsages.get(0);
        QuotaTypes quotaTypeExpected = QuotaTypes.listQuotaTypes().get(expected.getUsageType());
        Assert.assertEquals(BigDecimal.valueOf(15), result.getQuotaUsed());
        Assert.assertEquals(quotaTypeExpected.getQuotaUnit(), result.getUsageUnit());
        Assert.assertEquals(quotaTypeExpected.getQuotaName(), result.getUsageName());
    }

    @Test
    public void setStatementItemResourcesTestDoNotShowResourcesDoNothing() {
        QuotaStatementItemResponse item = new QuotaStatementItemResponse(1);

        quotaResponseBuilderSpy.setStatementItemResources(item, 0, getQuotaUsagesForTest(), false);

        Assert.assertNull(item.getResources());
    }

}
