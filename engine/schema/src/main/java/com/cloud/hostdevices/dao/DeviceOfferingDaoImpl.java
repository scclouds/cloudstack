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

import com.cloud.hostdevices.DeviceOfferingDeviceTagVO;
import com.cloud.hostdevices.DeviceOfferingVO;
import com.cloud.hostdevices.VMInstanceDeviceOfferingsVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.hostdevices.DeviceOffering;
import org.apache.commons.collections.CollectionUtils;
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
    private SearchBuilder<DeviceOfferingVO> deviceOfferingWithTagsSearch;
    private SearchBuilder<DeviceOfferingVO> deviceOfferingWithVMSearch;

    @PostConstruct
    private void init() {
        deviceOfferingSearch = getBaseSearchBuilder();
        deviceOfferingSearch.done();

        deviceOfferingWithVMSearch = getBaseSearchBuilder();
        SearchBuilder<VMInstanceDeviceOfferingsVO> vmSearchBuilder = vmDeviceOfferingsDao.createSearchBuilder();
        vmSearchBuilder.and("virtualMachineId", vmSearchBuilder.entity().getVirtualMachineId(), SearchCriteria.Op.EQ);
        deviceOfferingWithVMSearch.join("vmSearch", vmSearchBuilder, deviceOfferingWithVMSearch.entity().getId(), vmSearchBuilder.entity().getDeviceOfferingId(), JoinBuilder.JoinType.INNER);
        deviceOfferingWithVMSearch.done();

        deviceOfferingWithTagsSearch = getBaseSearchBuilder();
        SearchBuilder<DeviceOfferingDeviceTagVO> deviceTagSearchBuilder = deviceOfferingDeviceTagDao.createSearchBuilder();
        deviceTagSearchBuilder.and("deviceTag", deviceTagSearchBuilder.entity().getDeviceTag(), SearchCriteria.Op.IN);
        deviceOfferingWithTagsSearch.join("deviceTagSearch", deviceTagSearchBuilder, deviceOfferingWithTagsSearch.entity().getId(), deviceTagSearchBuilder.entity().getDeviceOfferingId(), JoinBuilder.JoinType.INNER);
        deviceOfferingWithTagsSearch.done();
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
    public Pair<List<DeviceOfferingVO>, Integer> listDeviceOfferings(Long id, String name, List<Long> domainIds, Long zoneId, List<String> deviceTags, DeviceOffering.State state, Boolean showOnlyPublic, Filter filter) {
        SearchCriteria<DeviceOfferingVO> sc = CollectionUtils.isEmpty(deviceTags) ? deviceOfferingSearch.create() : deviceOfferingWithTagsSearch.create();
        sc.setParametersIfNotNull("id", id);

        if (name != null) {
            sc.setParameters("name", "%" + name + "%");
        }

        sc.setParametersIfNotNull("zoneId", zoneId);
        sc.setParametersIfNotNull("state", state);
        sc.setParametersIfNotNull("isPublic", showOnlyPublic);

        if (domainIds != null && !domainIds.isEmpty()) {
            sc.setParametersIfNotNull("domainIds", domainIds.toArray());
        }

        if (!CollectionUtils.isEmpty(deviceTags)) {
            sc.setJoinParametersIfNotNull("deviceTagSearch", "deviceTag", deviceTags.toArray());
        }

        return searchAndCount(sc, filter);
    }

    @Override
    public DeviceOfferingVO findByName(String name) {
        SearchCriteria<DeviceOfferingVO> sc = deviceOfferingSearch.create();
        sc.setParametersIfNotNull("name", name);
        return findOneBy(sc);
    }

    @Override
    public List<String> listDeviceOfferingTags(Long deviceOfferingId) {
        return deviceOfferingDeviceTagDao.getDeviceOfferingTags(deviceOfferingId);
    }

    private SearchBuilder<DeviceOfferingVO> getBaseSearchBuilder() {
        SearchBuilder<DeviceOfferingVO> sc = createSearchBuilder();

        sc.and("id", sc.entity().getId(), SearchCriteria.Op.EQ);
        sc.and("name", sc.entity().getName(), SearchCriteria.Op.LIKE);
        sc.and("domainIds", sc.entity().getDomainId(), SearchCriteria.Op.IN);
        sc.and("zoneId", sc.entity().getZoneId(), SearchCriteria.Op.EQ);
        sc.and("state", sc.entity().getState(), SearchCriteria.Op.EQ);
        sc.and("isPublic", sc.entity().getIsPublic(), SearchCriteria.Op.EQ);

        return sc;
    }
}
