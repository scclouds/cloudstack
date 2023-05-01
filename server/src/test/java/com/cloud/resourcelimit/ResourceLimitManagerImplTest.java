// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.resourcelimit;

import com.cloud.configuration.Resource;
import com.cloud.configuration.ResourceCountVO;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Resource.ResourceType.class)
public class ResourceLimitManagerImplTest {

    @Spy
    @InjectMocks
    private ResourceLimitManagerImpl resourceLimitManagerImplSpy = new ResourceLimitManagerImpl();

    @Mock
    private ResourceCountDao resourceCountDaoMock;

    @Mock
    private AccountDao accountDaoMock;

    @Mock
    private ProjectDao projectDaoMock;

    @Mock
    private DomainDao domainDaoMock;

    @Mock
    private Domain domainMock;

    @Mock
    private Account accountMock;

    @Mock
    private AccountVO accountVoMock;

    @Mock
    private DomainVO domainVoMock;

    @Mock
    private ResourceCountVO resourceCountVoMock;

    private final Resource.ResourceType[] resourceTypes = Resource.ResourceType.values();
    private final Long domainId = new Random().nextLong();
    private final Long accountId = new Random().nextLong();

    @Test
    public void recalculateResourceCountForDomainAndResourceTypeTestDomainAndTypeNotNullReturnValue() {
        Mockito.doReturn(null).when(resourceLimitManagerImplSpy).lockDomainRows(Mockito.anyLong(), Mockito.any());
        Mockito.doNothing().when(resourceCountDaoMock).setResourceCount(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.anyLong());
        Mockito.doReturn(resourceCountVoMock).when(resourceCountDaoMock).findByOwnerAndType(Mockito.anyLong(), Mockito.any(), Mockito.any());

        for (Resource.ResourceType resourceType : resourceTypes) {
            long expected = new Random().nextLong();
            Mockito.doReturn(expected).when(resourceLimitManagerImplSpy).getNewResourceCountForDomainAndResourceType(Mockito.any(), Mockito.any());
            long result = resourceLimitManagerImplSpy.recalculateResourceCountForDomainAndResourceType(domainMock, resourceType);
            Assert.assertEquals(expected, result);
        }
    }

