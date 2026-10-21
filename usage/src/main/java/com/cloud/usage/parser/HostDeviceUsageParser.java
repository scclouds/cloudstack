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
package com.cloud.usage.parser;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.usage.UsageTypes;
import org.springframework.stereotype.Component;

import com.cloud.usage.UsageHostDeviceVO;
import com.cloud.usage.UsageManagerImpl;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageHostDeviceDao;
import com.cloud.user.AccountVO;
import com.cloud.utils.DateUtil;

@Component
public class HostDeviceUsageParser extends UsageParser {
    @Inject
    private UsageHostDeviceDao usageHostDeviceDao;

    @Override
    public String getParserName() {
        return "Host Device";
    }

    @Override
    protected boolean parse(AccountVO account, Date startDate, Date endDate) {
        if ((endDate == null) || endDate.after(new Date())) {
            endDate = new Date();
        }

        List<UsageHostDeviceVO> usageHostDevices = usageHostDeviceDao.getUsageRecords(account.getId(), startDate, endDate);

        if (usageHostDevices.isEmpty()) {
            logger.debug("No host device usage events for this period.");
            return true;
        }

        Map<HostDeviceUsageKey, List<UsageHostDeviceVO>> usagesPerKey = usageHostDevices.stream()
                .collect(Collectors.groupingBy(HostDeviceUsageKey::new));

        for (List<UsageHostDeviceVO> usages : usagesPerKey.values()) {
            long heldTimeInMillis = 0;
            for (UsageHostDeviceVO usage : usages) {
                heldTimeInMillis += getHeldTimeInPeriod(usage, startDate, endDate);
            }

            if (heldTimeInMillis > 0L) {
                // All the entries of the group share the VM, the device and the tag, so the first one can represent the group.
                createUsageRecord(usages.get(0), heldTimeInMillis, startDate, endDate, account);
            }
        }

        return true;
    }

    /**
     * Calculates how long the VM held the host device inside the parsing period. The time is inclusive of both ends, as in the other parsers.
     */
    private long getHeldTimeInPeriod(UsageHostDeviceVO usage, Date startDate, Date endDate) {
        Date reservedDate = usage.getCreated().before(startDate) ? startDate : usage.getCreated();
        Date releasedDate = usage.getRemoved() == null || usage.getRemoved().after(endDate) ? endDate : usage.getRemoved();

        if (reservedDate.after(endDate)) {
            return 0L;
        }

        return (releasedDate.getTime() - reservedDate.getTime()) + 1;
    }

    private void createUsageRecord(UsageHostDeviceVO usage, long heldTimeInMillis, Date startDate, Date endDate, AccountVO account) {
        float usageInHours = heldTimeInMillis / 1000f / 60f / 60f;
        String usageDisplay = new DecimalFormat("#.######").format(usageInHours);

        logger.debug("Creating host device usage record for host device [{}], VM [{}], usage [{}], startDate [{}], and endDate [{}], for account [{}].",
                usage.getHostDeviceId(), usage.getVmInstanceId(), usageDisplay, DateUtil.displayDateInTimezone(UsageManagerImpl.getUsageAggregationTimeZone(), startDate),
                DateUtil.displayDateInTimezone(UsageManagerImpl.getUsageAggregationTimeZone(), endDate), account.getId());

        String description = String.format("Host device: %d held by VM: %d usage time", usage.getHostDeviceId(), usage.getVmInstanceId());

        UsageVO usageRecord = new UsageVO(usage.getZoneId(), account.getId(), account.getDomainId(), description, usageDisplay + " Hrs", UsageTypes.HOST_DEVICE,
                (double) usageInHours, usage.getVmInstanceId(), null, null, null, usage.getHostDeviceId(), startDate, endDate, usage.getDeviceTag());
        usageDao.persist(usageRecord);
    }

    /**
     * Identifies a usage record: the VM that held the host device, the device and the tag it had when it was reserved.
     */
    private static class HostDeviceUsageKey {
        private final long vmInstanceId;
        private final long hostDeviceId;
        private final String deviceTag;

        HostDeviceUsageKey(UsageHostDeviceVO usage) {
            this.vmInstanceId = usage.getVmInstanceId();
            this.hostDeviceId = usage.getHostDeviceId();
            this.deviceTag = usage.getDeviceTag();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            HostDeviceUsageKey other = (HostDeviceUsageKey) obj;
            return vmInstanceId == other.vmInstanceId && hostDeviceId == other.hostDeviceId && Objects.equals(deviceTag, other.deviceTag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(vmInstanceId, hostDeviceId, deviceTag);
        }
    }
}
