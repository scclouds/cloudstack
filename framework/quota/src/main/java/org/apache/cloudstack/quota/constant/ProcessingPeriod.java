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
package org.apache.cloudstack.quota.constant;


import com.cloud.exception.InvalidParameterValueException;

import java.util.Arrays;

public enum ProcessingPeriod {
    BY_ENTRY,
    MONTHLY;

    public static ProcessingPeriod getProcessingPeriodByString(String period) {
        for (ProcessingPeriod type : ProcessingPeriod.values()) {
            if (type.toString().equalsIgnoreCase(period)) {
                return type;
            }
        }

        throw new InvalidParameterValueException(String.format("Invalid value [%s]; it must be one of the following values [%s].", period,
            Arrays.toString(ProcessingPeriod.values())));
    }
}
