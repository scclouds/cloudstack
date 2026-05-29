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
package com.cloud.storage.dao;

import java.util.List;

import com.cloud.storage.VolumeVO;
import org.springframework.stereotype.Component;

import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.utils.DateUtil.IntervalType;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.JoinBuilder.JoinType;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Component
public class SnapshotPolicyDaoImpl extends GenericDaoBase<SnapshotPolicyVO, Long> implements SnapshotPolicyDao {
    @Inject
    private VolumeDao volumeDao;

    private SearchBuilder<SnapshotPolicyVO> VolumeIdSearch;
    private SearchBuilder<SnapshotPolicyVO> VolumeIdIntervalSearch;
    private SearchBuilder<SnapshotPolicyVO> ActivePolicySearch;
    private SearchBuilder<SnapshotPolicyVO> SnapshotPolicyListingSearch;
    private SearchBuilder<SnapshotPolicyVO> SnapshotPolicyListingWithVolumeNameSearch;

    public SnapshotPolicyDaoImpl() {
    }

    @PostConstruct
    private void init () {
        VolumeIdSearch = createSearchBuilder();
        VolumeIdSearch.and("volumeId", VolumeIdSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        VolumeIdSearch.and("active", VolumeIdSearch.entity().isActive(), SearchCriteria.Op.EQ);
        VolumeIdSearch.and("display", VolumeIdSearch.entity().isDisplay(), SearchCriteria.Op.EQ);
        VolumeIdSearch.done();

        VolumeIdIntervalSearch = createSearchBuilder();
        VolumeIdIntervalSearch.and("volumeId", VolumeIdIntervalSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        VolumeIdIntervalSearch.and("interval", VolumeIdIntervalSearch.entity().getInterval(), SearchCriteria.Op.EQ);
        VolumeIdIntervalSearch.done();

        ActivePolicySearch = createSearchBuilder();
        ActivePolicySearch.and("active", ActivePolicySearch.entity().isActive(), SearchCriteria.Op.EQ);
        ActivePolicySearch.done();

        SnapshotPolicyListingSearch = createSearchBuilder();
        SnapshotPolicyListingSearch.and("account_id", SnapshotPolicyListingSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        SnapshotPolicyListingSearch.and("domain_ids", SnapshotPolicyListingSearch.entity().getDomainId(), SearchCriteria.Op.IN);
        SnapshotPolicyListingSearch.and("id", SnapshotPolicyListingSearch.entity().getId(), SearchCriteria.Op.EQ);
        SnapshotPolicyListingSearch.and("interval_type", SnapshotPolicyListingSearch.entity().getInterval(), SearchCriteria.Op.EQ);
        SnapshotPolicyListingSearch.and("volume_id", SnapshotPolicyListingSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        SnapshotPolicyListingSearch.done();

        SnapshotPolicyListingWithVolumeNameSearch = createSearchBuilder();
        SnapshotPolicyListingWithVolumeNameSearch.and("account_id", SnapshotPolicyListingWithVolumeNameSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        SnapshotPolicyListingWithVolumeNameSearch.and("domain_ids", SnapshotPolicyListingWithVolumeNameSearch.entity().getDomainId(), SearchCriteria.Op.IN);
        SnapshotPolicyListingWithVolumeNameSearch.and("id", SnapshotPolicyListingWithVolumeNameSearch.entity().getId(), SearchCriteria.Op.EQ);
        SnapshotPolicyListingWithVolumeNameSearch.and("interval_type", SnapshotPolicyListingWithVolumeNameSearch.entity().getInterval(), SearchCriteria.Op.EQ);
        SnapshotPolicyListingWithVolumeNameSearch.and("volume_id", SnapshotPolicyListingWithVolumeNameSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);

        SearchBuilder<VolumeVO> volumeSearch = volumeDao.createSearchBuilder();
        volumeSearch.and("name",  volumeSearch.entity().getName(), SearchCriteria.Op.LIKE);
        SnapshotPolicyListingWithVolumeNameSearch.join("volumeJoin", volumeSearch, SnapshotPolicyListingWithVolumeNameSearch.entity().getVolumeId(), volumeSearch.entity().getId(), JoinType.INNER);

        SnapshotPolicyListingWithVolumeNameSearch.done();
    }

    @Override
    public SnapshotPolicyVO findOneByVolumeInterval(long volumeId, IntervalType intvType) {
        SearchCriteria<SnapshotPolicyVO> sc = VolumeIdIntervalSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("interval", intvType.ordinal());
        return findOneBy(sc);
    }

    @Override
    public List<SnapshotPolicyVO> listByVolumeId(long volumeId) {
        return listByVolumeId(volumeId, null);
    }

    @Override
    public List<SnapshotPolicyVO> listByVolumeId(long volumeId, Filter filter) {
        SearchCriteria<SnapshotPolicyVO> sc = VolumeIdSearch.create();
        sc.setParameters("volumeId", volumeId);
        return listBy(sc, filter);
    }

    @Override
    public Pair<List<SnapshotPolicyVO>, Integer> listAndCountByVolumeId(long volumeId, boolean display) {
        return listAndCountByVolumeId(volumeId, display, null);
    }

    @Override
    public Pair<List<SnapshotPolicyVO>, Integer> listAndCountByVolumeId(long volumeId, boolean display, Filter filter) {
        SearchCriteria<SnapshotPolicyVO> sc = VolumeIdSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("display", display);
        sc.setParameters("active", true);
        return searchAndCount(sc, filter);
    }

    @Override
    public Pair<List<SnapshotPolicyVO>, Integer> listSnapshotPolicies(Long accountId, List<Long> domainIds, Long scheduleId, Integer intervalType, Long volumeId, String keyword) {
        SearchCriteria<SnapshotPolicyVO> sc = (keyword != null && !keyword.isEmpty()) ? SnapshotPolicyListingWithVolumeNameSearch.create() : SnapshotPolicyListingSearch.create();

        sc.setParametersIfNotNull("account_id", accountId);
        sc.setParametersIfNotNull("id", scheduleId);
        sc.setParametersIfNotNull("interval_type", intervalType);
        sc.setParametersIfNotNull("volume_id", volumeId);

        if (!domainIds.isEmpty()) {
            sc.setParameters("domain_ids", domainIds.toArray());
        }

        if (keyword != null && !keyword.isEmpty()) {
            sc.setJoinParametersIfNotNull("volumeJoin", "name", "%" + keyword + "%");
        }

        Filter filter = new Filter(SnapshotPolicyVO.class, "id", false, null, null);

        return listAndCountIncludingRemovedBy(sc, filter);
    }

    @Override
    public List<SnapshotPolicyVO> listActivePolicies() {
        SearchCriteria<SnapshotPolicyVO> sc = ActivePolicySearch.create();
        sc.setParameters("active", true);
        return listIncludingRemovedBy(sc);
    }
}
