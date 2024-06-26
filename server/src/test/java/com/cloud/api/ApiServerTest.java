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
package com.cloud.api;

import java.util.ArrayList;
import java.util.List;

import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.user.Account;
import com.cloud.user.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpSession;

@RunWith(MockitoJUnitRunner.class)
public class ApiServerTest {

    @Mock
    private HttpSession httpSessionMock;
    @Mock
    private User userMock;
    @Mock
    private Account accountMock;
    @Mock
    private Project projectMock;
    @Mock
    private ProjectManager projectManagerMock;

    @InjectMocks
    ApiServer apiServer = new ApiServer();

    private static final long ACCOUNT_ID = 1L;
    private static final long USER_ID = 2L;
    private static final long PROJECT_ID = 3L;
    private static final String PROJECT_UUID = "projectuuid";

    @Before
    public void setup() {
        Mockito.doReturn(ACCOUNT_ID).when(accountMock).getId();
        Mockito.doReturn(ACCOUNT_ID).when(userMock).getAccountId();

        Mockito.doReturn(USER_ID).when(userMock).getId();

        Mockito.doReturn(PROJECT_ID).when(projectMock).getId();
        Mockito.doReturn(PROJECT_UUID).when(projectMock).getUuid();

    }

    private void runTestSetupIntegrationPortListenerInvalidPorts(Integer port) {
        try (MockedConstruction<ApiServer.ListenerThread> mocked =
                     Mockito.mockConstruction(ApiServer.ListenerThread.class)) {
            apiServer.setupIntegrationPortListener(port);
            Assert.assertTrue(mocked.constructed().isEmpty());
        }
    }

    @Test
    public void testSetupIntegrationPortListenerInvalidPorts() {
        List<Integer> ports = new ArrayList<>(List.of(-1, -10, 0));
        ports.add(null);
        for (Integer port : ports) {
            runTestSetupIntegrationPortListenerInvalidPorts(port);
        }
    }

    @Test
    public void testSetupIntegrationPortListenerValidPort() {
        Integer validPort = 8080;
        try (MockedConstruction<ApiServer.ListenerThread> mocked =
                     Mockito.mockConstruction(ApiServer.ListenerThread.class)) {
            apiServer.setupIntegrationPortListener(validPort);
            Assert.assertFalse(mocked.constructed().isEmpty());
            ApiServer.ListenerThread listenerThread = mocked.constructed().get(0);
            Mockito.verify(listenerThread).start();
        }
    }

