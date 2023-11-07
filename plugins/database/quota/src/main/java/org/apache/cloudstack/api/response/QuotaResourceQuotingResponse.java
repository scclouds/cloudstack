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
import java.math.RoundingMode;
import java.util.List;

public class QuotaResourceQuotingResponse extends BaseResponse {

    @SerializedName("totalquote")
    @Param(description = "Total quote")
    private BigDecimal totalQuote;

    @SerializedName(ApiConstants.DETAILS)
    @Param(description = "Quoting by the IDs sent as API parameter")
    private List<ResourcesQuotingResultResponse> details;

    @SerializedName("currencysymbol")
    @Param(description = "The symbol for the currency in use to measure usage")
    private String currencySymbol;

    @SerializedName("currencylocale")
    @Param(description = "The location used for formatting the value (e.g. \"en-US\" for English or \"pt-BR\" for Brazilian Portuguese)")
    private String currencyLocale;

    public BigDecimal getTotalQuote() {
        return totalQuote;
    }

    public void setTotalQuote(BigDecimal totalQuote) {
        this.totalQuote = totalQuote.setScale(2, RoundingMode.HALF_EVEN);;
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    public void setCurrencyLocale(String currencyLocale) {
        this.currencyLocale = currencyLocale;
    }

    public List<ResourcesQuotingResultResponse> getDetails() {
        return details;
    }

    public void setDetails(List<ResourcesQuotingResultResponse> details) {
        this.details = details;
    }

    public QuotaResourceQuotingResponse() {
        super("quoting");
    }
}
