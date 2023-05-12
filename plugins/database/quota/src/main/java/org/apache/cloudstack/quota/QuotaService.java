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
package org.apache.cloudstack.quota;

import com.cloud.user.AccountVO;
import com.cloud.utils.component.PluggableService;

import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaUsageJoinVO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface QuotaService extends PluggableService {

    List<QuotaUsageJoinVO> getQuotaUsage(Long accountId, String accountName, Long domainId, Integer usageType, Date startDate, Date endDate);

    List<QuotaBalanceVO> listQuotaBalancesForAccount(Long accountId, String accountName, Long domainId, Date startDate, Date endDate);

    void setLockAccount(Long accountId, Boolean state);

    void setMinBalance(Long accountId, Double balance);

    Boolean isQuotaServiceEnabled();

    boolean saveQuotaAccount(AccountVO account, BigDecimal aggrUsage, Date endDate);

}
