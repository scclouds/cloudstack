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
package com.cloud.projects;

import java.util.ArrayList;
import java.util.List;

import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.webhook.WebhookHelper;
import org.apache.commons.collections.CollectionUtils;
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
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import com.cloud.projects.dao.ProjectDao;
import com.cloud.utils.component.ComponentContext;


@RunWith(MockitoJUnitRunner.class)
public class ProjectManagerImplTest {

    @Mock
    private Account accountMock;
    @Mock
    private ProjectVO projectMock;
    @Mock
    private AccountManager accountManagerMock;
    @Mock
    private DomainManager domainManagerMock;
    @Mock
    private ProjectAccountDao projectAccountDaoMock;

    @Spy
    @InjectMocks
    ProjectManagerImpl projectManager;

    @Mock
    ProjectDao projectDao;

    List<ProjectVO> updateProjects;

    private static final long ACCOUNT_ID = 1L;
    private static final long USER_ID = 2L;
    private static final long PROJECT_ID = 3L;
    private static final long ACCOUNT_DOMAIN_ID = 4L;
    private static final long PROJECT_DOMAIN_ID = 5L;

    @Before
    public void setUp() throws Exception {
        updateProjects = new ArrayList<>();
        Mockito.when(projectDao.update(Mockito.anyLong(), Mockito.any(ProjectVO.class))).thenAnswer((Answer<Boolean>) invocation -> {
            ProjectVO project = (ProjectVO)invocation.getArguments()[1];
            updateProjects.add(project);
            return true;
        });

        Mockito.doReturn(ACCOUNT_DOMAIN_ID).when(accountMock).getDomainId();
        Mockito.doReturn(PROJECT_DOMAIN_ID).when(projectMock).getDomainId();
    }

    private void runUpdateProjectNameAndDisplayTextTest(boolean nonNullName, boolean nonNullDisplayText) {
        ProjectVO projectVO = new ProjectVO();
        String newName = nonNullName ? "NewName" : null;
        String newDisplayText = nonNullDisplayText ? "NewDisplayText" : null;
        projectManager.updateProjectNameAndDisplayText(projectVO, newName, newDisplayText);
        if (!nonNullName && !nonNullDisplayText) {
            Assert.assertTrue(updateProjects.isEmpty());
        } else {
            Assert.assertFalse(updateProjects.isEmpty());
            Assert.assertEquals(1, updateProjects.size());
            ProjectVO updatedProject = updateProjects.get(0);
            Assert.assertNotNull(updatedProject);
            if (nonNullName) {
                Assert.assertEquals(newName, updatedProject.getName());
            }
            if (nonNullDisplayText) {
                Assert.assertEquals(newDisplayText, updatedProject.getDisplayText());
            }
        }
    }

    @Test
    public void testUpdateProjectNameAndDisplayTextNoUpdate() {
        runUpdateProjectNameAndDisplayTextTest(false, false);
    }

    @Test
    public void testUpdateProjectNameAndDisplayTextUpdateName() {
        runUpdateProjectNameAndDisplayTextTest(true, false);
    }

    @Test
    public void testUpdateProjectNameAndDisplayTextUpdateDisplayText() {
        runUpdateProjectNameAndDisplayTextTest(false, true);
    }

    @Test
    public void testUpdateProjectNameAndDisplayTextUpdateNameDisplayText() {
        runUpdateProjectNameAndDisplayTextTest(true, true);
    }

    @Test
    public void testDeleteWebhooksForAccount() {
        try (MockedStatic<ComponentContext> mockedComponentContext = Mockito.mockStatic(ComponentContext.class)) {
            WebhookHelper webhookHelper = Mockito.mock(WebhookHelper.class);
            List<ControlledEntity> webhooks = List.of(Mockito.mock(ControlledEntity.class),
                    Mockito.mock(ControlledEntity.class));
            Mockito.doReturn(webhooks).when(webhookHelper).listWebhooksByAccount(Mockito.anyLong());
            mockedComponentContext.when(() -> ComponentContext.getDelegateComponentOfType(WebhookHelper.class))
                    .thenReturn(webhookHelper);
            Project project = Mockito.mock(Project.class);
            Mockito.when(project.getProjectAccountId()).thenReturn(1L);
            List<? extends ControlledEntity> result = projectManager.listWebhooksForProject(project);
            Assert.assertEquals(2, result.size());
        }
    }

    @Test
    public void testDeleteWebhooksForAccountNoBean() {
        try (MockedStatic<ComponentContext> mockedComponentContext = Mockito.mockStatic(ComponentContext.class)) {
            mockedComponentContext.when(() -> ComponentContext.getDelegateComponentOfType(WebhookHelper.class))
                    .thenThrow(NoSuchBeanDefinitionException.class);
            List<? extends ControlledEntity> result =
                    projectManager.listWebhooksForProject(Mockito.mock(Project.class));
            Assert.assertTrue(CollectionUtils.isEmpty(result));
        }
    }

