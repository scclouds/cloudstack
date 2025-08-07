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
package org.apache.cloudstack.gui.theme;

import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.command.user.gui.theme.ListGuiThemesCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.gui.theme.dao.GuiThemeJoinDaoImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;


@RunWith(MockitoJUnitRunner.class)
public class GuiThemeServiceImplTest {

    @Mock
    GuiThemeJoinDaoImpl guiThemeJoinDaoMock;

    @Mock
    GuiThemeJoinVO guiThemeJoinVOMock;

    @Mock
    EntityManager entityManagerMock;

    @Mock
    Object objectMock;

    @Mock
    ListGuiThemesCmd listGuiThemesCmdMock;

    @Mock
    Account accountMock;

    @Mock
    AccountManager accountManagerMock;

    @Mock
    DomainVO domainVOMock;

    @Mock
    DomainDao domainDaoMock;

    @Mock
    CallContext callContextMock;

    @Spy
    @InjectMocks
    GuiThemeServiceImpl guiThemeServiceSpy;

    private static final String COMMON_NAME = "*acme.com,acm2.com";
    private static final String DOMAIN_UUIDS = "f26e7ffa-e830-40f5-aa70-e56d519793a8,f6b8ce6a-8153-4998-9d1c-dcae9eb11de7,19e87a19-4513-482b-9434-b15df9ba2a42";
    private static final String ACCOUNT_UUIDS = "0b669ef6-a05b-4263-a5a0-783259dc643d,cdd6674f-adf5-43d7-8db1-91a6d9ba0178,7b4e9444-8328-418c-a52b-52250ef3d3ef";
    private static final String BLANK_STRING = "";

    private static final long CALLER_ACCOUNT_ID = 10L;
    private static final String CALLER_ACCOUNT_UUID_IN_LIST = "7b4e9444-8328-418c-a52b-52250ef3d3ef";
    private static final String CALLER_ACCOUNT_UUID_OFF_LIST = "7b4e9444-8328-418c-a52b-52250ef3d3e0";
    private static final long CALLER_DOMAIN_ID = 100L;
    private static final long OTHER_DOMAIN_ID = 101L;
    private static final String UUID_IN_LIST_1 = "f26e7ffa-e830-40f5-aa70-e56d519793a8";
    private static final String UUID_IN_LIST_2 = "f6b8ce6a-8153-4998-9d1c-dcae9eb11de7";
    private static final String UUID_IN_LIST_3 = "19e87a19-4513-482b-9434-b15df9ba2a42";
    private static final String UUID_OFF_LIST = "f6b8ce6a-8153-4998-9d1c-dcae9eb11de0";

