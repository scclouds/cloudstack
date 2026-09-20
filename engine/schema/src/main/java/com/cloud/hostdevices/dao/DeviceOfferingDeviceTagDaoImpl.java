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
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.hostdevices.DeviceOffering;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DeviceOfferingDeviceTagDaoImpl extends GenericDaoBase<DeviceOfferingDeviceTagVO, Long> implements DeviceOfferingDeviceTagDao {
    private final SearchBuilder<DeviceOfferingDeviceTagVO> deviceOfferingDeviceTagSearch;

    public DeviceOfferingDeviceTagDaoImpl() {
        deviceOfferingDeviceTagSearch = createSearchBuilder();
        deviceOfferingDeviceTagSearch.and("deviceOfferingId", deviceOfferingDeviceTagSearch.entity().getDeviceOfferingId(), SearchCriteria.Op.IN);
        deviceOfferingDeviceTagSearch.done();
    }

    @Override
    public List<DeviceOfferingDeviceTagVO> getDeviceOfferingTags(Long deviceOfferingId) {
        return getTags(List.of(deviceOfferingId));
    }

    @Override
    public List<DeviceOfferingDeviceTagVO> getDeviceOfferingsTags(List<? extends DeviceOffering> deviceOfferings) {
        if (CollectionUtils.isEmpty(deviceOfferings)) {
            return new ArrayList<>();
        }
        return getTags(deviceOfferings.stream().map(DeviceOffering::getId).collect(Collectors.toList()));
    }

    private List<DeviceOfferingDeviceTagVO> getTags(List<Long> offeringIds) {
        SearchCriteria<DeviceOfferingDeviceTagVO> sc = deviceOfferingDeviceTagSearch.create();
        sc.setParameters("deviceOfferingId", offeringIds.toArray());
        return listBy(sc);
    }

    @Override
    public void expungeByOfferingId(long id) {
        SearchCriteria<DeviceOfferingDeviceTagVO> sc = deviceOfferingDeviceTagSearch.create();
        sc.setParameters("deviceOfferingId", List.of(id).toArray());
        expunge(sc);
    }

}
