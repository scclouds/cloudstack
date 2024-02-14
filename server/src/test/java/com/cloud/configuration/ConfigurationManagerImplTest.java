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
package com.cloud.configuration;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.NetworkModel;
import com.cloud.offering.DiskOffering;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.net.NetUtils;
import org.apache.cloudstack.api.command.admin.offering.UpdateDiskOfferingCmd;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.resourcedetail.DiskOfferingDetailVO;
import org.apache.cloudstack.resourcedetail.dao.DiskOfferingDetailsDao;
import org.junit.Assert;
import org.junit.Before;
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


@RunWith(PowerMockRunner.class)
@PrepareForTest(NetUtils.class)
public class ConfigurationManagerImplTest {
    @InjectMocks
    ConfigurationManagerImpl configurationManagerImplSpy = Mockito.spy(new ConfigurationManagerImpl());
    @Mock
    SearchCriteria<DiskOfferingDetailVO> searchCriteriaDiskOfferingDetailMock;
    @Mock
    DiskOffering diskOfferingMock;
    @Mock
    Account accountMock;
    @Mock
    User userMock;
    @Mock
    Domain domainMock;
    @Mock
    DataCenterDao zoneDaoMock;
    @Mock
    DomainDao domainDaoMock;
    @Mock
    EntityManager entityManagerMock;
    @Mock
    DiskOfferingDetailsDao diskOfferingDetailsDao;
    @Mock
    ConfigurationDao configurationDaoMock;
    @Mock
    ConfigDepot configDepotMock;
    @Mock
    ConfigurationVO configurationVOMock;
    @Mock
    ConfigKey configKeyMock;
    @Spy
    DiskOfferingVO diskOfferingVOSpy = new DiskOfferingVO();
    @Spy
    UpdateDiskOfferingCmd updateDiskOfferingCmdSpy = new UpdateDiskOfferingCmd();

    Long validId = 1L;
    Long invalidId = 100L;
    List<Long> filteredZoneIds = List.of(1L, 2L, 3L);
    List<Long> existingZoneIds = List.of(1L, 2L, 3L);
    List<Long> filteredDomainIds = List.of(1L, 2L, 3L);
    List<Long> existingDomainIds = List.of(1L, 2L, 3L);
    List<Long> emptyExistingZoneIds = new ArrayList<>();
    List<Long> emptyExistingDomainIds = new ArrayList<>();
    List<Long> emptyFilteredDomainIds = new ArrayList<>();

    @Before
    public void setUp() {
        Mockito.when(configurationVOMock.getScope()).thenReturn(ConfigKey.Scope.Global.name());
        Mockito.when(configurationDaoMock.findByName(Mockito.anyString())).thenReturn(configurationVOMock);
        Mockito.when(configDepotMock.get(Mockito.anyString())).thenReturn(configKeyMock);

        configurationManagerImplSpy.populateConfigValuesForValidationSet();
        configurationManagerImplSpy.weightBasedParametersForValidation();
        configurationManagerImplSpy.overProvisioningFactorsForValidation();
    }

