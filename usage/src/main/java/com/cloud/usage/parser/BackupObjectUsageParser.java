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

package com.cloud.usage.parser;

import com.cloud.usage.UsageBackupObjectVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageBackupObjectDao;
import com.cloud.usage.dao.UsageDao;
import com.cloud.user.AccountVO;
import org.apache.cloudstack.usage.UsageTypes;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

@Component
public class BackupObjectUsageParser {
    protected static Logger LOGGER = Logger.getLogger(BackupObjectUsageParser.class);

    private static UsageDao s_usageDao;
    private static UsageBackupObjectDao s_usageBackupObjectDao;

    @Inject
    private UsageDao usageDao;
    @Inject
    private UsageBackupObjectDao usageBackupObjectDao;

    @PostConstruct
    void init() {
        s_usageDao = usageDao;
        s_usageBackupObjectDao = usageBackupObjectDao;
    }

    public static boolean parse(AccountVO account, Date startDate, Date endDate) {
        LOGGER.debug(String.format("Parsing all backup object usage events for account [%s].", account));

        if (endDate == null || endDate.after(new Date())) {
            endDate = new Date();
        }

        final List<UsageBackupObjectVO> usageBackupObjects = s_usageBackupObjectDao.listUsageBackupObjectRecords(account.getId(), startDate, endDate);
        if (CollectionUtils.isEmpty(usageBackupObjects)) {
            LOGGER.debug(String.format("No usage backup objects for account [%s] and period between [%s] and [%s].", account, startDate, endDate));
            return true;
        }

        for (final UsageBackupObjectVO usageBackupObject : usageBackupObjects) {
            final Long backupId = usageBackupObject.getBackupId();
            final Long backupOfferingId = usageBackupObject.getBackupOfferingId();
            final Long vmId = usageBackupObject.getVmId();

            Date createdDate = usageBackupObject.getCreated();
            if (createdDate.before(startDate)) {
                createdDate = startDate;
            }

            Date removedDate = usageBackupObject.getRemoved();
            if (removedDate == null || removedDate.after(endDate)) {
                removedDate = endDate;
            }

            final long duration = (removedDate.getTime() - createdDate.getTime()) + 1;

            final float usage = duration / 1000f / 60f / 60f;
            DecimalFormat decimalFormat = new DecimalFormat("#.######");
            String usageDisplay = decimalFormat.format(usage);

            final String description = String.format("Backup object usage for backup with ID: %d, backup offering: %d, and VM: %d", backupId, backupOfferingId, vmId);

            final UsageVO usageRecord = new UsageVO(usageBackupObject.getZoneId(), account.getAccountId(), account.getDomainId(), description,
                    String.format("%s Hrs", usageDisplay), UsageTypes.BACKUP_OBJECT, new Double(usage), vmId, null, backupOfferingId, null, backupId, usageBackupObject.getSize(),
                    usageBackupObject.getProtectedSize(), startDate, endDate);

            s_usageDao.persist(usageRecord);
        }

        return true;
    }
}