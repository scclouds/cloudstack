package com.cloud.hostdevices.dao;

import com.cloud.hostdevices.VMInstanceDeviceOfferingsVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VMInstanceDeviceOfferingsDaoImpl extends GenericDaoBase<VMInstanceDeviceOfferingsVO, Long> implements VMInstanceDeviceOfferingsDao {
    private final SearchBuilder<VMInstanceDeviceOfferingsVO> searchBuilder;

    public VMInstanceDeviceOfferingsDaoImpl() {
        searchBuilder = createSearchBuilder();
        searchBuilder.and("deviceOfferingId", searchBuilder.entity().getDeviceOfferingId(), SearchCriteria.Op.EQ);
        searchBuilder.and("virtualMachineId", searchBuilder.entity().getVirtualMachineId(), SearchCriteria.Op.EQ);
        searchBuilder.done();
    }

    @Override
    public List<VMInstanceDeviceOfferingsVO> listByVmId(Long virtualMachineId) {
        SearchCriteria<VMInstanceDeviceOfferingsVO> sc = searchBuilder.create();
        sc.setParameters("virtualMachineId", virtualMachineId);
        return listBy(sc);
    }

    @Override
    public VMInstanceDeviceOfferingsVO findByVmIdAndDeviceId(Long vmId, Long deviceId) {
        SearchCriteria<VMInstanceDeviceOfferingsVO> sc = searchBuilder.create();
        sc.setParameters("virtualMachineId", vmId);
        sc.setParameters("deviceOfferingId", deviceId);
        return findOneBy(sc);
    }

    @Override
    public void expungeByVmId(Long vmId) {
        SearchCriteria<VMInstanceDeviceOfferingsVO> sc = searchBuilder.create();
        sc.setParameters("virtualMachineId", vmId);
        expunge(sc);
    }
}
