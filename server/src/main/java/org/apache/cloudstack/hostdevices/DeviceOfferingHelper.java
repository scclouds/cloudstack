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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hostdevices.DeviceOfferingDeviceTagVO;
import com.cloud.utils.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DeviceOfferingHelper {
    private static final Logger logger = LogManager.getLogger(DeviceOfferingHelper.class);

    private static final int MAX_DEVICE_TAG_LENGTH = 255;
    private static final Pattern DEVICE_TAG_NAME_PATTERN = Pattern.compile("[A-Za-z0-9]+");
    private static final Pattern DEVICE_TAG_PATTERN = Pattern.compile("([^:]+):([0-9]+)");

    public static Pair<String, Integer> parseDeviceTag(String tag) {
        Matcher matcher = DEVICE_TAG_PATTERN.matcher(tag);

        if (!matcher.matches()) {
            logger.error("Invalid device tag [{}]. The expected format is tag:amount.", tag);
            throw new InvalidParameterValueException(String.format("Invalid device tag: %s. The expected format is tag:amount.", tag));
        }

        String tagName = matcher.group(1).trim();
        validateDeviceTagName(tagName);

        return new Pair<>(tagName, Integer.parseInt(matcher.group(2)));
    }

    public static void validateDeviceTagName(String tagName) {
        if (!DEVICE_TAG_NAME_PATTERN.matcher(tagName).matches()) {
            logger.error("Invalid device tag [{}]. Device tags may contain only letters and numbers.", tagName);
            throw new InvalidParameterValueException(String.format("Invalid device tag: %s. Device tags may contain only letters and numbers.", tagName));
        }

        if (tagName.length() > MAX_DEVICE_TAG_LENGTH) {
            logger.error("Invalid device tag [{}]. The tag name is longer than {} characters.", tagName, MAX_DEVICE_TAG_LENGTH);
            throw new InvalidParameterValueException(String.format("Invalid device tag: %s. The tag name cannot be longer than %d characters.", tagName, MAX_DEVICE_TAG_LENGTH));
        }
    }

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
