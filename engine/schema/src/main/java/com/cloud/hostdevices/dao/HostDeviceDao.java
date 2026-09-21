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

import com.cloud.hostdevices.HostDeviceVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.hostdevices.HostDevice;

import java.util.List;

public interface HostDeviceDao extends GenericDao<HostDeviceVO, Long> {
    List<HostDeviceVO> listHostDevicesByHostId(Long hostId);

    List<HostDeviceVO> listHostDevicesAvailableForAllocation(Long hostId, List<String> deviceTags);

    Pair<List<HostDeviceVO>, Integer> listHostDevices(Long hostDeviceId, Long accountId, List<Long> domainIds, Long hostId, Long virtualMachineId, String deviceTag, HostDevice.State state, HostDevice.Type type, Filter filter);

    List<HostDeviceVO> listHostDevicesByVmId(Long vmId);

    List<HostDeviceVO> listHostDevicesByHostIdAndState(Long hostId, HostDevice.State state);

    List<HostDeviceVO> listAndLockHostDevicesByHostIdAndState(Long hostId, HostDevice.State state);

    List<HostDeviceVO> listHostDevicesByAccountId(long accountId);

    List<HostDeviceVO> listHostDevicesForOfferingAndVmCheck(Long hostId, List<String> deviceOfferingsTags, Long virtualMachineId);

    List<HostDeviceVO> listAndLockHostDevicesByVmId(Long vmId);
}
