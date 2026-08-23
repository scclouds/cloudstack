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
import org.apache.cloudstack.api.response.DeviceOfferingResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;

import javax.inject.Inject;
import java.util.List;

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
        response.setPublic(deviceOffering.getPublic());

        return response;
    }

    @Override
    public boolean assignVirtualMachineToDeviceOffering(Long virtualMachineId, Long deviceOfferingId) {
        Account caller = CallContext.current().getCallingAccount();

        VirtualMachine vm = vmInstanceDao.findById(virtualMachineId);

        if (vm == null) {
            logger.error("VM with ID [{}] could not be found, cancelling assignment.", virtualMachineId);
            throw new InvalidParameterValueException(String.format("Could not find VM with ID [%s].", virtualMachineId));
        }

        accountManager.checkAccess(caller, null, true, vm);

        DeviceOfferingVO deviceOffering = deviceOfferingDao.findById(deviceOfferingId);
        if (deviceOffering == null) {
            logger.error("Device offering with ID [{}] could not be found, cancelling assignment.", deviceOfferingId);
            throw new InvalidParameterValueException(String.format("Could not find device offering with ID [%s].", deviceOfferingId));
        }

        if (!deviceOffering.getPublic()) {
            Domain domain = domainDao.findById(caller.getDomainId());

            if (deviceOffering.getDomainId() != null && !deviceOffering.getDomainId().equals(domain.getId())) {
                logger.error("Device offering with ID [{}] is not public and does not belong to the caller's domain, cancelling assignment.", deviceOfferingId);
                throw new PermissionDeniedException("You do not have permission to assign this device offering.");
            }

            if (deviceOffering.getZoneId() != null && !deviceOffering.getZoneId().equals(domain.getId())) {
                logger.error("Device offering with ID [{}] is not public and does not belong to the caller's zone, cancelling assignment.", deviceOfferingId);
                throw new PermissionDeniedException("You do not have permission to assign this device offering.");
            }
        }

        vmInstanceDeviceOfferingsDao.persist(new VMInstanceDeviceOfferingsVO(virtualMachineId, deviceOfferingId));

        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        return List.of(CreateDeviceOfferingCmd.class, AssignVirtualMachineToDeviceOfferingCmd.class);
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
