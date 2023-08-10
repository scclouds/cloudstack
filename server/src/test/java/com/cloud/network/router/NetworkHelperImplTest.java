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
package com.cloud.network.router;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.network.Network;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.NicDao;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
public class NetworkHelperImplTest {

    private static final long HOST_ID = 10L;

    @Mock
    protected AgentManager agentManager;

    @Mock
    private NicDao nicDaoMock;

    private NicProfile nicProfile = new NicProfile();

    @Mock
    private NicVO nicVoMock;

    @Mock
    private Network networkMock;

    @Spy
    @InjectMocks
    protected NetworkHelperImpl networkHelperSpy = new NetworkHelperImpl();

    @Test(expected=ResourceUnavailableException.class)
    public void testSendCommandsToRouterWrongRouterVersion()
            throws AgentUnavailableException, OperationTimedoutException, ResourceUnavailableException {
        // Prepare
        NetworkHelperImpl nwHelperUT = spy(this.networkHelperSpy);
        VirtualRouter vr = mock(VirtualRouter.class);
        doReturn(false).when(nwHelperUT).checkRouterVersion(vr);

        // Execute
        nwHelperUT.sendCommandsToRouter(vr, null);

        // Assert
        verify(this.agentManager, times(0)).send((Long) Matchers.anyObject(), (Command) Matchers.anyObject());
    }

    @Test
    public void testSendCommandsToRouter()
            throws AgentUnavailableException, OperationTimedoutException, ResourceUnavailableException {
        // Prepare
        NetworkHelperImpl nwHelperUT = spy(this.networkHelperSpy);
        VirtualRouter vr = mock(VirtualRouter.class);
        when(vr.getHostId()).thenReturn(HOST_ID);
        doReturn(true).when(nwHelperUT).checkRouterVersion(vr);

        Commands commands = mock(Commands.class);
        when(commands.size()).thenReturn(3);
        Answer answer1 = mock(Answer.class);
        Answer answer2 = mock(Answer.class);
        Answer answer3 = mock(Answer.class);
        // In the second iteration it should match and return, without invoking the third
        Answer[] answers = {answer1, answer2, answer3};
        when(answer1.getResult()).thenReturn(true);
        when(answer2.getResult()).thenReturn(false);
        lenient().when(answer3.getResult()).thenReturn(false);
        when(this.agentManager.send(HOST_ID, commands)).thenReturn(answers);

        // Execute
        final boolean result = nwHelperUT.sendCommandsToRouter(vr, commands);

        // Assert
        verify(this.agentManager, times(1)).send(HOST_ID, commands);
        verify(answer1, times(1)).getResult();
        verify(answer2, times(1)).getResult();
        verify(answer3, times(0)).getResult();
        assertFalse(result);
    }

    /**
     * The only way result can be true is if each and every command receive a true result
     *
     * @throws AgentUnavailableException
     * @throws OperationTimedoutException
     */
    @Test
    public void testSendCommandsToRouterWithTrueResult()
            throws AgentUnavailableException, OperationTimedoutException, ResourceUnavailableException {
        // Prepare
        NetworkHelperImpl nwHelperUT = spy(this.networkHelperSpy);
        VirtualRouter vr = mock(VirtualRouter.class);
        when(vr.getHostId()).thenReturn(HOST_ID);
        doReturn(true).when(nwHelperUT).checkRouterVersion(vr);

        Commands commands = mock(Commands.class);
        when(commands.size()).thenReturn(3);
        Answer answer1 = mock(Answer.class);
        Answer answer2 = mock(Answer.class);
        Answer answer3 = mock(Answer.class);
        // In the second iteration it should match and return, without invoking the third
        Answer[] answers = {answer1, answer2, answer3};
        when(answer1.getResult()).thenReturn(true);
        when(answer2.getResult()).thenReturn(true);
        when(answer3.getResult()).thenReturn(true);
        when(this.agentManager.send(HOST_ID, commands)).thenReturn(answers);

        // Execute
        final boolean result = nwHelperUT.sendCommandsToRouter(vr, commands);

        // Assert
        verify(this.agentManager, times(1)).send(HOST_ID, commands);
        verify(answer1, times(1)).getResult();
        verify(answer2, times(1)).getResult();
        verify(answer3, times(1)).getResult();
        assertTrue(result);
    }

