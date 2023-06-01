//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
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
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.ResourcesToQuoteVO;
import org.apache.cloudstack.utils.jsinterpreter.JsInterpreter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface QuotaManager extends Manager {

    boolean calculateQuotaUsage();

    boolean isLockable(AccountVO account);

    Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> createMapQuotaTariffsPerUsageType(Set<Integer> usageTypes);

    Map<Integer, List<QuotaTariffVO>> getValidTariffsForQuoting(Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> allTariffsOfTheInformedTypes);

    BigDecimal getResourceRating(JsInterpreter jsInterpreter, ResourcesToQuoteVO resourceToQuote, List<QuotaTariffVO> tariffs, QuotaTypes quotaTypeObject, Date date)
            throws IllegalAccessException;
}
