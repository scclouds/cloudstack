package com.cloud.hostdevices.dao;

import com.cloud.hostdevices.DeviceOfferingDeviceTagVO;
import com.cloud.hostdevices.DeviceOfferingVO;
import com.cloud.hostdevices.VMInstanceDeviceOfferingsVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.hostdevices.DeviceOffering;
import org.apache.commons.collections.CollectionUtils;
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
    private SearchBuilder<DeviceOfferingVO> deviceOfferingWithTagsSearch;
    private SearchBuilder<DeviceOfferingVO> deviceOfferingWithVMSearch;

    @PostConstruct
    private void init() {
        deviceOfferingSearch = getBaseSearchBuilder();
        deviceOfferingSearch.done();

        deviceOfferingWithVMSearch = getBaseSearchBuilder();
        SearchBuilder<VMInstanceDeviceOfferingsVO> vmSearchBuilder = vmDeviceOfferingsDao.createSearchBuilder();
        vmSearchBuilder.and("virtualMachineId", vmSearchBuilder.entity().getVirtualMachineId(), SearchCriteria.Op.EQ);
        deviceOfferingWithVMSearch.join("vmSearch", vmSearchBuilder, deviceOfferingWithVMSearch.entity().getId(), vmSearchBuilder.entity().getDeviceOfferingId(), JoinBuilder.JoinType.INNER);
        deviceOfferingWithVMSearch.done();

        deviceOfferingWithTagsSearch = getBaseSearchBuilder();
        SearchBuilder<DeviceOfferingDeviceTagVO> deviceTagSearchBuilder = deviceOfferingDeviceTagDao.createSearchBuilder();
        deviceTagSearchBuilder.and("deviceTag", deviceTagSearchBuilder.entity().getDeviceTag(), SearchCriteria.Op.IN);
        deviceOfferingWithTagsSearch.join("deviceTagSearch", deviceTagSearchBuilder, deviceOfferingWithTagsSearch.entity().getId(), deviceTagSearchBuilder.entity().getDeviceOfferingId(), JoinBuilder.JoinType.INNER);
        deviceOfferingWithTagsSearch.done();
    }

    @Override
    public List<DeviceOfferingVO> listVirtualMachineDeviceOfferings(Long virtualMachineId) {
        SearchCriteria<DeviceOfferingVO> sc = deviceOfferingWithVMSearch.create();
        sc.setJoinParametersIfNotNull("vmSearch", "virtualMachineId", virtualMachineId);
        return listBy(sc);
    }

    @Override
    public List<DeviceOfferingVO> listDeviceOfferings(String name, List<Long> domainIds, Long zoneId, List<String> deviceTags, DeviceOffering.State state, Boolean showOnlyPublic) {
        SearchCriteria<DeviceOfferingVO> sc = CollectionUtils.isEmpty(deviceTags) ? deviceOfferingSearch.create() : deviceOfferingWithTagsSearch.create();
        sc.setParametersIfNotNull("name", name);
        sc.setParametersIfNotNull("zoneId", zoneId);
        sc.setParametersIfNotNull("state", state);
        sc.setParametersIfNotNull("isPublic", showOnlyPublic);

        if (domainIds != null && !domainIds.isEmpty()) {
            sc.setParametersIfNotNull("domainIds", domainIds.toArray());
        }

        if (!CollectionUtils.isEmpty(deviceTags)) {
            sc.setJoinParametersIfNotNull("deviceTagSearch", "deviceTag", deviceTags.toArray());
        }

        return listBy(sc);
    }

    @Override
    public DeviceOfferingVO findByName(String name) {
        SearchCriteria<DeviceOfferingVO> sc = deviceOfferingSearch.create();
        sc.setParametersIfNotNull("name", name);
        return findOneBy(sc);
    }

    @Override
    public List<String> listDeviceOfferingTags(Long deviceOfferingId) {
        return deviceOfferingDeviceTagDao.getDeviceOfferingTags(deviceOfferingId);
    }

    private SearchBuilder<DeviceOfferingVO> getBaseSearchBuilder() {
        SearchBuilder<DeviceOfferingVO> sc = createSearchBuilder();

        sc.and("name", sc.entity().getName(), SearchCriteria.Op.EQ);
        sc.and("domainIds", sc.entity().getDomainId(), SearchCriteria.Op.IN);
        sc.and("zoneId", sc.entity().getZoneId(), SearchCriteria.Op.EQ);
        sc.and("state", sc.entity().getState(), SearchCriteria.Op.EQ);
        sc.and("isPublic", sc.entity().getIsPublic(), SearchCriteria.Op.EQ);

        return sc;
    }
}
