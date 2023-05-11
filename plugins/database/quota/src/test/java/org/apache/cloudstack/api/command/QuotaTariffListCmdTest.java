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
package org.apache.cloudstack.api.command;

import com.cloud.user.User;
import junit.framework.TestCase;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaTariffResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.cloud.utils.Pair;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class QuotaTariffListCmdTest extends TestCase {
    @Mock
    QuotaResponseBuilder responseBuilder;

    @Mock
    User userMock;

    @Mock
    CallContext callContextMock;

    @Test
    @PrepareForTest (CallContext.class)
    public void testQuotaTariffListCmd() throws NoSuchFieldException, IllegalAccessException {
        QuotaTariffListCmd cmd = new QuotaTariffListCmd();

        Field rbField = QuotaTariffListCmd.class.getDeclaredField("_responseBuilder");
        rbField.setAccessible(true);
        rbField.set(cmd, responseBuilder);

        List<QuotaTariffVO> quotaTariffVOList = new ArrayList<>();
        QuotaTariffVO tariff = new QuotaTariffVO();
        tariff.setEffectiveOn(new Date());
        tariff.setCurrencyValue(new BigDecimal(100));
        tariff.setUsageType(QuotaTypes.VOLUME);

        quotaTariffVOList.add(new QuotaTariffVO());
        Mockito.when(responseBuilder.listQuotaTariffPlans(Mockito.eq(cmd))).thenReturn(new Pair<>(quotaTariffVOList, quotaTariffVOList.size()));
        Mockito.when(responseBuilder.createQuotaTariffResponse(Mockito.any(QuotaTariffVO.class), Mockito.eq(true))).thenReturn(new QuotaTariffResponse());

        PowerMockito.mockStatic(CallContext.class);
        PowerMockito.when(CallContext.current()).thenReturn(callContextMock);

        Mockito.doReturn(userMock).when(callContextMock).getCallingUser();

        Mockito.doReturn(true).when(responseBuilder).isUserAllowedToSeeActivationRules(userMock);

        cmd.execute();
        Mockito.verify(responseBuilder, Mockito.times(1)).createQuotaTariffResponse(Mockito.any(QuotaTariffVO.class), Mockito.eq(true));
    }
}
