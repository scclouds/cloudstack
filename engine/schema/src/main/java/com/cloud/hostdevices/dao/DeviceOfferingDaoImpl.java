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

package com.cloud.hostdevices.dao;

import com.cloud.hostdevices.DeviceOfferingVO;
import com.cloud.hostdevices.VMInstanceDeviceOfferingsVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.hostdevices.DeviceOffering;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Component
public class DeviceOfferingDaoImpl extends GenericDaoBase<DeviceOfferingVO, Long> implements DeviceOfferingDao {
    @Inject
    private VMInstanceDeviceOfferingsDao vmDeviceOfferingsDao;
    @Inject
    private DeviceOfferingDeviceTagDao deviceOfferingDeviceTagDao;

    private SearchBuilder<DeviceOfferingVO> deviceOfferingSearch;
    private SearchBuilder<DeviceOfferingVO> deviceOfferingVisibleDomainsSearch;
    private SearchBuilder<DeviceOfferingVO> deviceOfferingZoneSearch;
    private SearchBuilder<DeviceOfferingVO> deviceOfferingVisibleDomainsAndZoneSearch;
    private SearchBuilder<DeviceOfferingVO> deviceOfferingWithVMSearch;
    private SearchBuilder<DeviceOfferingVO> deviceOfferingNameSearch;

    @PostConstruct
    private void init() {
        deviceOfferingSearch = getBaseSearchBuilder();
        deviceOfferingSearch.done();

        deviceOfferingVisibleDomainsSearch = getBaseSearchBuilder();
        addVisibleDomainsCondition(deviceOfferingVisibleDomainsSearch);
        deviceOfferingVisibleDomainsSearch.done();

        deviceOfferingZoneSearch = getBaseSearchBuilder();
        addZoneCondition(deviceOfferingZoneSearch);
        deviceOfferingZoneSearch.done();

        deviceOfferingVisibleDomainsAndZoneSearch = getBaseSearchBuilder();
        addVisibleDomainsCondition(deviceOfferingVisibleDomainsAndZoneSearch);
        addZoneCondition(deviceOfferingVisibleDomainsAndZoneSearch);
        deviceOfferingVisibleDomainsAndZoneSearch.done();

        deviceOfferingNameSearch = createSearchBuilder();
        deviceOfferingNameSearch.and("name", deviceOfferingNameSearch.entity().getName(), SearchCriteria.Op.EQ);
        deviceOfferingNameSearch.done();

        deviceOfferingWithVMSearch = getBaseSearchBuilder();
        SearchBuilder<VMInstanceDeviceOfferingsVO> vmSearchBuilder = vmDeviceOfferingsDao.createSearchBuilder();
        vmSearchBuilder.and("virtualMachineId", vmSearchBuilder.entity().getVirtualMachineId(), SearchCriteria.Op.EQ);
        deviceOfferingWithVMSearch.join("vmSearch", vmSearchBuilder, deviceOfferingWithVMSearch.entity().getId(), vmSearchBuilder.entity().getDeviceOfferingId(), JoinBuilder.JoinType.INNER);
        deviceOfferingWithVMSearch.done();
    }

    @Override
    public List<DeviceOfferingVO> listVirtualMachineDeviceOfferings(Long virtualMachineId) {
        if (virtualMachineId == null) {
            return new ArrayList<>();
        }

        SearchCriteria<DeviceOfferingVO> sc = deviceOfferingWithVMSearch.create();
        sc.setJoinParameters("vmSearch", "virtualMachineId", virtualMachineId);
        return listBy(sc);
    }

    @Override
    public Pair<List<DeviceOfferingVO>, Integer> listDeviceOfferings(Long id, String name, String keyword, Long domainId, List<Long> visibleDomainIds, Long zoneId, List<String> deviceTags, DeviceOffering.State state, Filter filter) {
        SearchCriteria<DeviceOfferingVO> sc = getListSearchBuilder(visibleDomainIds != null, zoneId != null).create();
        sc.setParametersIfNotNull("id", id);

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        if (StringUtils.isNotBlank(keyword)) {
            sc.setParameters("keyword", "%" + keyword + "%");
        }

        sc.setParametersIfNotNull("exactDomainId", domainId);
        sc.setParametersIfNotNull("state", state);

        if (CollectionUtils.isNotEmpty(visibleDomainIds)) {
            sc.setParameters("visibleDomainIds", visibleDomainIds.toArray());
        }

        sc.setParametersIfNotNull("zoneId", zoneId);

        if (CollectionUtils.isNotEmpty(deviceTags)) {
            List<Long> taggedOfferingIds = deviceOfferingDeviceTagDao.listDeviceOfferingIdsByTags(deviceTags);

            if (taggedOfferingIds.isEmpty()) {
                return new Pair<>(new ArrayList<>(), 0);
            }

            sc.setParameters("ids", taggedOfferingIds.toArray());
        }

        return searchAndCount(sc, filter);
    }

    @Override
    public DeviceOfferingVO findByName(String name) {
        SearchCriteria<DeviceOfferingVO> sc = deviceOfferingNameSearch.create();
        sc.setParameters("name", name);
        return findOneBy(sc);
    }

    private SearchBuilder<DeviceOfferingVO> getListSearchBuilder(boolean hasVisibleDomains, boolean hasZone) {
        if (hasVisibleDomains && hasZone) {
            return deviceOfferingVisibleDomainsAndZoneSearch;
        }

        if (hasVisibleDomains) {
            return deviceOfferingVisibleDomainsSearch;
        }

        if (hasZone) {
            return deviceOfferingZoneSearch;
        }

        return deviceOfferingSearch;
    }

    private void addVisibleDomainsCondition(SearchBuilder<DeviceOfferingVO> searchBuilder) {
        searchBuilder.and().op("domainIsNull", searchBuilder.entity().getDomainId(), SearchCriteria.Op.NULL);
        searchBuilder.or("visibleDomainIds", searchBuilder.entity().getDomainId(), SearchCriteria.Op.IN);
        searchBuilder.cp();
    }

    private void addZoneCondition(SearchBuilder<DeviceOfferingVO> searchBuilder) {
        searchBuilder.and().op("zoneIsNull", searchBuilder.entity().getZoneId(), SearchCriteria.Op.NULL);
        searchBuilder.or("zoneId", searchBuilder.entity().getZoneId(), SearchCriteria.Op.EQ);
        searchBuilder.cp();
    }

    private SearchBuilder<DeviceOfferingVO> getBaseSearchBuilder() {
        SearchBuilder<DeviceOfferingVO> sc = createSearchBuilder();

        sc.and("id", sc.entity().getId(), SearchCriteria.Op.EQ);
        sc.and("ids", sc.entity().getId(), SearchCriteria.Op.IN);
        sc.and("name", sc.entity().getName(), SearchCriteria.Op.LIKE);
        sc.and("keyword", sc.entity().getName(), SearchCriteria.Op.LIKE);
        sc.and("exactDomainId", sc.entity().getDomainId(), SearchCriteria.Op.EQ);
        sc.and("state", sc.entity().getState(), SearchCriteria.Op.EQ);

        return sc;
    }
}