    private void setAccessProjectTests() {
        Mockito.lenient().doReturn(accountMock).when(accountManagerMock).getActiveAccountById(ACCOUNT_ID);
        Mockito.lenient().doReturn(projectMock).when(projectManager).getProject(PROJECT_ID);

        Mockito.lenient().doReturn(false).when(accountManagerMock).isRootAdmin(ACCOUNT_ID);
        Mockito.lenient().doReturn(false).when(accountManagerMock).isDomainAdmin(ACCOUNT_ID);
        Mockito.lenient().doReturn(false).when(domainManagerMock).isChildDomain(ACCOUNT_DOMAIN_ID, PROJECT_DOMAIN_ID);

        Mockito.lenient().doReturn(null).when(projectAccountDaoMock).findByProjectIdAccountIdNullUserId(PROJECT_ID, ACCOUNT_ID);
    }

    @Test
    public void canAccountAccessProjectTestAccountNotExists() {
        setAccessProjectTests();
        Mockito.doReturn(null).when(accountManagerMock).getActiveAccountById(ACCOUNT_ID);

        Assert.assertFalse(projectManager.canAccountAccessProject(ACCOUNT_ID, PROJECT_ID));
    }
    @Test
    public void canAccountAccessProjectTestAccountIsRootAdmin() {
        setAccessProjectTests();
        Mockito.doReturn(true).when(accountManagerMock).isRootAdmin(ACCOUNT_ID);

        Assert.assertTrue(projectManager.canAccountAccessProject(ACCOUNT_ID, PROJECT_ID));
    }
    @Test
    public void canAccountAccessProjectTestAccountIsDomainAdminProjectInChildDomain() {
        setAccessProjectTests();
        Mockito.doReturn(true).when(accountManagerMock).isDomainAdmin(ACCOUNT_ID);
        Mockito.doReturn(true).when(domainManagerMock).isChildDomain(ACCOUNT_DOMAIN_ID, PROJECT_DOMAIN_ID);

        Assert.assertTrue(projectManager.canAccountAccessProject(ACCOUNT_ID, PROJECT_ID));
    }
    @Test
    public void canAccountAccessProjectTestAccountIsDomainAdminProjectNotInChildDomain() {
        setAccessProjectTests();
        Mockito.doReturn(true).when(accountManagerMock).isDomainAdmin(ACCOUNT_ID);

        Assert.assertFalse(projectManager.canAccountAccessProject(ACCOUNT_ID, PROJECT_ID));
    }
    @Test
    public void canAccountAccessProjectTestAccountIsNotDomainAdminProjectInChildDomain() {
        setAccessProjectTests();
        Mockito.lenient().doReturn(true).when(domainManagerMock).isChildDomain(ACCOUNT_DOMAIN_ID, PROJECT_DOMAIN_ID);

        Assert.assertFalse(projectManager.canAccountAccessProject(ACCOUNT_ID, PROJECT_ID));
    }
    @Test
    public void canAccountAccessProjectTestAccountAddedToProject() {
        setAccessProjectTests();
        Mockito.doReturn(Mockito.mock(ProjectAccountVO.class)).when(projectAccountDaoMock).findByProjectIdAccountIdNullUserId(PROJECT_ID, ACCOUNT_ID);

        Assert.assertTrue(projectManager.canAccountAccessProject(ACCOUNT_ID, PROJECT_ID));
    }
    @Test
    public void canAccountAccessProjectTestAccountNotAddedToProject() {
        setAccessProjectTests();

        Assert.assertFalse(projectManager.canAccountAccessProject(ACCOUNT_ID, PROJECT_ID));
    }

    @Test
    public void canUserAccessProjectTestAccountCanAccessProject() {
        Mockito.doReturn(true).when(projectManager).canAccountAccessProject(ACCOUNT_ID, PROJECT_ID);

        Assert.assertTrue(projectManager.canUserAccessProject(USER_ID, ACCOUNT_ID, PROJECT_ID));
    }

    @Test
    public void canUserAccessProjectTestAccountCantAccessProjectAndUserAddedToProject() {
        Mockito.doReturn(false).when(projectManager).canAccountAccessProject(ACCOUNT_ID, PROJECT_ID);
        Mockito.doReturn(Mockito.mock(ProjectAccountVO.class)).when(projectAccountDaoMock).findByProjectIdUserId(PROJECT_ID, ACCOUNT_ID, USER_ID);

        Assert.assertTrue(projectManager.canUserAccessProject(USER_ID, ACCOUNT_ID, PROJECT_ID));
    }

    @Test
    public void canUserAccessProjectTestAccountCantAccessProjectAndUserNotAddedToProject() {
        Mockito.doReturn(false).when(projectManager).canAccountAccessProject(ACCOUNT_ID, PROJECT_ID);
        Mockito.doReturn(null).when(projectAccountDaoMock).findByProjectIdUserId(PROJECT_ID, ACCOUNT_ID, USER_ID);

        Assert.assertFalse(projectManager.canUserAccessProject(USER_ID, ACCOUNT_ID, PROJECT_ID));
    }
}
