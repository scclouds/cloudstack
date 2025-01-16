/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cloudstack.quota.vo;

import java.util.Map;

import org.apache.cloudstack.quota.constant.ProcessingPeriod;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QuotaTariffVOTest {

    @Test
    public void setUsageTypeDataTestSetAllDataAndReturnTrueToAllexistingQuotaType() {
        QuotaTariffVO quotaTariffVoTest = new QuotaTariffVO();

        for (Map.Entry<Integer, QuotaTypes> quotaType: QuotaTypes.listQuotaTypes().entrySet()) {
            boolean result = quotaTariffVoTest.setUsageTypeData(quotaType.getKey());

            Assert.assertTrue(result);
            Assert.assertEquals((int) quotaType.getKey(), quotaTariffVoTest.getUsageType());
            Assert.assertEquals(quotaType.getValue().getQuotaName(), quotaTariffVoTest.getUsageName());
            Assert.assertEquals(quotaType.getValue().getQuotaUnit(), quotaTariffVoTest.getUsageUnit());
            Assert.assertEquals(quotaType.getValue().getDiscriminator(), quotaTariffVoTest.getUsageDiscriminator());
        }
    }

    @Test
    public void setUsageTypeDataTestReturnFalseToInvalidUsageType() {
        QuotaTariffVO quotaTariffVoTest = new QuotaTariffVO();

        boolean result = quotaTariffVoTest.setUsageTypeData(0);

        Assert.assertFalse(result);
    }

    @Test
    public void setExecuteOnTestByEntryPeriodAndNotNullExecuteOnReturnFalse() {
        QuotaTariffVO quotaTariffVoTest = new QuotaTariffVO();

        quotaTariffVoTest.setProcessingPeriod(ProcessingPeriod.BY_ENTRY);
        boolean result = quotaTariffVoTest.setExecuteOn(0);

        Assert.assertTrue(result);
    }

    @Test
    public void setExecuteOnTestByEntryPeriodAndNullExecuteOnReturnTrue() {
        QuotaTariffVO quotaTariffVoTest = new QuotaTariffVO();

        quotaTariffVoTest.setProcessingPeriod(ProcessingPeriod.BY_ENTRY);
        boolean result = quotaTariffVoTest.setExecuteOn(null);

        Assert.assertTrue(result);
    }

    @Test
    public void setExecuteOnTestMonthlyPeriodAndNullExecuteOnReturnFalse() {
        QuotaTariffVO quotaTariffVoTest = new QuotaTariffVO();

        quotaTariffVoTest.setProcessingPeriod(ProcessingPeriod.MONTHLY);
        boolean result = quotaTariffVoTest.setExecuteOn(null);

        Assert.assertFalse(result);
    }

    @Test
    public void setExecuteOnTestMonthlyPeriodAndExecuteOnLessThan1ReturnFalse() {
        QuotaTariffVO quotaTariffVoTest = new QuotaTariffVO();

        quotaTariffVoTest.setProcessingPeriod(ProcessingPeriod.MONTHLY);
        boolean result = quotaTariffVoTest.setExecuteOn(0);

        Assert.assertFalse(result);
    }

    @Test
    public void setExecuteOnTestMonthlyPeriodAndExecuteOnMoreThan28ReturnFalse() {
        QuotaTariffVO quotaTariffVoTest = new QuotaTariffVO();

        quotaTariffVoTest.setProcessingPeriod(ProcessingPeriod.MONTHLY);
        boolean result = quotaTariffVoTest.setExecuteOn(29);

        Assert.assertFalse(result);
    }

    @Test
    public void setExecuteOnTestMonthlyPeriodAndExecuteOnReturnTrue() {
        QuotaTariffVO quotaTariffVoTest = new QuotaTariffVO();

        quotaTariffVoTest.setProcessingPeriod(ProcessingPeriod.MONTHLY);
        boolean result = quotaTariffVoTest.setExecuteOn(10);

        Assert.assertTrue(result);
    }

}