    @Test
    public void validateIfIntValueIsInRangeTestValidValueReturnNull() {
        String testVariable = configurationManagerImplSpy.validateIfIntValueIsInRange("String name", "3", "1-5");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateIfIntValueIsInRangeTestInvalidValueReturnString() {
        String testVariable = configurationManagerImplSpy.validateIfIntValueIsInRange("String name", "9", "1-5");
        Assert.assertNotNull(testVariable);
    }

    @Test
    public void validateIfStringValueIsInRangeTestValidValuesReturnNull() {
        String testVariable = "";
        List<String> methods = List.of("privateip", "hypervisorList", "instanceName", "domainName", "default");
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangePrivateIp(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeHypervisorList(Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeInstanceName(Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeDomainName(Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeOther(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        for (String method : methods) {
            testVariable = configurationManagerImplSpy.validateIfStringValueIsInRange("name", "value", method);
            Assert.assertNull(testVariable);
        }
    }

    @Test
    public void validateIfStringValueIsInRangeTestInvalidValuesReturnString() {
        String testVariable = "";
        List<String> methods = List.of("privateip", "hypervisorList", "instanceName", "domainName", "default");
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangePrivateIp(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangeHypervisorList(Mockito.anyString());
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangeInstanceName(Mockito.anyString());
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangeDomainName(Mockito.anyString());
        Mockito.doReturn("returnMsg").when(configurationManagerImplSpy).validateRangeOther(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        for (String method : methods) {
            testVariable = configurationManagerImplSpy.validateIfStringValueIsInRange("name", "value", method);
            Assert.assertEquals("The provided value is not returnMsg.", testVariable);
        }
    }


    @Test
    public void validateIfStringValueIsInRangeTestMultipleRangesValidValueReturnNull() {
        Mockito.doReturn("returnMsg1").when(configurationManagerImplSpy).validateRangePrivateIp(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(null).when(configurationManagerImplSpy).validateRangeInstanceName(Mockito.anyString());
        Mockito.doReturn("returnMsg2").when(configurationManagerImplSpy).validateRangeOther(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        String testVariable = configurationManagerImplSpy.validateIfStringValueIsInRange("name", "value", "privateip", "instanceName", "default");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateIfStringValueIsInRangeTestMultipleRangesInvalidValueReturnMessages() {
        Mockito.doReturn("returnMsg1").when(configurationManagerImplSpy).validateRangePrivateIp(Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn("returnMsg2").when(configurationManagerImplSpy).validateRangeInstanceName(Mockito.anyString());
        Mockito.doReturn("returnMsg3").when(configurationManagerImplSpy).validateRangeOther(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        String testVariable = configurationManagerImplSpy.validateIfStringValueIsInRange("name", "value", "privateip", "instanceName", "default");
        Assert.assertEquals("The provided value is neither returnMsg1 NOR returnMsg2 NOR returnMsg3.", testVariable);
    }


    @Test
    public void validateRangePrivateIpTestValidValueReturnNull() {
        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.isSiteLocalAddress(Mockito.anyString())).thenReturn(true);
        String testVariable = configurationManagerImplSpy.validateRangePrivateIp("name", "value");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangePrivateIpTestInvalidValueReturnString() {
        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.isSiteLocalAddress(Mockito.anyString())).thenReturn(false);
        String testVariable = configurationManagerImplSpy.validateRangePrivateIp("name", "value");
        Assert.assertEquals("a valid site local IP address", testVariable);
    }

    @Test
    public void validateRangeHypervisorListTestValidValueReturnNull() {
        String testVariable = configurationManagerImplSpy.validateRangeHypervisorList("Ovm3,VirtualBox,KVM,VMware");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeHypervisorListTestInvalidValueReturnString() {
        String testVariable = configurationManagerImplSpy.validateRangeHypervisorList("Ovm3,VirtualBox,KVM,VMware,Any,InvalidHypervisorName");
        Assert.assertEquals("a valid hypervisor type", testVariable);
    }

    @Test
    public void validateRangeInstanceNameTestValidValueReturnNull() {
        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.verifyInstanceName(Mockito.anyString())).thenReturn(true);
        String testVariable = configurationManagerImplSpy.validateRangeInstanceName("ThisStringShouldBeValid");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeInstanceNameTestInvalidValueReturnString() {
        PowerMockito.mockStatic(NetUtils.class);
        PowerMockito.when(NetUtils.verifyInstanceName(Mockito.anyString())).thenReturn(false);
        String testVariable = configurationManagerImplSpy.validateRangeInstanceName("This string should not be valid.");
        Assert.assertEquals("a valid instance name (instance names cannot contain hyphens, spaces or plus signs)", testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueDoesNotStartWithStarAndIsAValidValueReturnNull() {
        String testVariable = configurationManagerImplSpy.validateRangeDomainName("ThisStringShould.Work");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueDoesNotStartWithStarAndIsAValidValueButIsOver238charactersLongReturnString() {
        String testVariable = configurationManagerImplSpy.validateRangeDomainName("ThisStringDoesNotStartWithStarAndIsOverTwoHundredAndForty.CharactersLongWithAtLeast" +
                "OnePeriodEverySixtyFourLetters.ThisShouldCauseAnErrorBecauseItIsTooLong.TheRestOfThisAreRandomlyGeneratedCharacters.gNXhNOBNTNAoMCQqJMzcvFSBwHUhmWHftjfTNUaHR");
        Assert.assertEquals("a valid domain name", testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueDoesNotStartWithStarAndIsNotAValidValueReturnString() {
        String testVariable = configurationManagerImplSpy.validateRangeDomainName("ThisStringDoesNotMatchThePatternFor.DomainNamesSinceItHas1NumberInTheLastPartOfTheString");
        Assert.assertEquals("a valid domain name", testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueStartsWithStarAndIsAValidValueReturnNull() {

        String testVariable = configurationManagerImplSpy.validateRangeDomainName("*.ThisStringStartsWithAStarAndAPeriod.ThisShouldWorkEvenThoughItIsOverTwoHundredAnd" +
                "ThirtyEight.CharactersLong.BecauseTheFirstTwoCharactersAreIgnored.TheRestOfThisStringWasRandomlyGenerated.MgTUerXPlLyMaUpKTjAhxasFYRCfNCXmtWDwqSDOcTjASWlAXS");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueStartsWithStarAndIsAValidValueButIsOver238charactersLongReturnString() {

        String testVariable = configurationManagerImplSpy.validateRangeDomainName("*.ThisStringStartsWithStarAndIsOverTwoHundredAndForty.CharactersLongWithAtLeastOnePeriod" +
                "EverySixtyFourLetters.ThisShouldCauseAnErrorBecauseItIsTooLong.TheRestOfThisAreRandomlyGeneratedCharacters.gNXNOBNTNAoMChQqJMzcvFSBwHUhmWHftjfTNUaHRKVyXm");
        Assert.assertEquals("a valid domain name", testVariable);
    }

    @Test
    public void validateRangeDomainNameTestValueStartsWithStarAndIsNotAValidValueReturnString() {

        String testVariable = configurationManagerImplSpy.validateRangeDomainName("*.ThisStringDoesNotMatchThePatternFor.DomainNamesSinceItHas1NumberInTheLastPartOfTheString");
        Assert.assertEquals("a valid domain name", testVariable);
    }

    @Test
    public void validateRangeOtherTestValidValueReturnNull() {
        String testVariable = configurationManagerImplSpy.validateRangeOther("NameTest1", "SoShouldThis", "ThisShouldWork,ThisShouldAlsoWork,SoShouldThis");
        Assert.assertNull(testVariable);
    }

    @Test
    public void validateRangeOtherTestInvalidValueReturnString() {
        String testVariable = configurationManagerImplSpy.validateRangeOther("NameTest1", "ThisShouldNotWork", "ThisShouldWork,ThisShouldAlsoWork,SoShouldThis");
        Assert.assertNotNull(testVariable);
    }

    @Test
    public void validateDomainTestInvalidIdThrowException() {
        Mockito.doReturn(null).when(domainDaoMock).findById(invalidId);
        Assert.assertThrows(InvalidParameterValueException.class, () -> configurationManagerImplSpy.validateDomain(List.of(invalidId)));
    }

    @Test
    public void validateZoneTestInvalidIdThrowException() {
        Mockito.doReturn(null).when(zoneDaoMock).findById(invalidId);
        Assert.assertThrows(InvalidParameterValueException.class, () -> configurationManagerImplSpy.validateZone(List.of(invalidId)));
    }

    @Test
    public void updateDiskOfferingIfCmdAttributeNotNullTestNotNullValueUpdateOfferingAttribute() {
        Mockito.doReturn("DiskOfferingName").when(updateDiskOfferingCmdSpy).getDiskOfferingName();
        Mockito.doReturn("DisplayText").when(updateDiskOfferingCmdSpy).getDisplayText();
        Mockito.doReturn(1).when(updateDiskOfferingCmdSpy).getSortKey();
        Mockito.doReturn(false).when(updateDiskOfferingCmdSpy).getDisplayOffering();

        configurationManagerImplSpy.updateDiskOfferingIfCmdAttributeNotNull(this.diskOfferingVOSpy, updateDiskOfferingCmdSpy);

        Assert.assertEquals(updateDiskOfferingCmdSpy.getDiskOfferingName(), diskOfferingVOSpy.getName());
        Assert.assertEquals(updateDiskOfferingCmdSpy.getDisplayText(), diskOfferingVOSpy.getDisplayText());
        Assert.assertEquals(updateDiskOfferingCmdSpy.getSortKey(), (Integer) diskOfferingVOSpy.getSortKey());
        Assert.assertEquals(updateDiskOfferingCmdSpy.getDisplayOffering(), diskOfferingVOSpy.getDisplayOffering());
    }

    @Test
    public void updateDiskOfferingIfCmdAttributeNotNullTestNullValueDoesntUpdateOfferingAttribute() {
        diskOfferingVOSpy.setName("Name");
        diskOfferingVOSpy.setDisplayText("DisplayText");
        diskOfferingVOSpy.setSortKey(1);
        diskOfferingVOSpy.setDisplayOffering(true);

        configurationManagerImplSpy.updateDiskOfferingIfCmdAttributeNotNull(diskOfferingVOSpy, updateDiskOfferingCmdSpy);

        Assert.assertNotEquals(updateDiskOfferingCmdSpy.getDiskOfferingName(), diskOfferingVOSpy.getName());
        Assert.assertNotEquals(updateDiskOfferingCmdSpy.getDisplayText(), diskOfferingVOSpy.getDisplayText());
        Assert.assertNotEquals(updateDiskOfferingCmdSpy.getSortKey(), (Integer) diskOfferingVOSpy.getSortKey());
        Assert.assertNotEquals(updateDiskOfferingCmdSpy.getDisplayOffering(), diskOfferingVOSpy.getDisplayOffering());
    }

    @Test
    public void updateDiskOfferingDetailsDomainIdsTestDifferentDomainIdsDiskOfferingDetailsAddDomainIds() {
        List<DiskOfferingDetailVO> detailsVO = new ArrayList<>();
        Long diskOfferingId = validId;

        Mockito.doReturn(1).when(diskOfferingDetailsDao).remove(searchCriteriaDiskOfferingDetailMock);
        configurationManagerImplSpy.updateDiskOfferingDetailsDomainIds(detailsVO, searchCriteriaDiskOfferingDetailMock, diskOfferingId, filteredDomainIds, existingDomainIds);

        for (int i = 0; i < detailsVO.size(); i++) {
            Assert.assertEquals(filteredDomainIds.get(i), (Long) Long.parseLong(detailsVO.get(i).getValue()));
        }
    }

    @Test
    public void updateDiskOfferingDetailsZoneIdsTestDifferentZoneIdsDiskOfferingDetailsAddZoneIds() {
        List<DiskOfferingDetailVO> detailsVO = new ArrayList<>();
        Long diskOfferingId = validId;

        Mockito.doReturn(1).when(diskOfferingDetailsDao).remove(searchCriteriaDiskOfferingDetailMock);
        configurationManagerImplSpy.updateDiskOfferingDetailsDomainIds(detailsVO, searchCriteriaDiskOfferingDetailMock, diskOfferingId, filteredZoneIds, existingZoneIds);

        for (int i = 0; i < detailsVO.size(); i++) {
            Assert.assertEquals(filteredZoneIds.get(i), (Long) Long.parseLong(detailsVO.get(i).getValue()));
        }
    }

    @Test
    public void checkDomainAdminUpdateOfferingRestrictionsTestDifferentZoneIdsThrowException() {
        Assert.assertThrows(InvalidParameterValueException.class,
                () -> configurationManagerImplSpy.checkDomainAdminUpdateOfferingRestrictions(diskOfferingMock, userMock, filteredZoneIds, emptyExistingZoneIds, existingDomainIds, filteredDomainIds));
    }

    @Test
    public void checkDomainAdminUpdateOfferingRestrictionsTestEmptyExistingDomainIdsThrowException() {
        Assert.assertThrows(InvalidParameterValueException.class,
                () -> configurationManagerImplSpy.checkDomainAdminUpdateOfferingRestrictions(diskOfferingMock, userMock, filteredZoneIds, existingZoneIds, emptyExistingDomainIds, filteredDomainIds));
    }

    @Test
    public void checkDomainAdminUpdateOfferingRestrictionsTestEmptyFilteredDomainIdsThrowException() {
        Assert.assertThrows(InvalidParameterValueException.class,
                () -> configurationManagerImplSpy.checkDomainAdminUpdateOfferingRestrictions(diskOfferingMock, userMock, filteredZoneIds, existingZoneIds, existingDomainIds, emptyFilteredDomainIds));
    }

    @Test
    public void getAccountNonChildDomainsTestValidValuesReturnChildDomains() {
        List<Long> nonChildDomains = configurationManagerImplSpy.getAccountNonChildDomains(diskOfferingMock, accountMock, userMock, updateDiskOfferingCmdSpy, existingDomainIds);

        for (int i = 0; i < existingDomainIds.size(); i++) {
            Assert.assertEquals(existingDomainIds.get(i), nonChildDomains.get(i));
        }
    }

    @Test
    public void getAccountNonChildDomainsTestAllDomainsAreChildDomainsReturnEmptyList() {
        for (int i = 0; i < existingDomainIds.size(); i++) {
            Mockito.when(domainDaoMock.isChildDomain(accountMock.getDomainId(), existingDomainIds.get(i))).thenReturn(true);
        }

        List<Long> nonChildDomains = configurationManagerImplSpy.getAccountNonChildDomains(diskOfferingMock, accountMock, userMock, updateDiskOfferingCmdSpy, existingDomainIds);

        Assert.assertTrue(nonChildDomains.isEmpty());
    }

    @Test
    public void getAccountNonChildDomainsTestNotNullCmdAttributeThrowException() {
        Mockito.doReturn("name").when(updateDiskOfferingCmdSpy).getDiskOfferingName();

        Assert.assertThrows(InvalidParameterValueException.class, () -> configurationManagerImplSpy.getAccountNonChildDomains(diskOfferingMock, accountMock, userMock, updateDiskOfferingCmdSpy, existingDomainIds));
    }

    @Test
    public void checkIfDomainIsChildDomainTestNonChildDomainThrowException() {
        Mockito.doReturn(false).when(domainDaoMock).isChildDomain(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(domainMock).when(entityManagerMock).findById(Domain.class, 1L);

        Assert.assertThrows(InvalidParameterValueException.class, () -> configurationManagerImplSpy.checkIfDomainIsChildDomain(diskOfferingMock, accountMock, userMock, filteredDomainIds));
    }

    @Test
    public void validateConfigurationValueTestValidatesValueType() {
        Mockito.when(configKeyMock.type()).thenReturn(Integer.class);
        configurationManagerImplSpy.validateConfigurationValue("validate.my.type", "100", ConfigKey.Scope.Global.name());
        Mockito.verify(configurationManagerImplSpy).validateValueType("100", Integer.class);
    }

    @Test
    public void validateConfigurationValueTestValidatesValueRange() {
        Mockito.when(configKeyMock.type()).thenReturn(Integer.class);
        configurationManagerImplSpy.validateConfigurationValue("validate.my.range", "100", ConfigKey.Scope.Global.name());
        Mockito.verify(configurationManagerImplSpy).validateValueRange("validate.my.range", "100", Integer.class, null);
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsNullAndTypeIsString() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType(null, String.class));
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsNumericAndTypeIsString() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("1", String.class));
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsStringAndTypeIsString() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("test", String.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsNullAndTypeIsBoolean() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType(null, Boolean.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsNumericAndTypeIsBoolean() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("1", Boolean.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsStringAndTypeIsBoolean() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("test", Boolean.class));
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsTrueAndTypeIsBoolean() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("true", Boolean.class));

    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsFalseAndTypeIsBoolean() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("false", Boolean.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsNullAndTypeIsInteger() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType(null, Integer.class));
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsIntegerAndTypeIsInteger() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("1", Integer.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsDecimalAndTypeIsInteger() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("1.1", Integer.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsStringAndTypeIsInteger() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("test", Integer.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsNullAndTypeIsShort() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType(null, Short.class));
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsIntegerAndTypeIsShort() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("1", Short.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsDecimalAndTypeIsShort() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("1.1", Short.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsStringAndTypeIsShort() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("test", Short.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsNullAndTypeIsLong() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType(null, Long.class));
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsIntegerAndTypeIsLong() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("1", Long.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsDecimalAndTypeIsLong() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("1.1", Long.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsStringAndTypeIsLong() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("test", Long.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsNullAndTypeIsFloat() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType(null, Float.class));
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsNumericAndTypeIsFloat() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("1.1", Float.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsStringAndTypeIsFloat() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("test", Float.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsNullAndTypeIsDouble() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType(null, Double.class));
    }

    @Test
    public void validateValueTypeTestReturnsTrueWhenValueIsNumericAndTypeIsDouble() {
        Assert.assertTrue(configurationManagerImplSpy.validateValueType("1.1", Double.class));
    }

    @Test
    public void validateValueTypeTestReturnsFalseWhenValueIsStringAndTypeIsDouble() {
        Assert.assertFalse(configurationManagerImplSpy.validateValueType("test", Double.class));
    }

    @Test
    public void validateValueRangeTestReturnsNullWhenConfigKeyHasNoRange() {
        Assert.assertNull(configurationManagerImplSpy.validateValueRange("configkey.without.range", "0", Integer.class, null));
    }

    @Test
    public void validateValueRangeTestReturnsNullWhenConfigKeyHasRangeAndValueIsValid() {
        Assert.assertNull(configurationManagerImplSpy.validateValueRange(NetworkModel.MACIdentifier.key(), "100", Integer.class, null));
    }

    @Test
    public void validateValueRangeTestReturnsNotNullWhenConfigKeyHasRangeAndValueIsInvalid() {
        Assert.assertNotNull(configurationManagerImplSpy.validateValueRange(NetworkModel.MACIdentifier.key(), "-1", Integer.class, null));
    }

    @Test
    public void shouldValidateConfigRangeTestValueIsNullReturnFalse() {
        boolean result = configurationManagerImplSpy.shouldValidateConfigRange(null, Config.ConsoleProxyUrlDomain);
        Assert.assertFalse(result);
    }

    @Test
    public void shouldValidateConfigRangeTestConfigIsNullReturnFalse() {
        boolean result = configurationManagerImplSpy.shouldValidateConfigRange("test", null);
        Assert.assertFalse(result);
    }

    @Test
    public void shouldValidateConfigRangeTestConfigDoesNotHaveARangeReturnFalse() {
        boolean result = configurationManagerImplSpy.shouldValidateConfigRange("test", Config.ConsoleProxySessionMax);
        Assert.assertFalse(result);
    }

    @Test
    public void shouldValidateConfigRangeTestValueIsNotNullAndConfigHasRangeReturnTrue() {
        boolean result = configurationManagerImplSpy.shouldValidateConfigRange("test", Config.ConsoleProxyUrlDomain);
        Assert.assertTrue(result);
    }
}
