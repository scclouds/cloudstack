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

package org.apache.cloudstack.hostdevices;

import com.cloud.hostdevices.DeviceOfferingDeviceTagVO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeviceOfferingHelper {
    public static int countOfferingTagsAmount(Map<String, Integer> offeringsTags) {
        return offeringsTags
                .values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    public static Map<String, Integer> getDeviceOfferingToAmountMap(List<DeviceOfferingDeviceTagVO> offeringTags) {
        return offeringTags
                .stream().collect(Collectors.toMap(DeviceOfferingDeviceTagVO::getDeviceTag, DeviceOfferingDeviceTagVO::getAmount, Integer::sum));
    }

    public static Map<String, Integer> getExceedingTagAmounts(Map<String, Integer> tagAmounts, Map<String, Integer> baseline) {
        Map<String, Integer> difference = new HashMap<>();

        tagAmounts.forEach((tag, amount) -> {
            int remainingAmount = amount - baseline.getOrDefault(tag, 0);

            if (remainingAmount > 0) {
                difference.put(tag, remainingAmount);
            }
        });

        return difference;
    }
}
