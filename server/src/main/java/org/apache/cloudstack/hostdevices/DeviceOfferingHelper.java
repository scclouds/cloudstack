package org.apache.cloudstack.hostdevices;

import com.cloud.hostdevices.DeviceOfferingDeviceTagVO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeviceOfferingHelper {
    public static int countOfferingTagsAmount(Map<String, Integer> offeringsTags) {
        return offeringsTags
                .values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    public static Map<String, Integer> getDeviceOfferingToAmountMap(List<DeviceOfferingDeviceTagVO> offeringTags) {
        return offeringTags
                .stream().collect(Collectors.toMap(DeviceOfferingDeviceTagVO::getDeviceTag, DeviceOfferingDeviceTagVO::getAmount, Integer::sum));
    }
}
