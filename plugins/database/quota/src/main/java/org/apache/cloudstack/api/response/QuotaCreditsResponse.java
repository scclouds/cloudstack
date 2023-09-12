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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.math.BigDecimal;
import java.util.Date;

public class QuotaCreditsResponse extends BaseResponse {

    @SerializedName("credit")
    @Param(description = "The credit deposited.")
    private BigDecimal credit;

    @SerializedName("creditorid")
    @Param(description = "Account creditor's id.")
    private String accountCreditorId;

    @SerializedName("creditorname")
    @Param(description = "Account creditor's name.")
    private String accountCreditorName;

    @SerializedName("creditedon")
    @Param(description = "When the credit was added.")
    private Date creditedOn;

    @SerializedName("currency")
    @Param(description = "Credit's currency.")
    private String currency;

    @SerializedName(ApiConstants.POSTING_DATE)
    @Param(description = "Posting date of the payment.")
    private Date postingDate;

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public void setAccountCreditorId(String accountCreditorId) {
        this.accountCreditorId = accountCreditorId;
    }

    public void setAccountCreditorName(String accountCreditorName) {
        this.accountCreditorName = accountCreditorName;
    }

    public void setCreditedOn(Date creditedOn) {
        this.creditedOn = creditedOn;
    }

    public void setPostingDate(Date postingDate) {
        this.postingDate = postingDate;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getAccountCreditorId() {
        return accountCreditorId;
    }

    public String getAccountCreditorName() {
        return accountCreditorName;
    }

    public Date getCreditedOn() {
        return creditedOn;
    }

    public String getCurrency() {
        return currency;
    }

    public Date getPostingDate() {
        return postingDate;
    }
}
