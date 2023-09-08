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

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.cloud.utils.db.QueryBuilder;
import org.apache.cloudstack.quota.constant.ProcessingPeriod;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class QuotaTariffDaoImpl extends GenericDaoBase<QuotaTariffVO, Long> implements QuotaTariffDao {
    private static final Logger s_logger = Logger.getLogger(QuotaTariffDaoImpl.class.getName());

    private final SearchBuilder<QuotaTariffVO> searchUsageType;
    private final SearchBuilder<QuotaTariffVO> listAllIncludedUsageType;

    public QuotaTariffDaoImpl() {
        super();
        searchUsageType = createSearchBuilder();
        searchUsageType.and("usageType", searchUsageType.entity().getUsageType(), SearchCriteria.Op.EQ);
        searchUsageType.done();

        listAllIncludedUsageType = createSearchBuilder();
        listAllIncludedUsageType.and("onorbefore", listAllIncludedUsageType.entity().getEffectiveOn(), SearchCriteria.Op.LTEQ);
        listAllIncludedUsageType.and("quotatype", listAllIncludedUsageType.entity().getUsageType(), SearchCriteria.Op.EQ);
        listAllIncludedUsageType.done();
    }

    @Override
    public Boolean updateQuotaTariff(final QuotaTariffVO plan) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<Boolean>) status -> update(plan.getId(), plan));
    }

    @Override
    public QuotaTariffVO addQuotaTariff(final QuotaTariffVO plan) {
        if (plan.getIdObj() != null) {
            throw new IllegalStateException("The QuotaTariffVO being added should not have an Id set ");
        }
        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<QuotaTariffVO>) status -> persist(plan));
    }

    @Override
    public Pair<List<QuotaTariffVO>, Integer> listQuotaTariffs(Date startDate, Date endDate, Integer usageType, String name, String uuid, boolean listAll, Long startIndex,
                                                               Long pageSize, ProcessingPeriod processingPeriod, Integer executeOn) {
        return listQuotaTariffs(startDate, endDate, usageType, name, uuid, listAll, false, startIndex, pageSize, null, processingPeriod, executeOn);
    }

    @Override
    public Pair<List<QuotaTariffVO>, Integer> listQuotaTariffs(Date startDate, Date endDate, Integer usageType, String name, String uuid, boolean listAll, boolean listOnlyRemoved,
           Long startIndex, Long pageSize, String keyword, ProcessingPeriod processingPeriod, Integer executeOn) {

        Set<Integer> types = null;
        if (usageType != null) {
            types = Set.of(usageType);
        }

        return listQuotaTariffs(startDate, endDate, types, name, uuid, listAll, listOnlyRemoved, startIndex, pageSize, keyword, processingPeriod, executeOn);
    }

    @Override
    public Pair<List<QuotaTariffVO>, Integer> listQuotaTariffs(Date startDate, Date endDate, Set<Integer> usageTypes, String name, String uuid, boolean listAll,
           boolean listOnlyRemoved, Long startIndex, Long pageSize, String keyword, ProcessingPeriod processingPeriod, Integer executeOn) {
        SearchCriteria<QuotaTariffVO> searchCriteria = createListQuotaTariffsSearchCriteria(startDate, endDate, usageTypes, name, uuid, listOnlyRemoved, keyword, processingPeriod, executeOn);
        Filter sorter = new Filter(QuotaTariffVO.class, "usageType", false, startIndex, pageSize);
        sorter.addOrderBy(QuotaTariffVO.class, "effectiveOn", false);
        sorter.addOrderBy(QuotaTariffVO.class, "updatedOn", false);

        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<Pair<List<QuotaTariffVO>, Integer>>) status -> searchAndCount(searchCriteria, sorter, listAll));
    }

    /**
     * Lists quota tariffs with executeOn <= targetDate.
     */
    @Override
    public List<QuotaTariffVO> listQuotaTariffsWithExecuteOnUpToTargetDate(Integer targetDate) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<List<QuotaTariffVO>>) status -> {
            QueryBuilder<QuotaTariffVO> qb = QueryBuilder.create(QuotaTariffVO.class);

            qb.and(qb.entity().getProcessingPeriod(), SearchCriteria.Op.NEQ, ProcessingPeriod.BY_ENTRY);
            qb.and(qb.entity().getExecuteOn(), SearchCriteria.Op.LTEQ, targetDate);
            return search(qb.create(), null);
        });
    }

    /***
     * Lists quota tariffs with items that are not removed ordered first.
     * @param usageType usage type of the tariffs.
     * @param name name of the tariffs.
     * @return list of tariffs matching the provided parameters.
     */
    @Override
    public List<QuotaTariffVO> listQuotaTariffsOrderedByNotRemovedFirst(Integer usageType, String name) {
        return listQuotaTariffs(null, null, usageType, name, null, true, null, null, null, null)
                .first()
                .stream()
                .sorted(Comparator.comparing(QuotaTariffVO::getRemoved, Comparator.nullsFirst(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    protected SearchCriteria<QuotaTariffVO> createListQuotaTariffsSearchCriteria(Date startDate, Date endDate, Set<Integer> usageTypes, String name, String uuid,
        boolean listOnlyRemoved, String keyword, ProcessingPeriod processingPeriod, Integer executeOn) {
        SearchCriteria<QuotaTariffVO> searchCriteria = createListQuotaTariffsSearchBuilder(listOnlyRemoved, usageTypes).create();

        searchCriteria.setParametersIfNotNull("startDate", startDate);
        searchCriteria.setParametersIfNotNull("endDate", endDate);
        searchCriteria.setParametersIfNotNull("processingPeriod", processingPeriod);
        searchCriteria.setParametersIfNotNull("executeOn", executeOn);

        if (usageTypes != null) {
            searchCriteria.setParameters("usageType", usageTypes.toArray());
        }

        if (keyword != null) {
            searchCriteria.setParameters("nameLike", "%" + keyword + "%");
        }

        searchCriteria.setParametersIfNotNull("name", name);
        searchCriteria.setParametersIfNotNull("uuid", uuid);

        return searchCriteria;
    }

    protected SearchBuilder<QuotaTariffVO> createListQuotaTariffsSearchBuilder(boolean listOnlyRemoved, Set<Integer> usageTypes) {
        SearchBuilder<QuotaTariffVO> searchBuilder = createSearchBuilder();
        searchBuilder.and("startDate", searchBuilder.entity().getEffectiveOn(), SearchCriteria.Op.GTEQ);
        searchBuilder.and("endDate", searchBuilder.entity().getEndDate(), SearchCriteria.Op.LTEQ);
        searchBuilder.and("name", searchBuilder.entity().getName(), SearchCriteria.Op.EQ);
        searchBuilder.and("nameLike", searchBuilder.entity().getName(), SearchCriteria.Op.LIKE);
        searchBuilder.and("uuid", searchBuilder.entity().getUuid(), SearchCriteria.Op.EQ);
        searchBuilder.and("processingPeriod", searchBuilder.entity().getProcessingPeriod(), SearchCriteria.Op.EQ);
        searchBuilder.and("executeOn", searchBuilder.entity().getExecuteOn(), SearchCriteria.Op.EQ);

        if (listOnlyRemoved) {
            searchBuilder.and("removed", searchBuilder.entity().getRemoved(), SearchCriteria.Op.NNULL);
        }

        if (usageTypes != null) {
            searchBuilder.and("usageType", searchBuilder.entity().getUsageType(), SearchCriteria.Op.IN);
        }

        return searchBuilder;
    }

    @Override
    public QuotaTariffVO findByName(String name) {
        Pair<List<QuotaTariffVO>, Integer> pairQuotaTariffs = listQuotaTariffs(null, null, null, name, null, false, null, null, null, null);
        List<QuotaTariffVO> quotaTariffs = pairQuotaTariffs.first();

        if (CollectionUtils.isEmpty(quotaTariffs)) {
            s_logger.debug(String.format("Could not find quota tariff with name [%s].", name));
            return null;
        }

        return quotaTariffs.get(0);
    }

    @Override
    public QuotaTariffVO findById(Long id) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<QuotaTariffVO>) status -> super.findById(id));
    }

    @Override
    public QuotaTariffVO findByUuid(String uuid) {
        Pair<List<QuotaTariffVO>, Integer> pairQuotaTariffs = listQuotaTariffs(null, null, null, null, uuid, false, null, null, null, null);
        List<QuotaTariffVO> quotaTariffs = pairQuotaTariffs.first();

        if (CollectionUtils.isEmpty(quotaTariffs)) {
            s_logger.debug(String.format("Could not find quota tariff with UUID [%s].", uuid));
            return null;
        }

        return quotaTariffs.get(0);
    }
}
