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
package com.cloud.upgrade.dao;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.gui.theme.dao.GuiThemeDetailsDao;
import org.apache.cloudstack.gui.theme.dao.GuiThemeDetailsDaoImpl;
import org.apache.cloudstack.gui.themes.GuiThemeDetailsVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Upgrade41804to41805 implements DbUpgrade {
    protected Logger logger = Logger.getLogger(Upgrade41804to41805.class);

    GuiThemeDetailsDao guiThemeDetailsDao = new GuiThemeDetailsDaoImpl();

    private Map<Long, String> guiThemeCommonNames = new LinkedHashMap<>();

    private Map<Long, String> guiThemeDomains = new LinkedHashMap<>();

    private Map<Long, String> guiThemeAccounts = new LinkedHashMap<>();

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[]{"4.18.0.4", "4.18.0.5"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.18.0.5";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41804to41805.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[]{script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        migrateGuiThemeDetailsToNewTable(conn);
    }

    private void migrateGuiThemeDetailsToNewTable(Connection conn) {
        loadGuiThemeDetailsOnMaps(conn);
        persistGuiThemeDetails();
        DbUpgradeUtils.dropTableColumnsIfExist(conn, "gui_themes", List.of("common_names", "domain_uuids", "account_uuids"));
    }

    private void loadGuiThemeDetailsOnMaps(Connection conn) {
        final String selectGuiThemeDetails = "SELECT id, common_names, domain_uuids, account_uuids FROM `cloud`.`gui_themes`;";
        try (PreparedStatement preparedStatement = conn.prepareStatement(selectGuiThemeDetails)) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    long guiThemeId = resultSet.getLong("id");

                    guiThemeCommonNames.put(guiThemeId, resultSet.getString("common_names"));
                    logger.info(String.format("Found the common name(s) [%s] from GUI theme with ID [%s].", guiThemeCommonNames.get(guiThemeId), guiThemeId));

                    guiThemeDomains.put(guiThemeId, resultSet.getString("domain_uuids"));
                    logger.info(String.format("Found the domain UUID(s) [%s] from GUI theme with ID [%s].", guiThemeDomains.get(guiThemeId), guiThemeId));

                    guiThemeAccounts.put(guiThemeId, resultSet.getString("account_uuids"));
                    logger.info(String.format("Found the account UUID(s) [%s] from GUI theme with ID [%s].", guiThemeAccounts.get(guiThemeId), guiThemeId));
                }
            }
        } catch (SQLException e) {
            String message = String.format("Unable to retrieve GUI theme details due to [%s].", e.getMessage());
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    protected void persistGuiThemeDetails() {
        for (Map.Entry<Long, String> commonNameEntry : guiThemeCommonNames.entrySet()) {
            persistDetailsValueIfNotNull(commonNameEntry.getKey(), commonNameEntry.getValue(), "commonName");
        }

        for (Map.Entry<Long, String> domainEntry : guiThemeDomains.entrySet()) {
            persistDetailsValueIfNotNull(domainEntry.getKey(), domainEntry.getValue(), "domain");
        }

        for (Map.Entry<Long, String> accountEntry : guiThemeAccounts.entrySet()) {
            persistDetailsValueIfNotNull(accountEntry.getKey(), accountEntry.getValue(), "account");
        }
    }

    protected void persistDetailsValueIfNotNull(long guiThemeId, String providedParameter, String type) {
        if (providedParameter == null) {
            logger.trace(String.format("GUI theme provided parameter `%s` is null; therefore, it will be ignored.", type));
            return;
        }
        for (String splitParameter : StringUtils.deleteWhitespace(providedParameter).split(",")) {
            guiThemeDetailsDao.persist(new GuiThemeDetailsVO(guiThemeId, type, splitParameter));
        }
    }

    @Override
    public InputStream[] getCleanupScripts() {
        return new InputStream[0];
    }
}
