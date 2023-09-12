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

package org.apache.cloudstack.quota.activationrule.presetvariables;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class ProcessedData extends GenericPresetVariable {

    @PresetVariableDefinition(description = "The end date of the processed data.")
    private Date endDate;
    @PresetVariableDefinition(description = "The aggregation of all Quota tariffs applied to the resource during the period.")
    private BigDecimal aggregatedTariffsValue;
    @PresetVariableDefinition(description = "A list of objects containing the ID and value of the applied tariff.")
    private List<Tariff> tariffs;
    @PresetVariableDefinition(description = "The start date of the processed data.")
    private Date startDate;
    @PresetVariableDefinition(description = "The resource's usage during the period.")
    private Double usageValue;

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
        fieldNamesToIncludeInToString.add("endDate");
    }

    public void setAggregatedTariffsValue(BigDecimal aggregatedTariffsValue) {
        this.aggregatedTariffsValue = aggregatedTariffsValue;
        fieldNamesToIncludeInToString.add("aggregatedTariffsValue");
    }

    public List<Tariff> getTariffs() {
        return tariffs;
    }

    public void setTariffs(List<Tariff> tariffs) {
        this.tariffs = tariffs;
        fieldNamesToIncludeInToString.add("tariffs");
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
        fieldNamesToIncludeInToString.add("startDate");
    }

    public void setUsageValue(Double usageValue) {
        this.usageValue = usageValue;
        fieldNamesToIncludeInToString.add("usageValue");
    }
}
