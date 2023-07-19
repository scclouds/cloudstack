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
package org.apache.cloudstack.storage.heuristics;

import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.test.TestAppender;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.secstorage.HeuristicVO;
import org.apache.cloudstack.secstorage.dao.SecondaryStorageHeuristicDao;
import org.apache.cloudstack.secstorage.heuristics.HeuristicPurpose;
import org.apache.cloudstack.storage.heuristics.presetvariables.PresetVariables;
import org.apache.cloudstack.utils.jsinterpreter.JsInterpreter;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.regex.Pattern;

@RunWith(PowerMockRunner.class)
public class HeuristicRuleHelperTest {

    @Mock
    SecondaryStorageHeuristicDao secondaryStorageHeuristicDaoMock;

    @Mock
    HeuristicVO heuristicVOMock;

    @Mock
    VMTemplateVO vmTemplateVOMock;

    @Mock
    SnapshotInfo snapshotInfoMock;

    @Mock
    VolumeVO volumeVOMock;

    @Mock
    DataStoreManager dataStoreManagerMock;

    @Mock
    DataStore dataStoreMock;

    @Spy
    @InjectMocks
    HeuristicRuleHelper heuristicRuleHelperSpy = new HeuristicRuleHelper();

    @Test
    public void getImageStoreIfThereIsHeuristicRuleTestZoneDoesNotHaveHeuristicRuleShouldReturnNull() {
        Long zoneId = 1L;

        Mockito.when(secondaryStorageHeuristicDaoMock.findByZoneIdAndPurpose(Mockito.anyLong(), Mockito.any(HeuristicPurpose.class))).thenReturn(null);

        TestAppender.TestAppenderBuilder appenderBuilder = new TestAppender.TestAppenderBuilder();
        appenderBuilder.addExpectedPattern(Level.DEBUG, Pattern.quote(String.format("No heuristic rules found for zone with ID [%s] and heuristic purpose [%s]. Returning null.",
                zoneId, HeuristicPurpose.TEMPLATE)));
        TestAppender testLogAppender = appenderBuilder.build();
        TestAppender.safeAddAppender(HeuristicRuleHelper.LOGGER, testLogAppender);

        DataStore result = heuristicRuleHelperSpy.getImageStoreIfThereIsHeuristicRule(zoneId, HeuristicPurpose.TEMPLATE, null);

        testLogAppender.assertMessagesLogged();
        Assert.assertNull(result);
    }

    @Test
    public void getImageStoreIfThereIsHeuristicRuleTestZoneHasHeuristicRuleShouldCallInterpretHeuristicRule() {
        Long zoneId = 1L;

        Mockito.when(secondaryStorageHeuristicDaoMock.findByZoneIdAndPurpose(Mockito.anyLong(), Mockito.any(HeuristicPurpose.class))).thenReturn(heuristicVOMock);
        Mockito.when(heuristicVOMock.getHeuristicRule()).thenReturn("rule");
        Mockito.doReturn(null).when(heuristicRuleHelperSpy).interpretHeuristicRule(Mockito.anyString(), Mockito.any(HeuristicPurpose.class), Mockito.isNull(),
                Mockito.anyLong());

        TestAppender.TestAppenderBuilder appenderBuilder = new TestAppender.TestAppenderBuilder();
        appenderBuilder.addExpectedPattern(Level.DEBUG, Pattern.quote(String.format("Found the heuristic rule %s to apply for zone with ID [%s].", heuristicVOMock, zoneId)));
        TestAppender testLogAppender = appenderBuilder.build();
        TestAppender.safeAddAppender(HeuristicRuleHelper.LOGGER, testLogAppender);

        DataStore result = heuristicRuleHelperSpy.getImageStoreIfThereIsHeuristicRule(zoneId, HeuristicPurpose.TEMPLATE, null);

        testLogAppender.assertMessagesLogged();
        Assert.assertNull(result);
    }

    @Test
    public void buildPresetVariablesTestWithTemplateHeuristicPurposeShouldSetTemplateAndSecondaryStoragePresetVariables() {
        Mockito.doNothing().when(heuristicRuleHelperSpy).injectPresetVariables(Mockito.isNull(), Mockito.any(PresetVariables.class));
        Mockito.doReturn(null).when(heuristicRuleHelperSpy).setSecondaryStoragesVariable(Mockito.anyLong());

        heuristicRuleHelperSpy.buildPresetVariables(null, HeuristicPurpose.TEMPLATE, 1L, vmTemplateVOMock);

        Mockito.verify(heuristicRuleHelperSpy, Mockito.times(1)).setTemplatePresetVariable(Mockito.any(VMTemplateVO.class));
        Mockito.verify(heuristicRuleHelperSpy, Mockito.times(1)).setSecondaryStoragesVariable(Mockito.anyLong());
        Mockito.verify(heuristicRuleHelperSpy, Mockito.times(1)).injectPresetVariables(Mockito.isNull(), Mockito.any(PresetVariables.class));
    }

