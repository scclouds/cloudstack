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
package org.apache.cloudstack.usage;

import com.cloud.utils.Pair;
import org.apache.cloudstack.api.command.admin.usage.GenerateUsageRecordsCmd;
import org.apache.cloudstack.api.command.admin.usage.ListUsageRecordsCmd;
import org.apache.cloudstack.api.command.admin.usage.RemoveRawUsageRecordsCmd;
import org.apache.cloudstack.api.response.UsageTypeResponse;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.List;

public interface UsageService {
    ConfigKey<String> UsageTimeZone = new ConfigKey<>("Usage", String.class, "usage.timezone", "GMT", "The timezone that will be used in the Usage plugin for " +
            "executing the usage job and aggregating the stats. The dates in logs in those processes will be presented according to this configuration.", false);

    /**
     * Generate Billing Records from the last time it was generated to the
     * time specified.
     *
     * @param cmd the command wrapping the generate parameters
     *   - userId unique id of the user, pass in -1 to generate billing records
     *            for all users
     *   - startDate
     *   - endDate inclusive.  If date specified is greater than the current time, the
     *             system will use the current time.
     */
    boolean generateUsageRecords(GenerateUsageRecordsCmd cmd);

    /**
     * Retrieves all Usage Records generated between the start and end date specified
     *
     * @param userId unique id of the user, pass in -1 to retrieve billing records
     *        for all users
     * @param startDate inclusive.
     * @param endDate inclusive.  If date specified is greater than the current time, the
     *                system will use the current time.
     * @param page The page of usage records to see (500 results are returned at a time, if
     *             more than 500 records exist then additional results can be retrieved by
     *             the appropriate page number)
     * @return a list of usage records
     */
    Pair<List<? extends Usage>, Integer> getUsageRecords(ListUsageRecordsCmd cmd);

    boolean removeRawUsageRecords(RemoveRawUsageRecordsCmd cmd);

    List<UsageTypeResponse> listUsageTypes();
}