    private void setupSetAttributeDefaultProjectTests() {
        Mockito.doReturn(PROJECT_ID).when(userMock).getDefaultProjectId();
        Mockito.doReturn(projectMock).when(projectManagerMock).getProject(PROJECT_ID);
        Mockito.doReturn(true).when(projectManagerMock).canUserAccessProject(USER_ID, ACCOUNT_ID, PROJECT_ID);

        Mockito.doReturn(PROJECT_ID).when(accountMock).getDefaultProjectId();
        Mockito.doReturn(true).when(projectManagerMock).canAccountAccessProject(ACCOUNT_ID, PROJECT_ID);
    }
    @Test
    public void setAttributeDefaultProjectTestUserDefaultProjectExistsAndAccessible() {
        setupSetAttributeDefaultProjectTests();

        apiServer.setAttributeDefaultProject(httpSessionMock, userMock, accountMock);

        Mockito.verify(httpSessionMock).setAttribute("defaultprojectid", PROJECT_UUID);
    }
    @Test
    public void setAttributeDefaultProjectTestUserDefaultProjectNotExistsAccountDefaultProjectNull() {
        setupSetAttributeDefaultProjectTests();
        Mockito.doReturn(null).when(projectManagerMock).getProject(PROJECT_ID);
        Mockito.doReturn(null).when(accountMock).getDefaultProjectId();

        apiServer.setAttributeDefaultProject(httpSessionMock, userMock, accountMock);

        Mockito.verify(httpSessionMock, Mockito.never()).setAttribute("defaultprojectid", PROJECT_UUID);
    }
    @Test
    public void setAttributeDefaultProjectTestUserDefaultProjectNotExistsAccountDefaultProjectExistsAndAcessible() {
        setupSetAttributeDefaultProjectTests();
        Mockito.doReturn(null).doReturn(projectMock).when(projectManagerMock).getProject(PROJECT_ID);

        apiServer.setAttributeDefaultProject(httpSessionMock, userMock, accountMock);

        Mockito.verify(httpSessionMock).setAttribute("defaultprojectid", PROJECT_UUID);
    }
    @Test
    public void setAttributeDefaultProjectTestUserDefaultProjectExistsNotAccessibleAccountDefaultProjectNull() {
        setupSetAttributeDefaultProjectTests();
        Mockito.doReturn(false).when(projectManagerMock).canUserAccessProject(USER_ID, ACCOUNT_ID, PROJECT_ID);
        Mockito.doReturn(projectMock).doReturn(null).when(projectManagerMock).getProject(PROJECT_ID);

        apiServer.setAttributeDefaultProject(httpSessionMock, userMock, accountMock);

        Mockito.verify(httpSessionMock, Mockito.never()).setAttribute("defaultprojectid", PROJECT_UUID);
    }
    @Test
    public void setAttributeDefaultProjectTestUserDefaultProjectExistsNotAccessibleAccountDefaultProjectExistsAndAccessible() {
        setupSetAttributeDefaultProjectTests();
        Mockito.doReturn(false).when(projectManagerMock).canUserAccessProject(USER_ID, ACCOUNT_ID, PROJECT_ID);

        apiServer.setAttributeDefaultProject(httpSessionMock, userMock, accountMock);

        Mockito.verify(httpSessionMock).setAttribute("defaultprojectid", PROJECT_UUID);
    }
    @Test
    public void setAttributeDefaultProjectTestUserDefaultProjectNullAccountDefaultProjectNull() {
        setupSetAttributeDefaultProjectTests();
        Mockito.doReturn(null).when(userMock).getDefaultProjectId();
        Mockito.doReturn(null).when(accountMock).getDefaultProjectId();

        apiServer.setAttributeDefaultProject(httpSessionMock, userMock, accountMock);

        Mockito.verify(httpSessionMock, Mockito.never()).setAttribute("defaultprojectid", PROJECT_UUID);
    }
    @Test
    public void setAttributeDefaultProjectTestUserDefaultProjectNullAccountDefaultProjectNotExists() {
        setupSetAttributeDefaultProjectTests();
        Mockito.doReturn(null).when(userMock).getDefaultProjectId();
        Mockito.doReturn(null).when(projectManagerMock).getProject(PROJECT_ID);

        apiServer.setAttributeDefaultProject(httpSessionMock, userMock, accountMock);

        Mockito.verify(httpSessionMock, Mockito.never()).setAttribute("defaultprojectid", PROJECT_UUID);
    }
    @Test
    public void setAttributeDefaultProjectTestUserDefaultProjectNullAccountDefaultProjectExistsNotAccessible() {
        setupSetAttributeDefaultProjectTests();
        Mockito.doReturn(null).when(userMock).getDefaultProjectId();
        Mockito.doReturn(false).when(projectManagerMock).canAccountAccessProject(ACCOUNT_ID, PROJECT_ID);

        apiServer.setAttributeDefaultProject(httpSessionMock, userMock, accountMock);

        Mockito.verify(httpSessionMock, Mockito.never()).setAttribute("defaultprojectid", PROJECT_UUID);
    }
    @Test
    public void setAttributeDefaultProjectTestUserDefaultProjectNullAccountDefaultProjectExistsAccessible() {
        setupSetAttributeDefaultProjectTests();
        Mockito.doReturn(null).when(userMock).getDefaultProjectId();

        apiServer.setAttributeDefaultProject(httpSessionMock, userMock, accountMock);

        Mockito.verify(httpSessionMock).setAttribute("defaultprojectid", PROJECT_UUID);
    }
}