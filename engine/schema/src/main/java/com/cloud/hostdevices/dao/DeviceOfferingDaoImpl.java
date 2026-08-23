package com.cloud.hostdevices.dao;

import com.cloud.hostdevices.DeviceOfferingVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;

@Component
public class DeviceOfferingDaoImpl extends GenericDaoBase<DeviceOfferingVO, Long> implements DeviceOfferingDao {
    @Inject
    private VMInstanceDeviceOfferingsDao vmDeviceOfferingsDao;

    private SearchBuilder<DeviceOfferingVO> deviceOfferingSearch;

    @PostConstruct
    private void init() {
        deviceOfferingSearch = createSearchBuilder();

        SearchBuilder<com.cloud.hostdevices.VMInstanceDeviceOfferingsVO> vmSearchBuilder = vmDeviceOfferingsDao.createSearchBuilder();
        vmSearchBuilder.and("virtualMachineId", vmSearchBuilder.entity().getVirtualMachineId(), SearchCriteria.Op.EQ);
        deviceOfferingSearch.join("vmSearch", vmSearchBuilder, deviceOfferingSearch.entity().getId(), vmSearchBuilder.entity().getDeviceOfferingId(), JoinBuilder.JoinType.INNER);

        deviceOfferingSearch.done();
    }

    @Override
    public List<DeviceOfferingVO> listVirtualMachineDeviceOfferings(Long virtualMachineId) {
        SearchCriteria<DeviceOfferingVO> sc = deviceOfferingSearch.create();
        sc.setJoinParametersIfNotNull("vmSearch", "virtualMachineId", virtualMachineId);
        return listBy(sc);
    }
}
