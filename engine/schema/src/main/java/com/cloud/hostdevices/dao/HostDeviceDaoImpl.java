package com.cloud.hostdevices.dao;

import com.cloud.hostdevices.HostDeviceVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.hostdevices.HostDevice;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HostDeviceDaoImpl extends GenericDaoBase<HostDeviceVO, Long> implements HostDeviceDao  {
    public static final String ID = "id";
    public static final String ACCOUNT_ID = "accountId";
    public static final String DOMAIN_ID = "domainId";
    public static final String HOST_ID = "hostId";
    public static final String VIRTUAL_MACHINE_ID = "virtualMachineId";
    public static final String STATE = "state";
    public static final String OR_STATE = "orState";
    public static final String TYPE = "type";
    public static final String DEVICE_TAG = "deviceTag";
    public static final String DEVICE_TAG_IN = "deviceTagIn";

    private final SearchBuilder<HostDeviceVO> hostIdSearch;
    private final SearchBuilder<HostDeviceVO> hostDevicesSearch;
    private final SearchBuilder<HostDeviceVO> hostDevicesAvailableForAllocationSearch;

    public HostDeviceDaoImpl() {
        hostDevicesSearch = createSearchBuilder();
        hostDevicesSearch.and(ID, hostDevicesSearch.entity().getId(), SearchCriteria.Op.EQ);
        hostDevicesSearch.and(ACCOUNT_ID, hostDevicesSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        hostDevicesSearch.and(DOMAIN_ID, hostDevicesSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        hostDevicesSearch.and(HOST_ID, hostDevicesSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        hostDevicesSearch.and(VIRTUAL_MACHINE_ID, hostDevicesSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        hostDevicesSearch.and(STATE, hostDevicesSearch.entity().getState(), SearchCriteria.Op.EQ);
        hostDevicesSearch.and(TYPE, hostDevicesSearch.entity().getType(), SearchCriteria.Op.EQ);
        hostDevicesSearch.and(DEVICE_TAG, hostDevicesSearch.entity().getDeviceTag(), SearchCriteria.Op.EQ);
        hostDevicesSearch.done();

        hostIdSearch = createSearchBuilder();
        hostIdSearch.and(HOST_ID, hostIdSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        hostIdSearch.done();

        hostDevicesAvailableForAllocationSearch = createSearchBuilder();
        hostDevicesAvailableForAllocationSearch.and(HOST_ID, hostDevicesAvailableForAllocationSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        hostDevicesAvailableForAllocationSearch.and().op(STATE, hostDevicesAvailableForAllocationSearch.entity().getState(), SearchCriteria.Op.EQ);
        hostDevicesAvailableForAllocationSearch.or(VIRTUAL_MACHINE_ID, hostDevicesAvailableForAllocationSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);
        hostDevicesAvailableForAllocationSearch.and(OR_STATE, hostDevicesAvailableForAllocationSearch.entity().getState(), SearchCriteria.Op.EQ);
        hostDevicesAvailableForAllocationSearch.cp().done();
    }

    @Override
    public List<HostDeviceVO> listHostDevicesByHostId(Long hostId) {
        SearchCriteria<HostDeviceVO> sc = hostIdSearch.create();
        sc.setParameters(HOST_ID, hostId);
        return listBy(sc);
    }

    @Override
    public List<HostDeviceVO> listHostDevicesAvailableForAllocation(Long hostId, Long virtualMachineId, List<String> deviceTags) {
        SearchCriteria<HostDeviceVO> sc = hostDevicesAvailableForAllocationSearch.create();

        sc.setParameters(HOST_ID, hostId);
        sc.setParameters(STATE, HostDevice.State.Free);
        sc.setParameters(VIRTUAL_MACHINE_ID, virtualMachineId);
        sc.setParameters(OR_STATE, HostDevice.State.Attached);

        if (deviceTags != null) {
            sc.setParameters(DEVICE_TAG_IN, deviceTags.toArray());
        }

        return listBy(sc);
    }

    @Override
    public List<HostDeviceVO> listHostDevices(Long hostDeviceId, Long accountId, List<Long> domainIds, Long hostId, Long virtualMachineId, String deviceTag, HostDevice.State state, HostDevice.Type type) {
        SearchCriteria<HostDeviceVO> sc = hostDevicesSearch.create();

        sc.setParametersIfNotNull(ID, hostDeviceId);
        sc.setParametersIfNotNull(HOST_ID, hostId);
        sc.setParametersIfNotNull(VIRTUAL_MACHINE_ID, virtualMachineId);
        sc.setParametersIfNotNull(DEVICE_TAG, deviceTag);
        sc.setParametersIfNotNull(STATE, state);
        sc.setParametersIfNotNull(TYPE, type);
        sc.setParametersIfNotNull(ACCOUNT_ID, accountId);

        if (domainIds != null && !domainIds.isEmpty()) {
            sc.setParametersIfNotNull(DOMAIN_ID, domainIds.toArray());
        }

        return listBy(sc);
    }

    @Override
    public List<HostDeviceVO> listHostDevicesByVmId(Long vmId) {
        SearchCriteria<HostDeviceVO> sc = hostIdSearch.create();
        sc.setParameters(VIRTUAL_MACHINE_ID, vmId);
        return listBy(sc);
    }

    @Override
    public List<HostDeviceVO> listHostDevicesByHostIdAndState(Long hostId, HostDevice.State state) {
        SearchCriteria<HostDeviceVO> sc = hostDevicesSearch.create();

        sc.setParametersIfNotNull(HOST_ID, hostId);
        sc.setParameters(STATE, state);

        return listBy(sc);
    }
}
