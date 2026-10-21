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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.cloud.usage.UsageHostDeviceVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class UsageHostDeviceDaoImpl extends GenericDaoBase<UsageHostDeviceVO, Long> implements UsageHostDeviceDao {

    private SearchBuilder<UsageHostDeviceVO> openUsageSearch;
    private SearchBuilder<UsageHostDeviceVO> activeInPeriodSearch;

    @PostConstruct
    public void init() {
        openUsageSearch = createSearchBuilder();
        openUsageSearch.and("hostDeviceId", openUsageSearch.entity().getHostDeviceId(), SearchCriteria.Op.EQ);
        openUsageSearch.and("removed", openUsageSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        openUsageSearch.done();

        activeInPeriodSearch = createSearchBuilder();
        activeInPeriodSearch.and("accountId", activeInPeriodSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        activeInPeriodSearch.and("createdBeforeEnd", activeInPeriodSearch.entity().getCreated(), SearchCriteria.Op.LTEQ);
        activeInPeriodSearch.and().op("notRemoved", activeInPeriodSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeInPeriodSearch.or("removedAfterStart", activeInPeriodSearch.entity().getRemoved(), SearchCriteria.Op.GTEQ);
        activeInPeriodSearch.cp();
        activeInPeriodSearch.done();
    }

    @Override
    public void remove(long hostDeviceId, Date removed) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            SearchCriteria<UsageHostDeviceVO> sc = openUsageSearch.create();
            sc.setParameters("hostDeviceId", hostDeviceId);

            for (UsageHostDeviceVO usage : listBy(sc)) {
                usage.setRemoved(removed);
                update(usage.getId(), usage);
            }
        } catch (final Exception e) {
            txn.rollback();
            logger.error("Error closing the usage of host device [{}] due to [{}].", hostDeviceId, e.getMessage(), e);
        } finally {
            txn.close();
        }
    }

    @Override
    public List<UsageHostDeviceVO> getUsageRecords(long accountId, Date startDate, Date endDate) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            SearchCriteria<UsageHostDeviceVO> sc = activeInPeriodSearch.create();
            sc.setParameters("accountId", accountId);
            sc.setParameters("createdBeforeEnd", endDate);
            sc.setParameters("removedAfterStart", startDate);

            return listBy(sc);
        } catch (final Exception e) {
            txn.rollback();
            logger.warn("Error getting the host device usage records of account [{}] due to [{}].", accountId, e.getMessage(), e);
            return new ArrayList<>();
        } finally {
            txn.close();
        }
    }
}
