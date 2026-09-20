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

import com.cloud.hostdevices.VMInstanceDeviceOfferingsVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VMInstanceDeviceOfferingsDaoImpl extends GenericDaoBase<VMInstanceDeviceOfferingsVO, Long> implements VMInstanceDeviceOfferingsDao {
    private final SearchBuilder<VMInstanceDeviceOfferingsVO> searchBuilder;

    public VMInstanceDeviceOfferingsDaoImpl() {
        searchBuilder = createSearchBuilder();
        searchBuilder.and("deviceOfferingId", searchBuilder.entity().getDeviceOfferingId(), SearchCriteria.Op.EQ);
        searchBuilder.and("virtualMachineId", searchBuilder.entity().getVirtualMachineId(), SearchCriteria.Op.EQ);
        searchBuilder.done();
    }

    @Override
    public List<VMInstanceDeviceOfferingsVO> listByVmId(Long virtualMachineId) {
        SearchCriteria<VMInstanceDeviceOfferingsVO> sc = searchBuilder.create();
        sc.setParameters("virtualMachineId", virtualMachineId);
        return listBy(sc);
    }

    @Override
    public List<VMInstanceDeviceOfferingsVO> listByOfferingId(Long id) {
        SearchCriteria<VMInstanceDeviceOfferingsVO> sc = searchBuilder.create();
        sc.setParameters("deviceOfferingId", id);
        return listBy(sc);
    }

    @Override
    public VMInstanceDeviceOfferingsVO findByVmIdAndDeviceId(Long vmId, Long deviceId) {
        SearchCriteria<VMInstanceDeviceOfferingsVO> sc = searchBuilder.create();
        sc.setParameters("virtualMachineId", vmId);
        sc.setParameters("deviceOfferingId", deviceId);
        return findOneBy(sc);
    }

    @Override
    public int expungeByVmId(Long vmId) {
        SearchCriteria<VMInstanceDeviceOfferingsVO> sc = searchBuilder.create();
        sc.setParameters("virtualMachineId", vmId);
        return expunge(sc);
    }
}