    @Test
    public void getResourceTypeTestParameterNullReturnNull() {
        Assert.assertNull(resourceLimitManagerImplSpy.getResourceType(null));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getResourceTypeTestResourceTypeDoesNotExistThrowInvalidParameterValueException() {
        PowerMockito.mockStatic(Resource.ResourceType.class);
        PowerMockito.when(Resource.ResourceType.fromOrdinal(Mockito.anyInt())).thenReturn(null);
        resourceLimitManagerImplSpy.getResourceType(1);
    }

    @Test
    public void getResourceTypeTestResourceTypeExistReturnIt() {
        PowerMockito.mockStatic(Resource.ResourceType.class);

        for (Resource.ResourceType expected : resourceTypes) {
            PowerMockito.when(Resource.ResourceType.fromOrdinal(Mockito.anyInt())).thenReturn(expected);
            Resource.ResourceType result = resourceLimitManagerImplSpy.getResourceType(1);
            Assert.assertEquals(expected, result);
        }
    }

    @Test
    public void getAccountOrDomainResourceCountRecalculatedTestAccountAndDomainNullAndTypeNotNullThrowNullPointerException() {
        long random = new Random().nextLong();
        Mockito.doReturn(random).when(resourceLimitManagerImplSpy).recalculateResourceCountForDomainAndResourceTypeInTransaction(Mockito.any(), Mockito.any());
        for (Resource.ResourceType type : resourceTypes) {
            Assert.assertThrows(NullPointerException.class, () -> resourceLimitManagerImplSpy.getAccountOrDomainResourceCountRecalculated(null, null, type));
        }
    }

    @Test
    public void getAccountOrDomainResourceCountRecalculatedTestAccountNullAndDomainAndTypeNotNullCallRecalculateResourceCountForDomainAndResourceTypeInTransaction() {
        Mockito.doReturn(domainId).when(domainMock).getId();

        for (Resource.ResourceType type : resourceTypes) {
            long expectedCount = new Random().nextLong();
            Mockito.doReturn(expectedCount).when(resourceLimitManagerImplSpy).recalculateResourceCountForDomainAndResourceTypeInTransaction(Mockito.any(), Mockito.any());

            ResourceCountVO result = resourceLimitManagerImplSpy.getAccountOrDomainResourceCountRecalculated(null, domainMock, type);

            Assert.assertEquals(expectedCount, result.getCount());
            Assert.assertEquals(domainId, result.getDomainId());
            Assert.assertEquals(type, result.getType());
            Assert.assertNull(result.getAccountId());
        }
    }

    @Test
    public void getAccountOrDomainResourceCountRecalculatedTestAccountAndTypeNotNullAndDomainNullCallRecalculateAccountResourceCount() {
        Mockito.doReturn(accountId).when(accountMock).getId();

        for (Resource.ResourceType type : resourceTypes) {
            long expectedCount = new Random().nextLong();
            Mockito.doReturn(expectedCount).when(resourceLimitManagerImplSpy).recalculateAccountResourceCount(Mockito.any(), Mockito.any());

            ResourceCountVO result = resourceLimitManagerImplSpy.getAccountOrDomainResourceCountRecalculated(accountMock, null, type);

            Assert.assertEquals(expectedCount, result.getCount());
            Assert.assertNull(result.getDomainId());
            Assert.assertEquals(type, result.getType());
            Assert.assertEquals(accountId, result.getAccountId());
        }
    }

    @Test
    public void getAccountOrDomainResourceCountRecalculatedTestAccountAndTypeAndDomainNotNullCallRecalculateAccountResourceCount() {
        Mockito.doReturn(accountId).when(accountMock).getId();

        for (Resource.ResourceType type : resourceTypes) {
            long expectedCount = new Random().nextLong();
            Mockito.doReturn(expectedCount).when(resourceLimitManagerImplSpy).recalculateAccountResourceCount(Mockito.any(), Mockito.any());

            ResourceCountVO result = resourceLimitManagerImplSpy.getAccountOrDomainResourceCountRecalculated(accountMock, domainMock, type);

            Assert.assertEquals(expectedCount, result.getCount());
            Assert.assertNull(result.getDomainId());
            Assert.assertEquals(type, result.getType());
            Assert.assertEquals(accountId, result.getAccountId());
        }
    }

    @Test
    public void getNewResourceCountForAccountsOfDomainAndResourceTypeTestDomainAndTypeNotNullAndActiveAccountsIsNullReturnZero() {
        long expected = 0;
        Mockito.doReturn(null).when(accountDaoMock).findActiveAccountsForDomain(Mockito.any());

        for (Resource.ResourceType type : resourceTypes) {
            long result = resourceLimitManagerImplSpy.getNewResourceCountForAccountsOfDomainAndResourceType(domainMock, type);
            Assert.assertEquals(expected, result);
        }
    }

    @Test
    public void getNewResourceCountForAccountsOfDomainAndResourceTypeTestDomainNotNullAndTypeNotNullAndActiveAccountsIsEmptyReturnZero() {
        long expected = 0;
        Mockito.doReturn(new ArrayList<>()).when(accountDaoMock).findActiveAccountsForDomain(Mockito.any());

        for (Resource.ResourceType type : resourceTypes) {
            long result = resourceLimitManagerImplSpy.getNewResourceCountForAccountsOfDomainAndResourceType(domainMock, type);
            Assert.assertEquals(expected, result);
        }
    }

    @Test
    public void getNewResourceCountForAccountsOfDomainAndResourceTypeTestDomainAndTypeNotNullAndActiveAccountsIsEmptyReturnZero() {
        List<Account> accounts = List.of(accountVoMock, accountVoMock, accountVoMock);
        Mockito.doReturn(accounts).when(accountDaoMock).findActiveAccountsForDomain(Mockito.any());
        Mockito.doReturn(1l).when(resourceLimitManagerImplSpy).recalculateAccountResourceCount(Mockito.any(), Mockito.any());

        for (Resource.ResourceType type : resourceTypes) {
            long result = resourceLimitManagerImplSpy.getNewResourceCountForAccountsOfDomainAndResourceType(domainMock, type);
            Assert.assertEquals(accounts.size(), result);
        }

        Mockito.verify(resourceLimitManagerImplSpy, Mockito.times(resourceTypes.length * accounts.size())).recalculateAccountResourceCount(Mockito.any(), Mockito.any());
    }

    @Test
    public void getNewResourceCountForDomainAndResourceTypeTestAllTypesReturnValue() {
        Mockito.doReturn(3l).when(projectDaoMock).countProjectsForDomain(Mockito.anyLong());
        Mockito.doReturn(1l).when(resourceLimitManagerImplSpy).getNewResourceCountForAccountsOfDomainAndResourceType(Mockito.any(), Mockito.any());
        Mockito.doReturn(2l).when(resourceLimitManagerImplSpy).getNewResourceCountForSubdomainsAndResourceType(Mockito.any(), Mockito.any());

        for (Resource.ResourceType type : resourceTypes) {
            long result = resourceLimitManagerImplSpy.getNewResourceCountForDomainAndResourceType(domainMock, type);
            if (type == Resource.ResourceType.project) {
                Assert.assertEquals(6l, result);
            } else {
                Assert.assertEquals(3l, result);
            }
        }
    }

    @Test
    public void getNewResourceCountForSubdomainsAndResourceTypeTestTypeNotNullAndDomainChildrenNullReturnZero() {
        long expected = 0;
        Mockito.doReturn(null).when(domainDaoMock).findImmediateChildrenForParent(Mockito.any());

        for (Resource.ResourceType type : resourceTypes) {
            long result = resourceLimitManagerImplSpy.getNewResourceCountForSubdomainsAndResourceType(domainMock, type);
            Assert.assertEquals(expected, result);
        }
    }

    @Test
    public void getNewResourceCountForSubdomainsAndResourceTypeTestTypeNotNullAndDomainChildrenIsEmptyReturnZero() {
        long expected = 0;
        Mockito.doReturn(new ArrayList<>()).when(domainDaoMock).findImmediateChildrenForParent(Mockito.any());

        for (Resource.ResourceType type : resourceTypes) {
            long result = resourceLimitManagerImplSpy.getNewResourceCountForSubdomainsAndResourceType(domainMock, type);
            Assert.assertEquals(expected, result);
        }
    }

    @Test
    public void getNewResourceCountForSubdomainsAndResourceTypeTestTypeNotNullAndDomainChildrenIsNotEmptyReturnZero() {
        List<Domain> domains = List.of(domainVoMock, domainVoMock, domainVoMock);
        Mockito.doReturn(domains).when(domainDaoMock).findImmediateChildrenForParent(Mockito.any());
        Mockito.doReturn(1l).when(resourceLimitManagerImplSpy).recalculateResourceCountForDomainAndResourceTypeInTransaction(Mockito.any(), Mockito.any());

        for (Resource.ResourceType type : resourceTypes) {
            long result = resourceLimitManagerImplSpy.getNewResourceCountForSubdomainsAndResourceType(domainMock, type);
            Assert.assertEquals(domains.size(), result);
        }

        Mockito.verify(resourceLimitManagerImplSpy, Mockito.times(resourceTypes.length * domains.size()))
                .recalculateResourceCountForDomainAndResourceTypeInTransaction(Mockito.any(), Mockito.any());
    }
}
