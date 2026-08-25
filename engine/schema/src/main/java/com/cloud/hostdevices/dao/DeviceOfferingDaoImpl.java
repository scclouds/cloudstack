package com.cloud.hostdevices.dao;

import com.cloud.hostdevices.DeviceOfferingDeviceTagVO;
import com.cloud.hostdevices.DeviceOfferingVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.hostdevices.DeviceOffering;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;

@Component
public class DeviceOfferingDaoImpl extends GenericDaoBase<DeviceOfferingVO, Long> implements DeviceOfferingDao {
    @Inject
    private VMInstanceDeviceOfferingsDao vmDeviceOfferingsDao;
    @Inject
    private DeviceOfferingDeviceTagDao deviceOfferingDeviceTagDao;

    private SearchBuilder<DeviceOfferingVO> deviceOfferingSearch;

    @PostConstruct
    private void init() {
        deviceOfferingSearch = createSearchBuilder();
        deviceOfferingSearch.and("name", deviceOfferingSearch.entity().getName(), SearchCriteria.Op.EQ);
        deviceOfferingSearch.and("domainIds", deviceOfferingSearch.entity().getDomainId(), SearchCriteria.Op.IN);
        deviceOfferingSearch.and("zoneId", deviceOfferingSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        deviceOfferingSearch.and("state", deviceOfferingSearch.entity().getState(), SearchCriteria.Op.EQ);
        deviceOfferingSearch.and("isPublic", deviceOfferingSearch.entity().getIsPublic(), SearchCriteria.Op.EQ);

//        SearchBuilder<VMInstanceDeviceOfferingsVO> vmSearchBuilder = vmDeviceOfferingsDao.createSearchBuilder();
//        vmSearchBuilder.and("virtualMachineId", vmSearchBuilder.entity().getVirtualMachineId(), SearchCriteria.Op.EQ);
//        deviceOfferingSearch.join("vmSearch", vmSearchBuilder, deviceOfferingSearch.entity().getId(), vmSearchBuilder.entity().getDeviceOfferingId(), JoinBuilder.JoinType.INNER);
//
        SearchBuilder<DeviceOfferingDeviceTagVO> deviceTagSearchBuilder = deviceOfferingDeviceTagDao.createSearchBuilder();
        deviceTagSearchBuilder.and("deviceTag", deviceTagSearchBuilder.entity().getDeviceTag(), SearchCriteria.Op.IN);
        deviceOfferingSearch.join("deviceTagSearch", deviceTagSearchBuilder, deviceOfferingSearch.entity().getId(), deviceTagSearchBuilder.entity().getDeviceOfferingId(), JoinBuilder.JoinType.INNER);

        deviceOfferingSearch.done();
    }

    @Override
    public List<DeviceOfferingVO> listVirtualMachineDeviceOfferings(Long virtualMachineId) {
        SearchCriteria<DeviceOfferingVO> sc = deviceOfferingSearch.create();
        sc.setJoinParametersIfNotNull("vmSearch", "virtualMachineId", virtualMachineId);
        return listBy(sc);
    }

    @Override
    public List<DeviceOfferingVO> listDeviceOfferings(String name, List<Long> domainIds, Long zoneId, List<String> deviceTags, DeviceOffering.State state, boolean showOnlyPublic) {
        SearchCriteria<DeviceOfferingVO> sc = deviceOfferingSearch.create();
        sc.setParametersIfNotNull("name", name);
        sc.setParametersIfNotNull("zoneId", zoneId);
        sc.setParametersIfNotNull("state", state);
        sc.setParametersIfNotNull("isPublic", showOnlyPublic);

        if (domainIds != null && !domainIds.isEmpty()) {
            sc.setParametersIfNotNull("domainIds", domainIds.toArray());
        }

        if (deviceTags != null && !deviceTags.isEmpty()) {
            sc.setJoinParametersIfNotNull("deviceTagSearch", "deviceTag", deviceTags.toArray());
        }

        return listBy(sc);
    }
}
