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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

public class GenericPresetVariable {
    private transient Logger logger = Logger.getLogger(this.getClass().getName());

    @PresetVariableDefinition(description = "ID of the resource.")
    private String id;
    @PresetVariableDefinition(description = "Name of the resource.")
    private String name;
    protected transient Set<String> fieldNamesToIncludeInToString = new HashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        fieldNamesToIncludeInToString.add("id");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        fieldNamesToIncludeInToString.add("name");
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilderUtils.reflectOnlySelectedFields(this, fieldNamesToIncludeInToString.toArray(new String[0]));
    }

    /**
     * Sets all not null and non-transient fields in fieldNamesToIncludeInToString.
     */
    public void includeAllNotNullAndNonTransientFieldsInToString() throws IllegalAccessException {
        Field[] fields = this.getClass().getDeclaredFields();
        fields = ArrayUtils.addAll(fields, this.getClass().getSuperclass().getDeclaredFields());

        List<String> allFieldsName = Arrays.stream(fields).map(Field::getName).collect(Collectors.toList());

        logger.trace(String.format("Validating and including fields %s into [%s] toString.", allFieldsName, this.getClass().getName()));
        for (Field field : fields) {
            includeFieldInToStringIfNotNullAndNonTransient(field);
        }

        logger.trace(String.format("The following fields were included in [%s] toString: %s.", this.getClass().getName(), fieldNamesToIncludeInToString));
    }

    protected void includeFieldInToStringIfNotNullAndNonTransient(Field field) throws IllegalAccessException {
        String fieldName = field.getName();
        String thisClassName = this.getClass().getName();
        logger.trace(String.format("Validating if field [%s] is not transient and not null to include in [%s] toString.", fieldName, thisClassName));

        if (Modifier.isTransient(field.getModifiers())) {
            logger.trace(String.format("Field [%s] is transient; not including in [%s] toString.", fieldName, thisClassName));
            return;
        }

        field.setAccessible(true);
        Object fieldValue = field.get(this);
        field.setAccessible(false);

        if (fieldValue == null) {
            logger.trace(String.format("Field [%s] is null; not including in [%s] toString.", fieldName, thisClassName));
            return;
        }

        logger.trace(String.format("Including field [%s] in [%s] toString.", fieldName, thisClassName));
        this.fieldNamesToIncludeInToString.add(fieldName);
    }
}
