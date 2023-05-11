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
import java.util.List;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;

public class QuotaBalanceResponse extends BaseResponse {

    @SerializedName("currency")
    @Param(description = "Balance's currency")
    private String currency;

    @SerializedName("date")
    @Param(description = "Balance's date.")
    private Date date;

    @SerializedName("balance")
    @Param(description = "Balance's value.")
    private BigDecimal balance;

    @SerializedName("balance")
    @Param(description = "Balances in the period.")
    private List<QuotaBalanceResponse> balances;

    public QuotaBalanceResponse() {
    }

    public QuotaBalanceResponse(Date date, BigDecimal balance) {
        this.date = date;
        this.balance = balance;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setBalances(List<QuotaBalanceResponse> balances) {
        this.balances = balances;
    }

    public String getCurrency() {
        return currency;
    }

    public List<QuotaBalanceResponse> getBalances() {
        return balances;
    }

    public Date getDate() {
        return date;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
