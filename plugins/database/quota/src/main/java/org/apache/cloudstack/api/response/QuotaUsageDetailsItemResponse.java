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

import java.math.BigDecimal;
import java.util.Date;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class QuotaUsageDetailsItemResponse extends BaseResponse {

    @SerializedName("tariffid")
    @Param(description = "The ID of the tariff.")
    private String tariffId;

    @SerializedName("tariffname")
    @Param(description = "The name of the tariff.")
    private String tariffName;

    @SerializedName("quotaused")
    @Param(description = "The amount of quota used.")
    private BigDecimal quotaUsed;

    @SerializedName(ApiConstants.START_DATE)
    @Param(description = "Item's start date.")
    private Date startDate;

    @SerializedName(ApiConstants.END_DATE)
    @Param(description = "Item's end date.")
    private Date endDate;


    public QuotaUsageDetailsItemResponse() {
        super();
        this.setObjectName("quotausagedetailsitem");
    }

    public void setTariffId(String tariffId) {
        this.tariffId = tariffId;
    }

    public void setTariffName(String tariffName) {
        this.tariffName = tariffName;
    }

    public void setQuotaUsed(BigDecimal quotaUsed) {
        this.quotaUsed = quotaUsed;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}
