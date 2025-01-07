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
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Upgrade42000to42001 extends DbUpgradeAbstractImpl {

    private static final String SELECT_SNAPSHOTS = "SELECT s.`id` FROM `cloud`.`snapshots` s WHERE s.`snapshot_type` != 7;";

    private static final String DELETE_SNAPSHOTS = "DELETE FROM `cloud`.`snapshot_store_ref` WHERE `snapshot_id`=? AND `store_role`='Primary';";

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.20.0.0", "4.20.0.1"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.20.0.1";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-42000to42001.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        if (!SnapshotInfo.BackupSnapshotAfterTakingSnapshot.value()) {
            throw new CloudRuntimeException(String.format("The upgrade to 4.20.0.1-scclouds assumes [%s] is false, if there are snapshots on primary storage, the data migration must be made manually.", SnapshotInfo.BackupSnapshotAfterTakingSnapshot));
        }

        List<Long> snapshotIds = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_SNAPSHOTS)) {
            try (ResultSet result = pstmt.executeQuery()) {
                while (result.next()) {
                    snapshotIds.add(result.getLong("id"));
                }
            }
        } catch (SQLException e) {
            String message = String.format("Unable to retrieve snapshots due to [%s].", e.getMessage());
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        }

        logger.info("Got {} snapshots for data migration, will delete the stale snapshot references for those.", snapshotIds.size());

        for (Long snapshotId : snapshotIds) {
            logger.debug("Removing stale primary storage references of snapshot {}.", snapshotId);
            try (PreparedStatement pstmt = conn.prepareStatement(DELETE_SNAPSHOTS)) {
                pstmt.setLong(1, snapshotId);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                String message = String.format("Unable to remove stale snapshot references due to [%s].", e.getMessage());
                logger.error(message, e);
                throw new CloudRuntimeException(message, e);
            }
        }
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-42000to42001-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }
}
