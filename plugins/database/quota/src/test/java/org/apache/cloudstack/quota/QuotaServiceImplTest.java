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
package org.apache.cloudstack.quota;

import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import junit.framework.TestCase;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaUsageJoinDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class QuotaServiceImplTest extends TestCase {

    @Mock
    AccountDao accountDaoMock;
    @Mock
    QuotaAccountDao quotaAccountDaoMock;
    @Mock
    DomainDao domainDaoMock;
    @Mock
    QuotaBalanceDao quotaBalanceDaoMock;
    @Mock
    QuotaUsageJoinDao quotaUsageJoinDaoMock;

    @Mock
    private AccountVO accountVoMock;

    @Spy
    @InjectMocks
    QuotaServiceImpl quotaServiceImplSpy;

    @Before
    public void setup() throws ConfigurationException {
        quotaServiceImplSpy.configure("randomName", null);
    }

    @Test
    public void testGetQuotaUsage() {
        final long accountId = 2L;
        final String accountName = "admin123";
        final long domainId = 1L;
        final Date startDate = new DateTime().minusDays(2).toDate();
        final Date endDate = new Date();

        Mockito.doReturn(accountId).when(quotaServiceImplSpy).getAccountToWhomQuotaBalancesWillBeListed(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong());

        quotaServiceImplSpy.getQuotaUsage(accountId, accountName, domainId, QuotaTypes.IP_ADDRESS, startDate, endDate);
        Mockito.verify(quotaUsageJoinDaoMock, Mockito.times(1)).findQuotaUsage(Mockito.eq(accountId), Mockito.eq(domainId), Mockito.eq(QuotaTypes.IP_ADDRESS), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(Date.class), Mockito.any(Date.class));
    }

    @Test
    public void testSetLockAccount() {
        // existing account
        QuotaAccountVO quotaAccountVO = new QuotaAccountVO();
        Mockito.when(quotaAccountDaoMock.findByIdQuotaAccount(Mockito.anyLong())).thenReturn(quotaAccountVO);
        quotaServiceImplSpy.setLockAccount(2L, true);
        Mockito.verify(quotaAccountDaoMock, Mockito.times(0)).persistQuotaAccount(Mockito.any(QuotaAccountVO.class));
        Mockito.verify(quotaAccountDaoMock, Mockito.times(1)).updateQuotaAccount(Mockito.anyLong(), Mockito.any(QuotaAccountVO.class));

        // new account
        Mockito.when(quotaAccountDaoMock.findByIdQuotaAccount(Mockito.anyLong())).thenReturn(null);
        quotaServiceImplSpy.setLockAccount(2L, true);
        Mockito.verify(quotaAccountDaoMock, Mockito.times(1)).persistQuotaAccount(Mockito.any(QuotaAccountVO.class));
    }

    @Test
    public void testSetMinBalance() {
        final long accountId = 2L;
        final double balance = 10.3F;

        // existing account setting
        QuotaAccountVO quotaAccountVO = new QuotaAccountVO();
        Mockito.when(quotaAccountDaoMock.findByIdQuotaAccount(Mockito.anyLong())).thenReturn(quotaAccountVO);
        quotaServiceImplSpy.setMinBalance(accountId, balance);
        Mockito.verify(quotaAccountDaoMock, Mockito.times(0)).persistQuotaAccount(Mockito.any(QuotaAccountVO.class));
        Mockito.verify(quotaAccountDaoMock, Mockito.times(1)).updateQuotaAccount(Mockito.anyLong(), Mockito.any(QuotaAccountVO.class));

        // no account with limit set
        Mockito.when(quotaAccountDaoMock.findByIdQuotaAccount(Mockito.anyLong())).thenReturn(null);
        quotaServiceImplSpy.setMinBalance(accountId, balance);
        Mockito.verify(quotaAccountDaoMock, Mockito.times(1)).persistQuotaAccount(Mockito.any(QuotaAccountVO.class));
    }

    @Test
    public void getAccountToWhomQuotaBalancesWillBeListedTestAccountIdIsNotNullReturnsExistingAccount() {
        long expected = 1L;
        Mockito.doReturn(accountVoMock).when(accountDaoMock).findByIdIncludingRemoved(Mockito.anyLong());
        long result = quotaServiceImplSpy.getAccountToWhomQuotaBalancesWillBeListed(expected, "test", 2L);
        Assert.assertEquals(expected, result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getAccountToWhomQuotaBalancesWillBeListedTestAccountIdIsNotValidThrowsInvalidParameterValueException() {
        Mockito.doReturn(null).when(accountDaoMock).findActiveAccount(Mockito.anyString(), Mockito.anyLong());
        Mockito.doNothing().when(quotaServiceImplSpy).validateIsChildDomain(Mockito.anyString(), Mockito.anyLong());
        quotaServiceImplSpy.getAccountToWhomQuotaBalancesWillBeListed(null, "test", 2L);
    }

    @Test
    public void getAccountToWhomQuotaBalancesWillBeListedTestReturnsFirstAccountId() {
        long expected = 8302L;

        Mockito.doNothing().when(quotaServiceImplSpy).validateIsChildDomain(Mockito.anyString(), Mockito.anyLong());

        AccountVO accountVo = new AccountVO();
        accountVo.setId(expected);

        Mockito.doReturn(accountVo).when(accountDaoMock).findActiveAccount(Mockito.anyString(), Mockito.anyLong());

        long result = quotaServiceImplSpy.getAccountToWhomQuotaBalancesWillBeListed(null, "test", 9136L);

        Assert.assertEquals(expected, result);
    }

    @Test(expected = PermissionDeniedException.class)
    public void validateIsChildDomainTestIsNotChildDomainThrowsPermissionDeniedException() {
        CallContext callContextMock = Mockito.mock(CallContext.class);

        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            Mockito.when(CallContext.current()).thenReturn(callContextMock);
            Mockito.doReturn(Mockito.mock(Account.class)).when(callContextMock).getCallingAccount();
            Mockito.doReturn(false).when(domainDaoMock).isChildDomain(Mockito.anyLong(), Mockito.anyLong());;

            quotaServiceImplSpy.validateIsChildDomain("test", 1L);
        }
    }

    @Test
    public void validateIsChildDomainTestIsChildDomainDoNothing() {
        CallContext callContextMock = Mockito.mock(CallContext.class);

        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            Mockito.when(CallContext.current()).thenReturn(callContextMock);
            Mockito.doReturn(Mockito.mock(Account.class)).when(callContextMock).getCallingAccount();
            Mockito.doReturn(true).when(domainDaoMock).isChildDomain(Mockito.anyLong(), Mockito.anyLong());;

            quotaServiceImplSpy.validateIsChildDomain("test", 1L);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateStartDateAndEndDateForListQuotaBalancesForAccountTestStartDateIsNullAndEndDateIsNotNullThrowsInvalidParameterException() {
        quotaServiceImplSpy.validateStartDateAndEndDateForListQuotaBalancesForAccount(null, new Date());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateStartDateAndEndDateForListQuotaBalancesForAccountTestStartDateIsAfterNowThrowsInvalidParameterValueException() {
        Date startDate = DateUtils.addMinutes(new Date(), 1);
        quotaServiceImplSpy.validateStartDateAndEndDateForListQuotaBalancesForAccount(startDate, null);
    }

    @Test
    public void validateStartDateAndEndDateForListQuotaBalancesForAccountTestEndDateIsAfterNowDoesNothing() {
        Date startDate = DateUtils.addMinutes(new Date(), -1);
        Date endDate = DateUtils.addMinutes(new Date(), 1);
        quotaServiceImplSpy.validateStartDateAndEndDateForListQuotaBalancesForAccount(startDate, endDate);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateStartDateAndEndDateForListQuotaBalancesForAccountTestStartDateIsAfterEndDateThrowsInvalidParameterValueException() {
        Date startDate = DateUtils.addMinutes(new Date(), -10);
        Date endDate = DateUtils.addMinutes(new Date(), -15);
        quotaServiceImplSpy.validateStartDateAndEndDateForListQuotaBalancesForAccount(startDate, endDate);
    }

    @Test
    public void listQuotaBalancesForAccountTestLastQuotaBalanceIsNullReturnsNull() {
        Mockito.doReturn(1L).when(quotaServiceImplSpy).getAccountToWhomQuotaBalancesWillBeListed(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong());
        Mockito.doNothing().when(quotaServiceImplSpy).validateStartDateAndEndDateForListQuotaBalancesForAccount(Mockito.any(), Mockito.any());
        Mockito.doReturn(null).when(quotaBalanceDaoMock).getLastQuotaBalanceEntry(Mockito.anyLong(), Mockito.anyLong(), Mockito.any());

        List<QuotaBalanceVO> result = quotaServiceImplSpy.listQuotaBalancesForAccount(1L, "test", 2L, null, null);

        Assert.assertNull(result);
    }

    @Test
    public void listQuotaBalancesForAccountTestLastQuotaBalanceIsNotNullReturnsIt() {
        QuotaBalanceVO expected = new QuotaBalanceVO();

        Mockito.doReturn(1L).when(quotaServiceImplSpy).getAccountToWhomQuotaBalancesWillBeListed(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong());
        Mockito.doNothing().when(quotaServiceImplSpy).validateStartDateAndEndDateForListQuotaBalancesForAccount(Mockito.any(), Mockito.any());
        Mockito.doReturn(expected).when(quotaBalanceDaoMock).getLastQuotaBalanceEntry(Mockito.anyLong(), Mockito.anyLong(), Mockito.any());

        List<QuotaBalanceVO> result = quotaServiceImplSpy.listQuotaBalancesForAccount(1L, "test", 2L, null, null);

        Assert.assertEquals(expected, result.get(0));
    }

    @Test
    public void listQuotaBalancesForAccountTestReturnsQuotaBalances() {
        List<QuotaBalanceVO> expected = new ArrayList<>();

        Mockito.doReturn(1L).when(quotaServiceImplSpy).getAccountToWhomQuotaBalancesWillBeListed(Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong());
        Mockito.doNothing().when(quotaServiceImplSpy).validateStartDateAndEndDateForListQuotaBalancesForAccount(Mockito.any(), Mockito.any());
        Mockito.doReturn(expected).when(quotaBalanceDaoMock).listQuotaBalances(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any());

        List<QuotaBalanceVO> result = quotaServiceImplSpy.listQuotaBalancesForAccount(1L, "test", 2L, new Date(), null);

        Assert.assertEquals(expected, result);
    }

}
