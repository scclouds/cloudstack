//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.quota.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import org.apache.cloudstack.quota.vo.QuotaUsageDetailVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;

@Component
public class QuotaUsageDetailDaoImpl extends GenericDaoBase<QuotaUsageDetailVO, Long> implements QuotaUsageDetailDao {
    private static final Logger s_logger = Logger.getLogger(QuotaUsageDetailDaoImpl.class);

    private SearchBuilder<QuotaUsageDetailVO> searchQuotaUsageDetails;

    @Inject
    QuotaUsageDao quotaUsageDao;

    @PostConstruct
    public void init() {
        SearchBuilder<QuotaUsageVO> searchQuotaUsages = quotaUsageDao.createSearchBuilder();
        searchQuotaUsages.and("accountId", searchQuotaUsages.entity().getAccountId(), SearchCriteria.Op.EQ);
        searchQuotaUsages.and("domainId", searchQuotaUsages.entity().getDomainId(), SearchCriteria.Op.EQ);
        searchQuotaUsages.and("usageType", searchQuotaUsages.entity().getUsageType(), SearchCriteria.Op.EQ);
        searchQuotaUsages.and("startDate", searchQuotaUsages.entity().getStartDate(), SearchCriteria.Op.BETWEEN);
        searchQuotaUsages.and("endDate", searchQuotaUsages.entity().getEndDate(), SearchCriteria.Op.BETWEEN);

        searchQuotaUsageDetails = createSearchBuilder();
        searchQuotaUsageDetails.and("tariffId", searchQuotaUsageDetails.entity().getTariffId(), SearchCriteria.Op.EQ);
        searchQuotaUsageDetails.join("searchQuotaUsages", searchQuotaUsages, searchQuotaUsageDetails.entity().getQuotaUsageId(),
                searchQuotaUsages.entity().getId(), JoinBuilder.JoinType.INNER);
        searchQuotaUsageDetails.done();
    }

    @Override
    public void persistQuotaUsageDetail(final QuotaUsageDetailVO quotaUsageDetail) {
        s_logger.trace(String.format("Persisting quota usage detail [%s].", quotaUsageDetail));
        Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<QuotaUsageDetailVO>) status -> persist(quotaUsageDetail));
    }

    @Override
    public List<QuotaUsageDetailVO> listQuotaUsageDetails(Long quotaUsageId) {
        SearchCriteria<QuotaUsageDetailVO> sc = searchQuotaUsageDetails.create();
        sc.setParameters("quotaUsageId", quotaUsageId);
        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<List<QuotaUsageDetailVO>>) status -> listBy(sc));
    }

    @Override
    public List<QuotaUsageDetailVO> findQuotaUsageDetails(Long accountId, Long domainId, Integer usageType, Long tariffId, Date startDate, Date endDate) {
        SearchCriteria<QuotaUsageDetailVO> sc = searchQuotaUsageDetails.create();

        sc.setParametersIfNotNull("tariffId", tariffId);

        sc.setJoinParametersIfNotNull("searchQuotaUsages", "accountId", accountId);
        sc.setJoinParametersIfNotNull("searchQuotaUsages", "domainId", domainId);
        sc.setJoinParametersIfNotNull("searchQuotaUsages", "usageType", usageType);

        if (ObjectUtils.allNotNull(startDate, endDate)) {
            sc.setJoinParameters("searchQuotaUsages", "startDate", startDate, endDate);
            sc.setJoinParameters("searchQuotaUsages", "endDate", startDate, endDate);
        }

        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<List<QuotaUsageDetailVO>>) status -> listBy(sc));
    }

}
