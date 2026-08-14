package org.apache.cloudstack.hostdevices.persistence;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HostDeviceDaoImpl extends GenericDaoBase<HostDeviceVO, Long> implements HostDeviceDao  {
    public static final String HOST_ID = "hostId";

    private final SearchBuilder<HostDeviceVO> hostIdSearch;

    public HostDeviceDaoImpl() {
        hostIdSearch = createSearchBuilder();
        hostIdSearch.and(HOST_ID, hostIdSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        hostIdSearch.done();
    }

    @Override
    public List<HostDeviceVO> listHostDevicesByHostId(Long hostId) {
        SearchCriteria<HostDeviceVO> sc = hostIdSearch.create();
        sc.setParameters(HOST_ID, hostId);
        return listBy(sc);
    }
}
