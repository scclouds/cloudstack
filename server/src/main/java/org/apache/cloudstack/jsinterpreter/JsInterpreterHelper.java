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

package org.apache.cloudstack.jsinterpreter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openjdk.nashorn.api.scripting.ScriptUtils;
import org.openjdk.nashorn.internal.runtime.Context;
import org.openjdk.nashorn.internal.runtime.ErrorManager;
import org.openjdk.nashorn.internal.runtime.options.Options;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsInterpreterHelper {
    private final Logger logger = Logger.getLogger(this.getClass());

    /**
     * Returns all variables from the given script.
     *
     * @param script the script to extract the variables.
     * @return A {@link Set<String>} containing all variables in the script.
     */
    public Set<String> getScriptVariables(String script) {
        String parseTree = getScriptAsJsonTree(script);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = null;

        try {
            jsonNode = mapper.readTree(parseTree);
        } catch (JsonProcessingException e) {
            logger.error(String.format("Unable to create the script JSON tree due to: [%s].", e.getMessage()), e);
        }

        logger.trace(String.format("Searching script variables from [%s].", script));
        StringBuilder variable = new StringBuilder();
        Set<String> variables = new HashSet<>();
        iterateOverJsonTree(jsonNode.fields(), variable, variables);

        if (StringUtils.isNotBlank(variable.toString())) {
            logger.trace(String.format("Adding variable [%s] into the variables set.", variable));
            variables.add(variable.toString());
        }

        logger.trace(String.format("Found the following variables from the given script: [%s]", variables));
        return variables;
    }

    private String getScriptAsJsonTree(String script) {
        logger.trace(String.format("Creating JSON Tree for script [%s].", script));
        Options options = new Options("nashorn");
        options.set("anon.functions", true);
        options.set("parse.only", true);
        options.set("scripting", true);

        ErrorManager errors = new ErrorManager();
        Context contextm = new Context(options, errors, Thread.currentThread().getContextClassLoader());
        Context.setGlobal(contextm.createGlobal());

        return ScriptUtils.parse(script, "nashorn", false);
    }

    protected void iterateOverJsonTree(Iterator<Map.Entry<String, JsonNode>> iterator, StringBuilder variable, Set<String> variables) {
        while (iterator.hasNext()) {
            iterateOverJsonTree(iterator.next(), variable, variables);
        }
    }

    protected void iterateOverJsonTree(Map.Entry<String, JsonNode> fields, StringBuilder variable, Set<String> variables) {
        JsonNode node = null;

        if (fields.getValue().isArray()) {
            iterateOverArrayNodes(fields, variable, variables);
        } else {
            node = fields.getValue();
        }

        String fieldName = null;
        fieldName = searchIntoObjectNodes(variable, variables, node);

        if (fieldName == null) {
            String key = fields.getKey();
            if ("name".equals(key) || "property".equals(key)) {
                appendFieldValueToVariable(key, fields.getValue(), variable, variables);
            }
        }
    }

    protected void iterateOverArrayNodes(Map.Entry<String, JsonNode> fields, StringBuilder variable, Set<String> variables) {
        int count = 0;

        while (fields.getValue().get(count) != null) {
            iterateOverJsonTree(fields.getValue().get(count).fields(), variable, variables);
            count++;
        }
    }

    protected String searchIntoObjectNodes(StringBuilder variable, Set<String> variables, JsonNode node) {
        String fieldName = null;

        if (node == null) {
            return null;
        }

        Iterator<String> iterator = node.fieldNames();
        while (iterator.hasNext()) {
            fieldName = iterator.next();
            if ("name".equals(fieldName) || "property".equals(fieldName)) {
                appendFieldValueToVariable(fieldName, node.get(fieldName), variable, variables);
            }

            if (node.get(fieldName).isArray()) {
                iterateOverJsonTree(node.get(fieldName).get(0).fields(), variable, variables);
            } else {
                iterateOverJsonTree(node.get(fieldName).fields(), variable, variables);
            }
        }

        return fieldName;
    }

    protected void appendFieldValueToVariable(String key, JsonNode value, StringBuilder variable, Set<String> variables) {
        if (!"name".equals(key)) {
            logger.trace(String.format("Appending field value [%s] to variable [%s] as the field name is not \"name\".", value.toString(), variable));
            variable.append(".").append(value.toString().replace("\"", ""));
            return;
        }

        logger.trace(String.format("Building new variable [%s] as the field name is \"name\"", value.toString()));
        if (StringUtils.isNotBlank(variable.toString())) {
            logger.trace(String.format("Adding variable [%s] into the variables set.", variable));
            variables.add(variable.toString());
            variable.setLength(0);
        }
        variable.append(value.toString().replace("\"", ""));
    }

    /**
     * Replaces all variables in script that matches the key in {@link Map} for their respective values.
     *
     * @param script the script which the variables will be replaced.
     * @param variablesToReplace a {@link Map} which has the key as the variable to be replaced and the value as the variable to replace.
     * @return A new script with the variables replaced.
     */
    public String replaceScriptVariables(String script, Map<String, String> variablesToReplace) {
        String regex = String.format("\\b(%s)\\b", String.join("|", variablesToReplace.keySet()));
        Matcher matcher = Pattern.compile(regex).matcher(script);

        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, variablesToReplace.get(matcher.group()));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
