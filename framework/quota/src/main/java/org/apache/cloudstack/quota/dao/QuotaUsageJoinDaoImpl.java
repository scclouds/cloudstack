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
package org.apache.cloudstack.quota.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import org.apache.cloudstack.quota.vo.QuotaUsageJoinVO;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;

@Component
public class QuotaUsageJoinDaoImpl extends GenericDaoBase<QuotaUsageJoinVO, Long> implements QuotaUsageJoinDao {

    private SearchBuilder<QuotaUsageJoinVO> searchQuotaUsages;

    @PostConstruct
    public void init() {
        searchQuotaUsages = createSearchBuilder();
        searchQuotaUsages.and("accountId", searchQuotaUsages.entity().getAccountId(), SearchCriteria.Op.EQ);
        searchQuotaUsages.and("domainId", searchQuotaUsages.entity().getDomainId(), SearchCriteria.Op.EQ);
        searchQuotaUsages.and("usageType", searchQuotaUsages.entity().getUsageType(), SearchCriteria.Op.EQ);
        searchQuotaUsages.and("resourceId", searchQuotaUsages.entity().getResourceId(), SearchCriteria.Op.EQ);
        searchQuotaUsages.and("networkId", searchQuotaUsages.entity().getNetworkId(), SearchCriteria.Op.EQ);
        searchQuotaUsages.and("offeringId", searchQuotaUsages.entity().getOfferingId(), SearchCriteria.Op.EQ);
        searchQuotaUsages.and("startDate", searchQuotaUsages.entity().getStartDate(), SearchCriteria.Op.BETWEEN);
        searchQuotaUsages.and("endDate", searchQuotaUsages.entity().getEndDate(), SearchCriteria.Op.BETWEEN);
        searchQuotaUsages.done();
    }

    @Override
    public List<QuotaUsageJoinVO> findQuotaUsage(Long accountId, Long domainId, Integer usageType, Long resourceId, Long networkId, Long offeringId, Date startDate, Date endDate) {
        SearchCriteria<QuotaUsageJoinVO> sc = searchQuotaUsages.create();

        sc.setParametersIfNotNull("accountId", accountId);
        sc.setParametersIfNotNull("domainId", domainId);
        sc.setParametersIfNotNull("usageType", usageType);
        sc.setParametersIfNotNull("resourceId", resourceId);
        sc.setParametersIfNotNull("networkId", networkId);
        sc.setParametersIfNotNull("offeringId", offeringId);

        if (ObjectUtils.allNotNull(startDate, endDate)) {
            sc.setParameters("startDate", startDate, endDate);
            sc.setParameters("endDate", startDate, endDate);
        }

        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<List<QuotaUsageJoinVO>>) status -> listBy(sc));
    }
}
