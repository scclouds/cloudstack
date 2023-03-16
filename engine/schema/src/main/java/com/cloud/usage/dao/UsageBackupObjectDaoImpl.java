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

package com.cloud.usage.dao;

import com.cloud.usage.UsageBackupObjectVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;

@Component
public class UsageBackupObjectDaoImpl extends GenericDaoBase<UsageBackupObjectVO, Long> implements UsageBackupObjectDao {
    private final Logger logger = LogManager.getLogger(getClass());


    private SearchBuilder<UsageBackupObjectVO> searchUsageBackupObjects;

    @PostConstruct
    public void init() {
        searchUsageBackupObjects = createSearchBuilder();

        searchUsageBackupObjects.and("accountId", searchUsageBackupObjects.entity().getAccountId(), SearchCriteria.Op.EQ);
        searchUsageBackupObjects.and("backupId", searchUsageBackupObjects.entity().getBackupId(), SearchCriteria.Op.EQ);
        searchUsageBackupObjects.and("created", searchUsageBackupObjects.entity().getCreated(), SearchCriteria.Op.BETWEEN);

        searchUsageBackupObjects.done();
    }

    @Override
    public void persistUsageBackupObject(UsageBackupObjectVO usageBackupObject) {
        logger.trace(String.format("Persisting usage backup object [%s].", usageBackupObject));
        Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<UsageBackupObjectVO>) status -> persist(usageBackupObject));
    }

    @Override
    public void removeUsageBackupObjectByBackupId(Long backupId) {
        SearchCriteria<UsageBackupObjectVO> sc = searchUsageBackupObjects.create();

        sc.setParameters("backupId", backupId);

        logger.trace(String.format("Deleting usage backup object with ID [%s].", backupId));
        Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<Integer>) status -> remove(sc));
    }

    @Override
    public List<UsageBackupObjectVO> listUsageBackupObjectRecords(Long accountId, Date startDate, Date endDate) {
        SearchCriteria<UsageBackupObjectVO> sc = searchUsageBackupObjects.create();

        sc.setParameters("accountId", accountId);
        sc.setParameters("created", startDate, endDate);

        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<List<UsageBackupObjectVO>>) status -> listBy(sc));
    }
}
