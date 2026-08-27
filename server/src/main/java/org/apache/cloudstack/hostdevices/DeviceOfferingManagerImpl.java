package org.apache.cloudstack.hostdevices;

import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.hostdevices.DeviceOfferingDeviceTagVO;
import com.cloud.hostdevices.DeviceOfferingVO;
import com.cloud.hostdevices.VMInstanceDeviceOfferingsVO;
import com.cloud.hostdevices.dao.DeviceOfferingDao;
import com.cloud.hostdevices.dao.DeviceOfferingDeviceTagDao;
import com.cloud.hostdevices.dao.VMInstanceDeviceOfferingsDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.command.admin.hostdevices.CreateDeviceOfferingCmd;
import org.apache.cloudstack.api.command.user.hostdevices.AssignVirtualMachineToDeviceOfferingCmd;
import org.apache.cloudstack.api.command.user.hostdevices.ListDeviceOfferingsCmd;
import org.apache.cloudstack.api.command.user.hostdevices.RemoveVirtualMachineFromDeviceOfferingCmd;
import org.apache.cloudstack.api.response.DeviceOfferingResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DeviceOfferingManagerImpl extends ManagerBase implements DeviceOfferingManager {
    @Inject
    private DeviceOfferingDao deviceOfferingDao;
    @Inject
    private DeviceOfferingDeviceTagDao deviceOfferingDeviceTagsDao;
    @Inject
    private DomainDao domainDao;
    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private AccountManager accountManager;
    @Inject
    private VMInstanceDeviceOfferingsDao vmInstanceDeviceOfferingsDao;

    @Override
    public DeviceOffering createDeviceOffering(CreateDeviceOfferingCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long domainId = cmd.getDomainId();
        Long zoneId = cmd.getZoneId();

        if (!caller.getType().equals(Account.Type.ADMIN)) {
            logger.error("Cancelling creation because caller [{}] tried to create a device offering without being admin.", caller);
            throw new PermissionDeniedException("This action is only allowed for admins.");
        }

        if (domainId != null && zoneId != null) {
            logger.error("Cancelling device offering creation because both domainId and zoneId parameters were provided, and they are mutually exclusive.");
            throw new InvalidParameterValueException("Only one of domainId or zoneId can be specified, not both.");
        }

        if (cmd.getTags().isEmpty()) {
            logger.error("No device tag was provided, cancelling creation.");
            throw new InvalidParameterValueException("You must inform at least one device tag for the device offering.");
        }

        if (domainId != null) {
            Domain domain = domainDao.findById(domainId);

            if (domain == null) {
                logger.error("Domain with ID [{}] could not be found, cancelling offering creation.");
                throw new InvalidParameterValueException("Could not find domain with the informed ID.");
            }
        }

        if (zoneId != null) {
            DataCenter dataCenter = dataCenterDao.findById(zoneId);

            if (dataCenter == null) {
                logger.error("Zone with ID [{}] could not be found, cancelling offering creation.");
                throw new InvalidParameterValueException("Could not find zone with the informed ID.");
            }
        }

        // TODO: provavelmente precisa de uma transação aqui
        DeviceOfferingVO deviceOffering = deviceOfferingDao.persist(new DeviceOfferingVO(cmd.getName(), cmd.getDescription(), domainId, zoneId));

        for (String tag : cmd.getTags()) {
            deviceOfferingDeviceTagsDao.persist(new DeviceOfferingDeviceTagVO(deviceOffering.getId(), tag));
        }

        return deviceOffering;
    }

    @Override
    public DeviceOfferingResponse createDeviceOfferingResponse(DeviceOffering deviceOffering) {
        DeviceOfferingResponse response = new DeviceOfferingResponse();

        response.setId(deviceOffering.getUuid());
        response.setName(deviceOffering.getName());
        response.setDescription(deviceOffering.getDescription());
        response.setState(deviceOffering.getState().toString());
        response.setDomainId(deviceOffering.getDomainId());
        response.setZoneID(deviceOffering.getZoneId());
        response.setCreated(deviceOffering.getCreated());
        response.setRemoved(deviceOffering.getRemoved());
        response.setPublic(deviceOffering.getIsPublic());

        return response;
    }

    @Override
    public boolean assignVirtualMachineToDeviceOffering(Long virtualMachineId, Long deviceOfferingId) {
        Account caller = CallContext.current().getCallingAccount();

        VirtualMachine vm = getVMAndCheckAccess(virtualMachineId, caller);

        if (!Arrays.asList(VirtualMachine.State.Stopped, VirtualMachine.State.Running).contains(vm.getState())) {
            logger.error("Could not assign device offering to VM [{}], because it is in the [{}] state", virtualMachineId, vm.getState());
            throw new InvalidParameterValueException(String.format("VM is not in a valid state to assign device offering. Current state is [%s], and valid states are: %s", vm.getState(), Arrays.asList(VirtualMachine.State.Stopped, VirtualMachine.State.Running)));
        }

        getDeviceOfferingAndCheckAccess(deviceOfferingId, caller);

        List<VMInstanceDeviceOfferingsVO> existingAssignmentsForVM = vmInstanceDeviceOfferingsDao.listByVmId(virtualMachineId);
        if (CollectionUtils.isNotEmpty(existingAssignmentsForVM) && existingAssignmentsForVM.stream().anyMatch(assignment -> assignment.getDeviceOfferingId().equals(deviceOfferingId))) {
            logger.error("VM with ID [{}] already has this device offering assigned, cancelling assignment.", virtualMachineId);
            throw new InvalidParameterValueException(String.format("VM with ID [%s] already has this device offering assigned.", virtualMachineId));
        }

        vmInstanceDeviceOfferingsDao.persist(new VMInstanceDeviceOfferingsVO(virtualMachineId, deviceOfferingId));

        return true;
    }

    @Override
    public boolean removeVirtualMachineFromDeviceOffering(Long virtualMachineId, Long deviceOfferingId) {
        Account caller = CallContext.current().getCallingAccount();

        VirtualMachine vm = getVMAndCheckAccess(virtualMachineId, caller);

        if (vm.getState().equals(VirtualMachine.State.Running)) {
            logger.error("VM with ID [{}] is running, cannot remove device offering.", virtualMachineId);
            throw new InvalidParameterValueException(String.format("VM with ID [%s] is running. Please stop it to remove device offering.", virtualMachineId));
        }

        getDeviceOfferingAndCheckAccess(deviceOfferingId, caller);

        VMInstanceDeviceOfferingsVO assignedDeviceOffering = vmInstanceDeviceOfferingsDao.findByVmIdAndDeviceId(virtualMachineId, deviceOfferingId);
        if (assignedDeviceOffering == null) {
            logger.error("VM with ID [{}] does not have this device offering assigned, cannot remove.", virtualMachineId);
            throw new InvalidParameterValueException(String.format("VM with ID [%s] does not have this device offering assigned.", virtualMachineId));
        }

        vmInstanceDeviceOfferingsDao.expunge(assignedDeviceOffering.getId());
        return true;
    }

    @Override
    public List<DeviceOfferingVO> listDeviceOfferings(ListDeviceOfferingsCmd listDeviceOfferingsCmd) {
        Account caller = CallContext.current().getCallingAccount();
        String name = listDeviceOfferingsCmd.getName();
        Long domainId = listDeviceOfferingsCmd.getDomainId();
        Long zoneId = listDeviceOfferingsCmd.getZoneId();
        List<String> deviceTags = listDeviceOfferingsCmd.getDeviceTags();
        String stringState = listDeviceOfferingsCmd.getState();
        Boolean listAll = listDeviceOfferingsCmd.getListAll();

        DeviceOffering.State state = DeviceOffering.State.Active;
        if (stringState != null) {
            state = EnumUtils.getEnum(DeviceOffering.State.class, stringState);
            if (state == null) {
                logger.error("Invalid state [{}] provided for device offering listing.", stringState);
                throw new InvalidParameterValueException(String.format("Invalid state [%s] provided. Valid states are: %s",
                        stringState,
                        EnumUtils.getEnumList(DeviceOffering.State.class).stream().map(Enum::name).collect(Collectors.joining(", "))));
            }
        }

        if (domainId != null) {
            getDomainAndCheckAccess(domainId, caller);
        }

        DataCenter dataCenter = null;
        if (zoneId != null) {
            dataCenter = dataCenterDao.findById(zoneId);

            if (dataCenter == null) {
                logger.error("Zone with ID [{}] could not be found.", zoneId);
                throw new InvalidParameterValueException(String.format("Could not find zone with ID [%s].", zoneId));
            }
        }

        boolean showOnlyPublic = true;
        List<Long> domainIds = new ArrayList<>();
        if (caller.getType().equals(Account.Type.DOMAIN_ADMIN) && listAll != null && listAll) {
            showOnlyPublic = false;
            domainIds = domainDao.getDomainAndChildrenIds(caller.getDomainId());
        }

        if (caller.getType().equals(Account.Type.ADMIN) && listAll != null && listAll) {
            showOnlyPublic = false;
        }

        return deviceOfferingDao.listDeviceOfferings(name, domainIds, zoneId, deviceTags, state, showOnlyPublic);
    }

    @Override
    public DeviceOfferingResponse generateDeviceOfferingResponse(DeviceOffering offering) {
        DeviceOfferingResponse response = new DeviceOfferingResponse();

        response.setId(offering.getUuid());
        response.setName(offering.getName());
        response.setDescription(offering.getDescription());
        response.setState(offering.getState().toString());
        response.setDomainId(offering.getDomainId());
        response.setZoneID(offering.getZoneId());
        response.setCreated(offering.getCreated());
        response.setRemoved(offering.getRemoved());
        response.setPublic(offering.getIsPublic());

        return response;
    }

    private Domain getDomainAndCheckAccess(Long domainId, Account caller) {
        Domain domain = domainDao.findById(domainId);

        if (domain == null) {
            logger.error("Domain with ID [{}] could not be found.", domainId);
            throw new InvalidParameterValueException(String.format("Could not find domain with ID [%s].", domainId));
        }

        if (domain.getId() != caller.getDomainId()) {
            logger.error("Caller [{}] does not have access to domain with ID [{}].", caller, domainId);
            throw new PermissionDeniedException(String.format("You do not have permission to access domain with ID [%s].", domainId));
        }

        return domain;
    }

    private VirtualMachine getVMAndCheckAccess(Long virtualMachineId, Account caller) {
        VirtualMachine vm = vmInstanceDao.findById(virtualMachineId);

        if (vm == null) {
            logger.error("VM with ID [{}] could not be found.", virtualMachineId);
            throw new InvalidParameterValueException(String.format("Could not find VM with ID [%s].", virtualMachineId));
        }

        accountManager.checkAccess(caller, null, true, vm);

        return vm;
    }

    private DeviceOffering getDeviceOfferingAndCheckAccess(Long deviceOfferingId, Account caller) {
        DeviceOfferingVO deviceOffering = deviceOfferingDao.findById(deviceOfferingId);
        if (deviceOffering == null) {
            logger.error("Device offering with ID [{}] could not be found.", deviceOfferingId);
            throw new InvalidParameterValueException(String.format("Could not find device offering with ID [%s].", deviceOfferingId));
        }

        if (!deviceOffering.getIsPublic()) {
            Domain domain = domainDao.findById(caller.getDomainId());

            if (deviceOffering.getDomainId() != null && !deviceOffering.getDomainId().equals(domain.getId())) {
                logger.error("Device offering with ID [{}] is not public and does not belong to the caller's domain.", deviceOfferingId);
                throw new PermissionDeniedException("You do not have permission to use this device offering.");
            }

            if (deviceOffering.getZoneId() != null && !deviceOffering.getZoneId().equals(domain.getId())) {
                logger.error("Device offering with ID [{}] is not public and does not belong to the caller's zone.", deviceOfferingId);
                throw new PermissionDeniedException("You do not have permission to use this device offering.");
            }
        }

        return deviceOffering;
    }

    @Override
    public List<Class<?>> getCommands() {
        return List.of(CreateDeviceOfferingCmd.class, ListDeviceOfferingsCmd.class, AssignVirtualMachineToDeviceOfferingCmd.class, RemoveVirtualMachineFromDeviceOfferingCmd.class);
    }

    @Override
    public String getConfigComponentName() {
        return DeviceOfferingManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[0];
    }
}