    /**
     * If the number of answers is different to the number of commands the result is false
     *
     * @throws AgentUnavailableException
     * @throws OperationTimedoutException
     */
    @Test
    public void testSendCommandsToRouterWithNoAnswers()
            throws AgentUnavailableException, OperationTimedoutException, ResourceUnavailableException {
        // Prepare
        NetworkHelperImpl nwHelperUT = spy(this.networkHelperSpy);
        VirtualRouter vr = mock(VirtualRouter.class);
        when(vr.getHostId()).thenReturn(HOST_ID);
        doReturn(true).when(nwHelperUT).checkRouterVersion(vr);

        Commands commands = mock(Commands.class);
        when(commands.size()).thenReturn(3);
        Answer answer1 = mock(Answer.class);
        Answer answer2 = mock(Answer.class);
        // In the second iteration it should match and return, without invoking the third
        Answer[] answers = {answer1, answer2};
        when(this.agentManager.send(HOST_ID, commands)).thenReturn(answers);

        // Execute
        final boolean result = nwHelperUT.sendCommandsToRouter(vr, commands);

        // Assert
        verify(this.agentManager, times(1)).send(HOST_ID, commands);
        verify(answer1, times(0)).getResult();
        assertFalse(result);
    }

    @Test
    @PrepareForTest(NetworkOrchestrationService.class)
    public void setPublicNicMacAddressSameAsPeerNicTestConfigurationIsFalseExpectDoNothing() {
        PowerMockito.mockStatic(NetworkOrchestrationService.class);
        PowerMockito.when(NetworkOrchestrationService.getUseSameMacAddressForPublicNicOfVirtualRoutersOnSameNetworkValue()).thenReturn(false);

        NicProfile nicProfileMock = Mockito.mock(NicProfile.class);
        Network networkMock = Mockito.mock(Network.class);
        networkHelperSpy.setPublicNicMacAddressSameAsPeerNic(nicProfileMock, networkMock);

        Mockito.verify(nicProfileMock, Mockito.never()).setMacAddress(Mockito.anyString());
    }

    @Test
    @PrepareForTest(NetworkOrchestrationService.class)
    public void setPublicNicMacAddressSameAsPeerNicTestConfigurationIsTrueAndThereIsNoPeerNicExpectDoNothing() {
        PowerMockito.mockStatic(NetworkOrchestrationService.class);
        PowerMockito.when(NetworkOrchestrationService.getUseSameMacAddressForPublicNicOfVirtualRoutersOnSameNetworkValue()).thenReturn(true);

        String expectedValue = "original";
        nicProfile.setIPv4Address("10.0.0.1");
        nicProfile.setMacAddress(expectedValue);
        Mockito.doReturn(null).when(nicDaoMock).findByIp4AddressAndNetworkId(Mockito.anyString(), Mockito.anyLong());
        networkHelperSpy.setPublicNicMacAddressSameAsPeerNic(nicProfile, networkMock);

        Assert.assertEquals(expectedValue, nicProfile.getMacAddress());
    }

    @Test
    @PrepareForTest(NetworkOrchestrationService.class)
    public void setPublicNicMacAddressSameAsPeerNicTestConfigurationIsTrueAndThereIsAPeerNicExpectSetMacAddress() {
        PowerMockito.mockStatic(NetworkOrchestrationService.class);
        PowerMockito.when(NetworkOrchestrationService.getUseSameMacAddressForPublicNicOfVirtualRoutersOnSameNetworkValue()).thenReturn(true);

        String expectedValue = "macaddress";
        nicProfile.setIPv4Address("10.0.0.1");
        nicProfile.setMacAddress("original");

        Mockito.doReturn(nicVoMock).when(nicDaoMock).findByIp4AddressAndNetworkId(Mockito.anyString(), Mockito.anyLong());
        Mockito.doReturn(expectedValue).when(nicVoMock).getMacAddress();
        networkHelperSpy.setPublicNicMacAddressSameAsPeerNic(nicProfile, networkMock);

        Assert.assertEquals(expectedValue, nicProfile.getMacAddress());
    }
}
