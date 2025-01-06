//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.BaseResponse;

import java.math.BigDecimal;
import java.util.List;

public class QuotaUsageDetailsResponse extends BaseResponse {

    @SerializedName("resourcename")
    @Param(description = "The name of the resource.")
    private String resourceName;

    @SerializedName("resourceid")
    @Param(description = "The ID of the resource.")
    private String resourceId;

    @SerializedName("usagename")
    @Param(description = "The name of the usage type.")
    private String usageName;

    @SerializedName("unit")
    @Param(description = "The unit of the usage type.")
    private String unit;

    @SerializedName("items")
    @Param(description = "The list of quota usage details.", responseObject = QuotaUsageDetailsItemResponse.class)
    private List<QuotaUsageDetailsItemResponse> items;

    @SerializedName("totalquotaused")
    @Param(description = "Total amount of quota used.")
    private BigDecimal totalQuotaUsed;

    public QuotaUsageDetailsResponse() {
        super();
        this.setObjectName("quotausagedetails");
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public void setUsageName(String usageName) {
        this.usageName = usageName;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setQuotaUsageDetails(List<QuotaUsageDetailsItemResponse> items) {
        this.items = items;
    }

    public void setTotalQuotaUsed(BigDecimal totalQuotaUsed) {
        this.totalQuotaUsed = totalQuotaUsed;
    }

}
