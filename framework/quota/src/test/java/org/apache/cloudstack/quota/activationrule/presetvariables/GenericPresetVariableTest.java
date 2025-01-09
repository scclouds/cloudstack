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

package org.apache.cloudstack.quota.activationrule.presetvariables;

import com.google.gson.Gson;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;

@RunWith(MockitoJUnitRunner.class)
public class GenericPresetVariableTest {

    @Test
    public void setIdTestAddFieldIdToCollection() {
        GenericPresetVariable variable = new GenericPresetVariable();
        variable.setId("test");
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("id"));
    }

    @Test
    public void setNameTestAddFieldNameToCollection() {
        GenericPresetVariable variable = new GenericPresetVariable();
        variable.setName("test");
        Assert.assertTrue(variable.fieldNamesToIncludeInToString.contains("name"));
    }

    @Test
    public void toStringTestSetAllFieldsAndReturnAJson() {
        GenericPresetVariable variable = new GenericPresetVariable();
        variable.setId("test id");
        variable.setName("test name");

        String expected = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(variable, "id", "name");
        String result = variable.toString();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void toStringTestSetSomeFieldsAndReturnAJson() {
        GenericPresetVariable variable = new GenericPresetVariable();
        variable.setId("test id");

        String expected = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(variable, "id");
        String result = variable.toString();

        Assert.assertEquals(expected, result);

        variable = new GenericPresetVariable();
        variable.setName("test name");

        expected = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(variable, "name");
        result = variable.toString();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void includeFieldInToStringIfNotNullAndNonTransientTestDoNothingWhenFieldIsNull() throws IllegalAccessException, NoSuchFieldException {
        GenericPresetVariable gpv = new Gson().fromJson("{}", GenericPresetVariable.class);
        Field field = gpv.getClass().getDeclaredField("id");

        gpv.includeFieldInToStringIfNotNullAndNonTransient(field);

        Assert.assertTrue(gpv.fieldNamesToIncludeInToString.isEmpty());
    }

    @Test
    public void includeFieldInToStringIfNotNullAndNonTransientTestIncludeFieldInToStringWhenFieldIsNotNull() throws IllegalAccessException, NoSuchFieldException {
        GenericPresetVariable gpv = new Gson().fromJson("{id: \"test\"}", GenericPresetVariable.class);
        Field field = gpv.getClass().getDeclaredField("id");

        gpv.includeFieldInToStringIfNotNullAndNonTransient(field);

        Assert.assertArrayEquals(new String[]{"id"}, gpv.fieldNamesToIncludeInToString.toArray());
    }

    @Test
    public void includeAllNotNullAndNonTransientFieldsInToStringTestIncludeAllFields() throws IllegalAccessException {
        Host hostSpy = Mockito.spy(Host.class);
        int qtFields = hostSpy.getClass().getDeclaredFields().length + hostSpy.getClass().getSuperclass().getDeclaredFields().length;

        hostSpy.includeAllNotNullAndNonTransientFieldsInToString();

        Mockito.verify(hostSpy, Mockito.times(qtFields)).includeFieldInToStringIfNotNullAndNonTransient(Mockito.any());
    }
}