    @Test
    public void buildPresetVariablesTestWithIsoHeuristicPurposeShouldSetTemplateAndSecondaryStoragePresetVariables() {
        Mockito.doNothing().when(heuristicRuleHelperSpy).injectPresetVariables(Mockito.isNull(), Mockito.any(PresetVariables.class));
        Mockito.doReturn(null).when(heuristicRuleHelperSpy).setSecondaryStoragesVariable(Mockito.anyLong());

        heuristicRuleHelperSpy.buildPresetVariables(null, HeuristicPurpose.ISO, 1L, vmTemplateVOMock);

        Mockito.verify(heuristicRuleHelperSpy, Mockito.times(1)).setTemplatePresetVariable(Mockito.any(VMTemplateVO.class));
        Mockito.verify(heuristicRuleHelperSpy, Mockito.times(1)).setSecondaryStoragesVariable(Mockito.anyLong());
        Mockito.verify(heuristicRuleHelperSpy, Mockito.times(1)).injectPresetVariables(Mockito.isNull(), Mockito.any(PresetVariables.class));
    }

    @Test
    public void buildPresetVariablesTestWithSnapshotHeuristicPurposeShouldSetSnapshotAndSecondaryStoragePresetVariables() {
        Mockito.doNothing().when(heuristicRuleHelperSpy).injectPresetVariables(Mockito.isNull(), Mockito.any(PresetVariables.class));
        Mockito.doReturn(null).when(heuristicRuleHelperSpy).setSecondaryStoragesVariable(Mockito.anyLong());

        heuristicRuleHelperSpy.buildPresetVariables(null, HeuristicPurpose.SNAPSHOT, 1L, snapshotInfoMock);

        Mockito.verify(heuristicRuleHelperSpy, Mockito.times(1)).setSnapshotPresetVariable(Mockito.any(SnapshotInfo.class));
        Mockito.verify(heuristicRuleHelperSpy, Mockito.times(1)).setSecondaryStoragesVariable(Mockito.anyLong());
        Mockito.verify(heuristicRuleHelperSpy, Mockito.times(1)).injectPresetVariables(Mockito.isNull(), Mockito.any(PresetVariables.class));
    }

    @Test
    public void buildPresetVariablesTestWithSnapshotHeuristicPurposeShouldSetVolumeAndSecondaryStoragePresetVariables() {
        Mockito.doNothing().when(heuristicRuleHelperSpy).injectPresetVariables(Mockito.isNull(), Mockito.any(PresetVariables.class));
        Mockito.doReturn(null).when(heuristicRuleHelperSpy).setSecondaryStoragesVariable(Mockito.anyLong());

        heuristicRuleHelperSpy.buildPresetVariables(null, HeuristicPurpose.VOLUME, 1L, volumeVOMock);

        Mockito.verify(heuristicRuleHelperSpy, Mockito.times(1)).setVolumePresetVariable(Mockito.any(VolumeVO.class));
        Mockito.verify(heuristicRuleHelperSpy, Mockito.times(1)).setSecondaryStoragesVariable(Mockito.anyLong());
        Mockito.verify(heuristicRuleHelperSpy, Mockito.times(1)).injectPresetVariables(Mockito.isNull(), Mockito.any(PresetVariables.class));
    }

    @Test
    public void interpretHeuristicRuleTestHeuristicRuleDoesNotReturnAStringShouldThrowCloudRuntimeException() {
        String heuristicRule = "1";

        Mockito.doNothing().when(heuristicRuleHelperSpy).buildPresetVariables(Mockito.any(JsInterpreter.class), Mockito.any(HeuristicPurpose.class), Mockito.anyLong(),
                Mockito.any());

        String expectedMessage = String.format("Error while interpreting heuristic rule [%s], the rule did not return a String.", heuristicRule);
        CloudRuntimeException assertThrows = Assert.assertThrows(CloudRuntimeException.class,
                () -> heuristicRuleHelperSpy.interpretHeuristicRule(heuristicRule, HeuristicPurpose.TEMPLATE, volumeVOMock, 1L));
        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void interpretHeuristicRuleTestHeuristicRuleReturnAStringWithInvalidUuidShouldThrowCloudRuntimeException() {
        String heuristicRule = "'uuid'";

        Mockito.doNothing().when(heuristicRuleHelperSpy).buildPresetVariables(Mockito.any(JsInterpreter.class), Mockito.any(HeuristicPurpose.class), Mockito.anyLong(),
                Mockito.any());
        Mockito.doReturn(null).when(dataStoreManagerMock).getImageStoreByUuid(Mockito.anyString());

        String expectedMessage = String.format("Unable to find a secondary storage with the UUID [%s] returned by the heuristic rule [%s]. Check if the rule is " +
                "returning a valid UUID.", "uuid", heuristicRule);
        CloudRuntimeException assertThrows = Assert.assertThrows(CloudRuntimeException.class,
                () -> heuristicRuleHelperSpy.interpretHeuristicRule(heuristicRule, HeuristicPurpose.TEMPLATE, volumeVOMock, 1L));
        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void interpretHeuristicRuleTestHeuristicRuleReturnAStringWithAValidUuidShouldReturnAImageStore() {
        String heuristicRule = "'uuid'";

        Mockito.doNothing().when(heuristicRuleHelperSpy).buildPresetVariables(Mockito.any(JsInterpreter.class), Mockito.any(HeuristicPurpose.class), Mockito.anyLong(),
                Mockito.any());
        Mockito.doReturn(dataStoreMock).when(dataStoreManagerMock).getImageStoreByUuid(Mockito.anyString());

        DataStore result = heuristicRuleHelperSpy.interpretHeuristicRule(heuristicRule, HeuristicPurpose.TEMPLATE, volumeVOMock, 1L);

        Assert.assertNotNull(result);
    }
}