    @Test
    public void listGuiThemesTestCallerNoRolePermission() {
        Pair<List<GuiThemeVO>, Integer> emptyPair = new Pair<>(new ArrayList<>(), 0);
        Mockito.doReturn(false).when(listGuiThemesCmdMock).getListOnlyDefaultTheme();
        Mockito.doReturn(emptyPair).when(guiThemeServiceSpy).listGuiThemesWithNoAuth(Mockito.nullable(ListGuiThemesCmd.class));

        try (MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);
            Mockito.doReturn(accountMock).when(callContextMock).getCallingAccount();
            Mockito.doReturn(false).when(guiThemeServiceSpy).callerHasRolePermission(accountMock, null);

            guiThemeServiceSpy.listGuiThemes(listGuiThemesCmdMock);
            Mockito.verify(guiThemeServiceSpy, Mockito.times(1)).listGuiThemesWithNoAuth(Mockito.nullable(ListGuiThemesCmd.class));
        }
    }

    @Test
    public void listGuiThemesTestShouldCallNormalFlowWhenAuthenticatedAndRoleHasPermission() {
        Pair<List<GuiThemeVO>, Integer> emptyPair = new Pair<>(new ArrayList<>(), 0);
        Mockito.doReturn(false).when(listGuiThemesCmdMock).getListOnlyDefaultTheme();
        Mockito.doReturn(emptyPair).when(guiThemeServiceSpy).listGuiThemesInternal(Mockito.nullable(ListGuiThemesCmd.class));

        try (MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);
            Mockito.doReturn(accountMock).when(callContextMock).getCallingAccount();
            Mockito.doReturn(true).when(guiThemeServiceSpy).callerHasRolePermission(accountMock, null);

            guiThemeServiceSpy.listGuiThemes(listGuiThemesCmdMock);
            Mockito.verify(guiThemeServiceSpy, Mockito.times(1)).listGuiThemesInternal(Mockito.nullable(ListGuiThemesCmd.class));
        }
    }

    @Test
    public void listGuiThemesTestListOnlyDefaultThemesShouldCallFindDefaultTheme() {
        Mockito.doReturn(true).when(listGuiThemesCmdMock).getListOnlyDefaultTheme();

        try (MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);

            guiThemeServiceSpy.listGuiThemes(listGuiThemesCmdMock);
            Mockito.verify(guiThemeJoinDaoMock, Mockito.times(1)).findDefaultTheme();
        }
    }

    @Test
    public void shouldSetGuiThemeToPrivateTestDomainIdsAndAccountIdsAreBlankShouldReturnFalse() {
        boolean result = guiThemeServiceSpy.shouldSetGuiThemeToPrivate(BLANK_STRING, BLANK_STRING);

        Assert.assertFalse(result);
    }

    @Test
    public void shouldSetGuiThemeToPrivateTestDomainIdsNotBlankAndAccountIdsIsBlankShouldReturnTrue() {
        boolean result = guiThemeServiceSpy.shouldSetGuiThemeToPrivate(DOMAIN_UUIDS, BLANK_STRING);

        Assert.assertTrue(result);
    }

    @Test
    public void shouldSetGuiThemeToPrivateTestDomainIdsIsBlankAndAccountIdsIsNotBlankShouldReturnTrue() {
        boolean result = guiThemeServiceSpy.shouldSetGuiThemeToPrivate(BLANK_STRING, ACCOUNT_UUIDS);

        Assert.assertTrue(result);
    }

    @Test
    public void shouldSetGuiThemeToPrivateTestDomainIdsAndAccountIdsAreNotBlankShouldReturnTrue() {
        boolean result = guiThemeServiceSpy.shouldSetGuiThemeToPrivate(DOMAIN_UUIDS, ACCOUNT_UUIDS);

        Assert.assertTrue(result);
    }

    @Test
    public void validateObjectUuidsTestProvidedUuidsIsNullShouldNotThrowCloudRuntimeException() {
        guiThemeServiceSpy.validateObjectUuids(null, null);
    }

    @Test
    public void validateObjectUuidsTestProvidedUuidsIsBlankShouldNotThrowCloudRuntimeException() {
        guiThemeServiceSpy.validateObjectUuids(BLANK_STRING, null);
    }

    @Test
    public void validateObjectUuidsTestProvidedUuidsIsBlankAndCommaSeparatedShouldNotThrowCloudRuntimeException() {
        String blankCommaSeparatedString = ",,,";
        guiThemeServiceSpy.validateObjectUuids(blankCommaSeparatedString, null);
    }

    @Test
    public void validateObjectUuidsTestProvidedUuidIsValidShouldNotThrowCloudRuntimeException() {
        String validUuid = "4";

        Mockito.when(entityManagerMock.findByUuid(Mockito.any(Class.class), Mockito.anyString())).thenReturn(objectMock);
        guiThemeServiceSpy.validateObjectUuids(validUuid, Account.class);
    }

    @Test
    public void validateObjectUuidsTestProvidedUuidsAreValidShouldNotThrowCloudRuntimeException() {
        Mockito.when(entityManagerMock.findByUuid(Mockito.any(Class.class), Mockito.anyString())).thenReturn(objectMock);
        guiThemeServiceSpy.validateObjectUuids(ACCOUNT_UUIDS, Account.class);
    }

    @Test(expected = CloudRuntimeException.class)
    public void validateObjectUuidsTestProvidedUuidsAreNotValidShouldThrowCloudRuntimeException() {
        Mockito.when(entityManagerMock.findByUuid(Mockito.any(Class.class), Mockito.anyString())).thenReturn(null);
        guiThemeServiceSpy.validateObjectUuids(ACCOUNT_UUIDS, Account.class);
    }

    @Test
    public void checkIfDefaultThemeIsAllowedTestThemeIsNotConsideredDefault() {
        Mockito.when(guiThemeServiceSpy.isConsideredDefaultTheme(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(false);

        guiThemeServiceSpy.checkIfDefaultThemeIsAllowed(COMMON_NAME, BLANK_STRING, BLANK_STRING, null);
    }

    @Test
    public void checkIfDefaultThemeIsAllowedTestThemeIsConsideredDefaultAndThereIsNotADefaultThemeRegistered() {
        Mockito.when(guiThemeServiceSpy.isConsideredDefaultTheme(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        Mockito.when(guiThemeJoinDaoMock.findDefaultTheme()).thenReturn(null);

        guiThemeServiceSpy.checkIfDefaultThemeIsAllowed(BLANK_STRING, BLANK_STRING, BLANK_STRING, null);
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfDefaultThemeIsAllowedTestThemeIsConsideredDefaultAndThereIsADefaultThemeRegisteredShouldThrowCloudRuntimeException() {
        Mockito.when(guiThemeServiceSpy.isConsideredDefaultTheme(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        Mockito.when(guiThemeJoinDaoMock.findDefaultTheme()).thenReturn(guiThemeJoinVOMock);

        guiThemeServiceSpy.checkIfDefaultThemeIsAllowed(BLANK_STRING, BLANK_STRING, BLANK_STRING, null);
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfDefaultThemeIsAllowedTestThemeIsConsideredDefaultAndWillBeUpdatedAndIsDifferentIdShouldThrowCloudRuntimeException() {
        Mockito.when(guiThemeServiceSpy.isConsideredDefaultTheme(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        Mockito.when(guiThemeJoinDaoMock.findDefaultTheme()).thenReturn(guiThemeJoinVOMock);
        Mockito.when(guiThemeJoinVOMock.getId()).thenReturn(1L);

        guiThemeServiceSpy.checkIfDefaultThemeIsAllowed(BLANK_STRING, BLANK_STRING, BLANK_STRING, 2L);
    }

    @Test
    public void checkIfDefaultThemeIsAllowedTestThemeIsConsideredDefaultAndWillBeUpdatedAndIsTheSameShouldAllowUpdate() {
        Mockito.when(guiThemeServiceSpy.isConsideredDefaultTheme(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        Mockito.when(guiThemeJoinDaoMock.findDefaultTheme()).thenReturn(guiThemeJoinVOMock);
        Mockito.when(guiThemeJoinVOMock.getId()).thenReturn(1L);

        guiThemeServiceSpy.checkIfDefaultThemeIsAllowed(BLANK_STRING, BLANK_STRING, BLANK_STRING, 1L);
    }

    private void setupCallerHasAccessToResponseTests() {
        Mockito.lenient().doReturn(false).when(guiThemeJoinVOMock).getIsPublic();
        Mockito.lenient().doReturn(false).when(guiThemeJoinVOMock).isRecursiveDomains();
        Mockito.lenient().doReturn(false).when(accountManagerMock).isRootAdmin(CALLER_ACCOUNT_ID);
        Mockito.lenient().doReturn(CALLER_ACCOUNT_ID).when(accountMock).getId();
        Mockito.lenient().doReturn(CALLER_DOMAIN_ID).when(accountMock).getDomainId();
        Mockito.lenient().doReturn(CALLER_ACCOUNT_UUID_OFF_LIST).when(accountMock).getUuid();
        Mockito.lenient().doThrow(PermissionDeniedException.class).when(accountManagerMock).checkAccess(Mockito.any(Account.class), Mockito.any(Domain.class));
    }

    @Test
    public void callerHasAccessToResponseTestPublicThemeReturnTrue() {
        setupCallerHasAccessToResponseTests();
        Mockito.doReturn(true).when(guiThemeJoinVOMock).getIsPublic();
        Assert.assertTrue(guiThemeServiceSpy.callerHasAccessToResponse(accountMock, guiThemeJoinVOMock));
    }

    @Test
    public void callerHasAccessToResponseTestCallerIsRootAdminReturnTrue() {
        setupCallerHasAccessToResponseTests();
        Mockito.doReturn(true).when(accountManagerMock).isRootAdmin(Mockito.anyLong());
        Assert.assertTrue(guiThemeServiceSpy.callerHasAccessToResponse(accountMock, guiThemeJoinVOMock));
    }

    @Test
    public void callerHasAccessToResponseTestCallerAccountAllowedReturnTrue() {
        setupCallerHasAccessToResponseTests();
        Mockito.doReturn(CALLER_ACCOUNT_UUID_IN_LIST).when(accountMock).getUuid();
        Mockito.doReturn(ACCOUNT_UUIDS).when(guiThemeJoinVOMock).getAccounts();
        Assert.assertTrue(guiThemeServiceSpy.callerHasAccessToResponse(accountMock, guiThemeJoinVOMock));
    }

    @Test
    public void callerHasAccessToResponseTestCallerAccountNotAllowedAndNoDomainsAllowedReturnFalse() {
        setupCallerHasAccessToResponseTests();
        Mockito.lenient().doReturn(null).when(guiThemeJoinVOMock).getDomains();
        Assert.assertFalse(guiThemeServiceSpy.callerHasAccessToResponse(accountMock, guiThemeJoinVOMock));
    }

    @Test
    public void callerHasAccessToResponseTestCallerAccountNotAllowedDomainNotAllowedReturnFalse() {
        setupCallerHasAccessToResponseTests();
        Mockito.doReturn(DOMAIN_UUIDS).when(guiThemeJoinVOMock).getDomains();

        Mockito.doReturn(Mockito.mock(DomainVO.class)).when(domainDaoMock).findByUuid(UUID_IN_LIST_1);
        Mockito.doReturn(domainVOMock).when(domainDaoMock).findByUuid(UUID_IN_LIST_2);
        Mockito.doReturn(Mockito.mock(DomainVO.class)).when(domainDaoMock).findByUuid(UUID_IN_LIST_3);

        Mockito.doReturn(OTHER_DOMAIN_ID).when(domainVOMock).getId();
        Assert.assertFalse(guiThemeServiceSpy.callerHasAccessToResponse(accountMock, guiThemeJoinVOMock));
    }

    @Test
    public void callerHasAccessToResponseTestCallerDomainDirectlyAllowedReturnTrue() {
        setupCallerHasAccessToResponseTests();
        Mockito.doReturn(DOMAIN_UUIDS).when(guiThemeJoinVOMock).getDomains();

        Mockito.doReturn(Mockito.mock(DomainVO.class)).when(domainDaoMock).findByUuid(UUID_IN_LIST_1);
        Mockito.doReturn(domainVOMock).when(domainDaoMock).findByUuid(UUID_IN_LIST_2);

        Mockito.doReturn(CALLER_DOMAIN_ID).when(domainVOMock).getId();
        Assert.assertTrue(guiThemeServiceSpy.callerHasAccessToResponse(accountMock, guiThemeJoinVOMock));
    }

    @Test
    public void callerHasAccessToResponseTestCallerHasAccessToAllowedDomainReturnTrue() {
        setupCallerHasAccessToResponseTests();
        Mockito.doReturn(DOMAIN_UUIDS).when(guiThemeJoinVOMock).getDomains();

        Mockito.doReturn(Mockito.mock(DomainVO.class)).when(domainDaoMock).findByUuid(UUID_IN_LIST_1);
        Mockito.doReturn(domainVOMock).when(domainDaoMock).findByUuid(UUID_IN_LIST_2);

        Mockito.doNothing().when(accountManagerMock).checkAccess(accountMock, domainVOMock);

        Assert.assertTrue(guiThemeServiceSpy.callerHasAccessToResponse(accountMock, guiThemeJoinVOMock));
    }

    @Test
    public void callerHasAccessToResponseTestCallerDomainChildOfAllowedDomainOfRecursiveThemeReturnTrue() {
        setupCallerHasAccessToResponseTests();
        Mockito.doReturn(DOMAIN_UUIDS).when(guiThemeJoinVOMock).getDomains();

        Mockito.doReturn(Mockito.mock(DomainVO.class)).when(domainDaoMock).findByUuid(UUID_IN_LIST_1);
        Mockito.doReturn(domainVOMock).when(domainDaoMock).findByUuid(UUID_IN_LIST_2);

        Mockito.doReturn(true).when(guiThemeJoinVOMock).isRecursiveDomains();
        Mockito.doReturn(true).when(domainDaoMock).isChildDomain(OTHER_DOMAIN_ID, CALLER_DOMAIN_ID);

        Mockito.doReturn(OTHER_DOMAIN_ID).when(domainVOMock).getId();
        Assert.assertTrue(guiThemeServiceSpy.callerHasAccessToResponse(accountMock, guiThemeJoinVOMock));
    }
}
