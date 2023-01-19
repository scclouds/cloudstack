//
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
//

package com.cloud.utils;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configurator;

public class LogUtils {
    protected static Logger LOGGER = LogManager.getLogger(LogUtils.class);

    private static String configFileLocation = null;

    public static void initLog4j(String log4jConfigFileName) {
        assert (log4jConfigFileName != null);
        File file = PropertiesUtil.findConfigFile(log4jConfigFileName);
        if (file != null) {
            configFileLocation = file.getAbsolutePath();
            Configurator.initialize(null, configFileLocation);
        } else {
            String nameWithoutExtension = log4jConfigFileName.substring(0, log4jConfigFileName.lastIndexOf('.'));
            file = PropertiesUtil.findConfigFile(nameWithoutExtension + ".properties");
            if (file != null) {
                configFileLocation = file.getAbsolutePath();
                Configurator.initialize(null, configFileLocation);
            }
        }
        if (configFileLocation != null) {
            LOGGER.info("log4j configuration found at " + configFileLocation);
        }
    }
    public static Set<String> getLogFileNames() {
        Set<String> fileNames = new HashSet<>();
        org.apache.logging.log4j.core.Logger rootLogger = (org.apache.logging.log4j.core.Logger)LogManager.getRootLogger();
        Map<String, Appender> appenderMap = rootLogger.getAppenders();
        int appenderCount = 0;
        for (Appender appender : appenderMap.values()){
            ++appenderCount;
            if (appender instanceof FileAppender) {
                String fileName =((FileAppender) appender).getFileName();
                fileNames.add(fileName);
                LOGGER.debug("File for {} : {}", appender.getName(), fileName);
            } else if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Not counting {} as a file.", appender.getName());
            }
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Out of {} appenders, {} are log files.", appenderCount, fileNames.size());
        }
        return fileNames;
    }
}
