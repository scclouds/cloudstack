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

import com.cloud.storage.Volume;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import org.apache.cloudstack.backup.Backup;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class Upgrade42003to42004 extends DbUpgradeAbstractImpl {

    private static final String SELECT_OFFERING = "SELECT `id` FROM `cloud`.`native_backup_offering`;";

    private static final String INSERT_OFFERING = "INSERT INTO `cloud`.`native_backup_offering` (uuid, name, compress, validate, allow_quick_restore, allow_extract_file, created) " +
            "VALUES (uuid(), 'Standard', false, false, false, false, now());";
    private static final String UPDATE_OFFERING = "UPDATE `cloud`.`backup_offering` bo SET `external_id`=(SELECT `uuid` from `cloud`.`native_backup_offering` nbo WHERE `id`=1)" +
            " WHERE `external_id`='SB' AND `provider`='knib';";

    private static final String SELECT_BACKUPS = "SELECT `id` FROM `backups` WHERE `backed_volumes` IS NULL AND `status` ='BackedUp';";

    private static final String SELECT_BACKED_VOLUMES = "SELECT `volume_id` from `native_backup_store_ref` WHERE `backup_id`=%s;";

    private static final String SELECT_VOLUME = "SELECT `uuid`, `path`, `volume_type`, `size`, `device_id` from  `volumes` WHERE `id` = %s;";

    private static final String UPDATE_BACKUP = "UPDATE `backups` SET `backed_volumes` = '%s' WHERE `id` = %s;";

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.20.0.3", "4.20.0.4"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.20.0.4";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-42003to42004.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        addIndexes(conn);
        migrateBackedVolumes(conn);
        migrateNativeOffering(conn);
    }

    private void addIndexes(Connection conn) {
        DbUpgradeUtils.addIndexIfNeeded(conn, "event", "account_id", "domain_id", "archived",  "display",
                "resource_type", "resource_id", "start_id", "type", "level", "created", "id");
    }

    private void migrateBackedVolumes(Connection conn) {
        ResultSet backupsToUpdate;
        try (PreparedStatement select = conn.prepareStatement(SELECT_BACKUPS)) {
            backupsToUpdate = select.executeQuery();

            while (backupsToUpdate.next()) {
                long backupId = backupsToUpdate.getLong(1);

                logger.debug("Migrating backed_volumes for backup [{}].", backupId);

                String queryBackedVolumes = String.format(SELECT_BACKED_VOLUMES, backupId);
                ResultSet backedVolumes = conn.prepareStatement(queryBackedVolumes).executeQuery();
                String volumeInfos = getVolumeInfos(conn, backedVolumes);

                logger.debug("Got volume infos [{}] for backup [{}]. Updating it.", volumeInfos, backupId);

                String updateBackupQuery = String.format(UPDATE_BACKUP, volumeInfos, backupId);
                conn.prepareStatement(updateBackupQuery).executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Unable to migrate backup data due to [{}].", e);
            throw new CloudRuntimeException(e);
        }
    }

    private String getVolumeInfos(Connection conn, ResultSet backedVolumes) throws SQLException {
        ArrayList<Backup.VolumeInfo> volumeInfos = new ArrayList<>();
        while (backedVolumes.next()) {
            String queryVolumes = String.format(SELECT_VOLUME, backedVolumes.getLong(1));
            ResultSet volume = conn.prepareStatement(queryVolumes).executeQuery();

            if (!volume.next()) {
                throw new CloudRuntimeException("No volume returned?");
            }
            Backup.VolumeInfo volumeInfo = new Backup.VolumeInfo(volume.getString(1), volume.getString(2), Volume.Type.valueOf(volume.getString(3)), volume.getLong(4),
            volume.getLong(5));
            volumeInfos.add(volumeInfo);
        }
        return new Gson().toJson(volumeInfos.toArray(), Backup.VolumeInfo[].class);
    }

    private void migrateNativeOffering(Connection conn) {
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_OFFERING)) {
            try (ResultSet result = pstmt.executeQuery()) {
                if (result.next()) {
                    logger.info("There are already entries on the native_backup_offering table, no need to migrate data.");
                    return;
                }
            }
        } catch (SQLException e) {
            String message = String.format("Unable to retrieve native backup offerings due to [%s].", e.getMessage());
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        }

        logger.info("Migrating legacy backup offering");

        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_OFFERING)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            String message = String.format("Unable to insert native backup offering due to [%s].", e.getMessage());
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        }

        try (PreparedStatement pstmt = conn.prepareStatement(UPDATE_OFFERING)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            String message = String.format("Unable to update legacy backup offering due to [%s].", e.getMessage());
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    @Override
    public InputStream[] getCleanupScripts() {
        return new InputStream[0];
    }
}
