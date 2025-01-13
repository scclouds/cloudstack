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
import java.util.List;

public class QuotaTariffStatementResponse extends BaseResponse {
    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "account id")
    private String accountId;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "account name")
    private String accountName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "domain id")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "domain path")
    private String domain;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "project id")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "project name")
    private String project;

    @SerializedName("quotausage")
    @Param(description = "list of quota usage grouped by tariff", responseObject = QuotaTariffStatementItemResponse.class)
    private List<QuotaTariffStatementItemResponse> quotaUsage;

    @SerializedName("currency")
    @Param(description = "currency")
    private String currency;

    @SerializedName("totalquotaused")
    @Param(description = "total quota used during this period")
    private BigDecimal totalQuotaUsed;

    @SerializedName("startdate")
    @Param(description = "start date")
    private Date startDate = null;

    @SerializedName("enddate")
    @Param(description = "end date")
    private Date endDate = null;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public List<QuotaTariffStatementItemResponse> getQuotaUsage() {
        return quotaUsage;
    }

    public void setQuotaUsage(List<QuotaTariffStatementItemResponse> quotaUsage) {
        this.quotaUsage = quotaUsage;
    }

    public BigDecimal getTotalQuotaUsed() {
        return totalQuotaUsed;
    }

    public void setTotalQuotaUsed(BigDecimal totalQuotaUsed) {
        this.totalQuotaUsed = totalQuotaUsed;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}
