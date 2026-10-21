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

import java.util.Date;
import java.util.List;

import com.cloud.usage.UsageHostDeviceVO;
import com.cloud.utils.db.GenericDao;

public interface UsageHostDeviceDao extends GenericDao<UsageHostDeviceVO, Long> {

    /**
     * Closes the open usage entries of the host device, setting their removal date.
     */
    void remove(long hostDeviceId, Date removed);

    /**
     * Lists the usage entries of the account that were active in any moment between the start and end dates.
     */
    List<UsageHostDeviceVO> getUsageRecords(long accountId, Date startDate, Date endDate);
}
