package com.cloud.hostdevices.dao;

import com.cloud.hostdevices.DeviceOfferingDeviceTagVO;
import com.cloud.hostdevices.DeviceOfferingVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.hostdevices.DeviceOffering;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DeviceOfferingDeviceTagDaoImpl extends GenericDaoBase<DeviceOfferingDeviceTagVO, Long> implements DeviceOfferingDeviceTagDao {
    private final SearchBuilder<DeviceOfferingDeviceTagVO> deviceOfferingDeviceTagSearch;

    public DeviceOfferingDeviceTagDaoImpl() {
        deviceOfferingDeviceTagSearch = createSearchBuilder();
        deviceOfferingDeviceTagSearch.and("deviceOfferingId", deviceOfferingDeviceTagSearch.entity().getDeviceOfferingId(), SearchCriteria.Op.IN);
        deviceOfferingDeviceTagSearch.done();
    }

    public List<String> getDeviceOfferingsTags(List<DeviceOfferingVO> deviceOfferings) {
        List<Long> ids = deviceOfferings.stream().map(DeviceOffering::getId).collect(Collectors.toList());
        SearchCriteria<DeviceOfferingDeviceTagVO> sc = deviceOfferingDeviceTagSearch.create();
        if (!ids.isEmpty()) {
            sc.setParameters("deviceOfferingId", ids.toArray());
        }

        return listBy(sc).stream().map(DeviceOfferingDeviceTagVO::getDeviceTag).collect(Collectors.toList());
    }
}
