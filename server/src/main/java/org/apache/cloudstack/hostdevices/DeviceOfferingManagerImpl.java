// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.hostdevices;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.configuration.Resource;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.hostdevices.DeviceOfferingDeviceTagVO;
import com.cloud.hostdevices.DeviceOfferingVO;
import com.cloud.hostdevices.VMInstanceDeviceOfferingsVO;
import com.cloud.hostdevices.dao.DeviceOfferingDao;
import com.cloud.hostdevices.dao.DeviceOfferingDeviceTagDao;
import com.cloud.hostdevices.dao.VMInstanceDeviceOfferingsDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.command.admin.hostdevices.CreateDeviceOfferingCmd;
import org.apache.cloudstack.api.command.admin.hostdevices.DeleteDeviceOfferingCmd;
import org.apache.cloudstack.api.command.admin.hostdevices.UpdateDeviceOfferingCmd;
import org.apache.cloudstack.api.command.user.hostdevices.AssignVirtualMachineToDeviceOfferingCmd;
import org.apache.cloudstack.api.command.user.hostdevices.ListDeviceOfferingsCmd;
import org.apache.cloudstack.api.command.user.hostdevices.RemoveVirtualMachineFromDeviceOfferingCmd;
import org.apache.cloudstack.api.response.DeviceOfferingResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
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
    @Inject
    private ResourceLimitService resourceLimitMgr;

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DEVICE_OFFERING_CREATE, eventDescription = "creating device offering")
    public DeviceOffering createDeviceOffering(CreateDeviceOfferingCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long domainId = cmd.getDomainId();
        Long zoneId = cmd.getZoneId();
        String name = cmd.getName();

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
                logger.error("Domain with ID [{}] could not be found, cancelling offering creation.", domainId);
                throw new InvalidParameterValueException("Could not find domain with the informed ID.");
            }
        }

        if (zoneId != null) {
            DataCenter dataCenter = dataCenterDao.findById(zoneId);

            if (dataCenter == null) {
                logger.error("Zone with ID [{}] could not be found, cancelling offering creation.", zoneId);
                throw new InvalidParameterValueException("Could not find zone with the informed ID.");
            }
        }

        return Transaction.execute((TransactionCallback<DeviceOfferingVO>) status -> {
            DeviceOffering nameDeviceOffering = deviceOfferingDao.findByName(name);

            if (nameDeviceOffering != null) {
                logger.error("Device offering with name [{}] already exists, cancelling creation.", name);
                throw new InvalidParameterValueException("A device offering with the same name already exists.");
            }

            DeviceOfferingVO newOffering = deviceOfferingDao.persist(new DeviceOfferingVO(cmd.getName(), cmd.getDescription(), domainId, zoneId));

            for (String tag : parseDeviceOfferingTagsParameter(cmd.getTags())) {
                deviceOfferingDeviceTagsDao.persist(new DeviceOfferingDeviceTagVO(newOffering.getId(), tag));
            }

            return newOffering;
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DEVICE_OFFERING_ASSIGN, eventDescription = "assigning device offering to VM")
    public boolean assignVirtualMachineToDeviceOffering(Long virtualMachineId, Long deviceOfferingId) throws ResourceAllocationException {
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

        checkVmOwnerHostDeviceLimit(vm, deviceOfferingId);

        vmInstanceDeviceOfferingsDao.persist(new VMInstanceDeviceOfferingsVO(virtualMachineId, deviceOfferingId));

        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DEVICE_OFFERING_REMOVE, eventDescription = "removing device offering from VM")
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
    public Pair<List<? extends DeviceOffering>, Integer> listDeviceOfferings(ListDeviceOfferingsCmd listDeviceOfferingsCmd) {
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

        Boolean showOnlyPublic = true;
        List<Long> domainIds = new ArrayList<>();
        if (caller.getType().equals(Account.Type.DOMAIN_ADMIN) && BooleanUtils.isTrue(listAll)) {
            showOnlyPublic = null;
            domainIds = domainDao.getDomainAndChildrenIds(caller.getDomainId());
        }

        if (caller.getType().equals(Account.Type.ADMIN) && BooleanUtils.isTrue(listAll)) {
            showOnlyPublic = null;
        }

        Filter filter = new Filter(DeviceOfferingVO.class, "id", true, listDeviceOfferingsCmd.getStartIndex(), listDeviceOfferingsCmd.getPageSizeVal());
        Pair<List<DeviceOfferingVO>, Integer> result = deviceOfferingDao.listDeviceOfferings(listDeviceOfferingsCmd.getId(), name, domainIds, zoneId, deviceTags, state, showOnlyPublic, filter);

        return new Pair<>(result.first(), result.second());
    }

    @Override
    public DeviceOfferingResponse generateDeviceOfferingResponse(DeviceOffering offering) {
        DeviceOfferingResponse response = new DeviceOfferingResponse();

        response.setId(offering.getUuid());
        response.setName(offering.getName());
        response.setDescription(offering.getDescription());
        response.setState(offering.getState().toString());
        if (offering.getDomainId() != null) {
            Domain offeringDomain = domainDao.findById(offering.getDomainId());

            if (offeringDomain != null) {
                response.setDomainId(offeringDomain.getUuid());
            }
        }

        if (offering.getZoneId() != null) {
            DataCenter offeringZone = dataCenterDao.findById(offering.getZoneId());

            if (offeringZone != null) {
                response.setZoneId(offeringZone.getUuid());
            }
        }
        response.setCreated(offering.getCreated());
        response.setRemoved(offering.getRemoved());
        response.setIsPublic(offering.getIsPublic());

        List<String> deviceTags = deviceOfferingDeviceTagsDao.getDeviceOfferingTags(offering.getId());
        if (CollectionUtils.isNotEmpty(deviceTags)) {
            response.setDeviceTags(deviceTags);
        }

        return response;
    }

    @Override
    public void unassignVmFromOfferings(Long vmId) {
        VirtualMachine vm = vmInstanceDao.findById(vmId);

        if (vm == null) {
            logger.error("VM with ID [{}] could not be found.", vmId);
            throw new InvalidParameterValueException(String.format("Could not find VM with ID [%s].", vmId));
        }

        List<VMInstanceDeviceOfferingsVO> existingAssignmentsForVM = vmInstanceDeviceOfferingsDao.listByVmId(vmId);
        if (CollectionUtils.isEmpty(existingAssignmentsForVM)) {
            logger.info("VM with ID [{}] has no device offerings assigned, nothing to unassign.", vmId);
            return;
        }

        logger.debug("Unassigning device offerings {} from VM with ID [{}].", existingAssignmentsForVM.stream().map(VMInstanceDeviceOfferingsVO::getDeviceOfferingId).collect(Collectors.toList()), vmId);
        vmInstanceDeviceOfferingsDao.expungeByVmId(vmId);
    }

    @Override
    public boolean isVmAssignedToDeviceOfferings(VirtualMachine vm) {
        List<VMInstanceDeviceOfferingsVO> existingAssignmentsForVM = vmInstanceDeviceOfferingsDao.listByVmId(vm.getId());
        return CollectionUtils.isNotEmpty(existingAssignmentsForVM);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DEVICE_OFFERING_EDIT, eventDescription = "updating device offering")
    public DeviceOffering updateDeviceOffering(UpdateDeviceOfferingCmd updateDeviceOfferingCmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long id = updateDeviceOfferingCmd.getId();
        String displayName = updateDeviceOfferingCmd.getName();
        String description = updateDeviceOfferingCmd.getDescription();
        List<String> deviceTags = updateDeviceOfferingCmd.getTags();
        String stringState = updateDeviceOfferingCmd.getState();

        return Transaction.execute((TransactionCallback<DeviceOfferingVO>) status -> {
            DeviceOfferingVO deviceOffering = deviceOfferingDao.lockRow(id, true);

            if (deviceOffering == null) {
                logger.error("Device offering with ID [{}] could not be found.", id);
                throw new InvalidParameterValueException(String.format("Could not find device offering with ID [%s].", id));
            }

            if (!deviceOffering.getIsPublic() && !accountManager.isRootAdmin(caller.getAccountId())) {
                Domain domain = domainDao.findById(caller.getDomainId());

                if (deviceOffering.getDomainId() != null && !deviceOffering.getDomainId().equals(domain.getId())) {
                    logger.error("Device offering with ID [{}] is not public and does not belong to the caller's domain.", id);
                    throw new PermissionDeniedException("You do not have permission to use this device offering.");
                }
            }

            DeviceOffering.State state = null;
            if (stringState != null) {
                state = EnumUtils.getEnum(DeviceOffering.State.class, stringState);
                if (state == null) {
                    logger.error("Invalid state [{}] provided for device offering update.", stringState);
                    throw new InvalidParameterValueException(String.format("Invalid state [%s] provided. Valid states are: Active and Inactive", stringState));
                }
            }

            if (deviceTags != null && CollectionUtils.isEmpty(deviceTags)) {
                logger.error("No device tag was provided, cancelling device offering update.");
                throw new InvalidParameterValueException("You must inform at least one device tag for the device offering.");
            }

            if (displayName != null) {
                deviceOffering.setName(displayName);
            }
            if (description != null) {
                deviceOffering.setDescription(description);
            }
            if (state != null) {
                deviceOffering.setState(state);
            }

            deviceOfferingDao.update(deviceOffering.getId(), deviceOffering);
            updateDeviceOfferingTags(deviceOffering.getId(), deviceTags);

            return deviceOffering;
        });
    }

    @Override
    public List<DeviceOfferingVO> getDeviceOfferingsByVmId(Long vmId) {
        return deviceOfferingDao.listVirtualMachineDeviceOfferings(vmId);
    }

    @Override
    public boolean canAccountAccessOffering(DeviceOffering deviceOffering, Account newAccount) {
        // TODO ERIK: Ver sobre a questão de limitação a nivel de zona

        if (deviceOffering.getIsPublic()) {
            return true;
        }

        Domain offeringDomain = domainDao.findById(deviceOffering.getDomainId());

        if (offeringDomain == null) {
            return false;
        }

        try {
            accountManager.checkAccess(newAccount, offeringDomain);
        } catch (PermissionDeniedException e) {
            logger.debug("Account [{}] does not have access to the domain of device offering [{}].", newAccount.getUuid(), deviceOffering.getUuid());
            return false;
        }

        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DEVICE_OFFERING_DELETE, eventDescription = "deleting device offering")
    public boolean deleteOffering(Long id) {
        Account caller = CallContext.current().getCallingAccount();

        if (!Account.Type.ADMIN.equals(caller.getType())) {
            logger.error("Cancelling deletion because caller [{}] tried to delete a device offering without being admin.", caller);
            throw new PermissionDeniedException("This action is only permitted for admins.");
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                DeviceOfferingVO deviceOffering = deviceOfferingDao.lockRow(id, true);

                if (deviceOffering == null) {
                    logger.error("Device offering with ID [{}] could not be found.", id);
                    throw new InvalidParameterValueException(String.format("Could not find device offering with ID [%s].", id));
                }

                List<VMInstanceDeviceOfferingsVO> assignedVMs = vmInstanceDeviceOfferingsDao.listByOfferingId(id);

                if (CollectionUtils.isNotEmpty(assignedVMs)) {
                    logger.error("Cannot delete device offering with ID [{}] because the following VMs are still assigned to it: {}.", id, assignedVMs.stream().map(VMInstanceDeviceOfferingsVO::getVirtualMachineId).collect(Collectors.toList()));
                    throw new InvalidParameterValueException(String.format("Cannot delete device offering with ID [%s] because it is still assigned to VMs.", id));
                }

                deviceOffering.setState(DeviceOffering.State.Inactive);
                deviceOfferingDao.update(deviceOffering.getId(), deviceOffering);
                deviceOfferingDao.remove(id);
                deviceOfferingDeviceTagsDao.expungeByOfferingId(id);
            }
        });

        return true;
    }

    private void updateDeviceOfferingTags(Long offeringId, List<String> deviceTags) {
        if (deviceTags == null) {
            return;
        }

        List<String> newTags = parseDeviceOfferingTagsParameter(deviceTags);

        deviceOfferingDeviceTagsDao.expungeByOfferingId(offeringId);

        for (String tag : newTags) {
            deviceOfferingDeviceTagsDao.persist(new DeviceOfferingDeviceTagVO(offeringId, tag));
        }
    }

    protected void checkVmOwnerHostDeviceLimit(VirtualMachine vm, Long deviceOfferingId) throws ResourceAllocationException {
        List<String> offeringTags = deviceOfferingDeviceTagsDao.getDeviceOfferingTags(deviceOfferingId);

        if (CollectionUtils.isEmpty(offeringTags)) {
            return;
        }

        Account owner = accountManager.getActiveAccountById(vm.getAccountId());

        if (owner == null) {
            throw new CloudRuntimeException(String.format("Could not find the owner of VM [%s].", vm.getUuid()));
        }

        resourceLimitMgr.checkResourceLimit(owner, Resource.ResourceType.host_device, offeringTags.size());
    }

    private Domain getDomainAndCheckAccess(Long domainId, Account caller) {
        Domain domain = domainDao.findById(domainId);

        if (domain == null) {
            logger.error("Domain with ID [{}] could not be found.", domainId);
            throw new InvalidParameterValueException(String.format("Could not find domain with ID [%s].", domainId));
        }

        accountManager.checkAccess(caller, domain);

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

    private DeviceOfferingVO getDeviceOfferingAndCheckAccess(Long deviceOfferingId, Account caller) {
        DeviceOfferingVO deviceOffering = deviceOfferingDao.findById(deviceOfferingId);
        if (deviceOffering == null) {
            logger.error("Device offering with ID [{}] could not be found.", deviceOfferingId);
            throw new InvalidParameterValueException(String.format("Could not find device offering with ID [%s].", deviceOfferingId));
        }

        if (!deviceOffering.getIsPublic() && !accountManager.isRootAdmin(caller.getAccountId())) {
            Domain domain = domainDao.findById(caller.getDomainId());

            if (deviceOffering.getDomainId() != null && !deviceOffering.getDomainId().equals(domain.getId())) {
                logger.error("Device offering with ID [{}] is not public and does not belong to the caller's domain.", deviceOfferingId);
                throw new PermissionDeniedException("You do not have permission to use this device offering.");
            }

            // TODO ERIK: nao sei como ver isso
//            if (deviceOffering.getZoneId() != null && !deviceOffering.getZoneId().equals(domain.get())) {
//                logger.error("Device offering with ID [{}] is not public and does not belong to the caller's zone.", deviceOfferingId);
//                throw new PermissionDeniedException("You do not have permission to use this device offering.");
//            }
        }

        return deviceOffering;
    }

    private List<String> parseDeviceOfferingTagsParameter(List<String> commandTags) {
        if (CollectionUtils.isEmpty(commandTags)) {
            logger.error("No device tag was provided.");
            throw new InvalidParameterValueException("You must inform at least one device tag for the device offering.");
        }

        List<String> tags = new ArrayList<>();
        for (String tag : commandTags) {
            String[] tagAndAmount = tag.split(":");
            String tagName = tagAndAmount[0];
            int amount = 1;

            if (tagAndAmount.length > 1) {
                try {
                    amount = Integer.parseInt(tagAndAmount[1]);
                } catch (NumberFormatException e) {
                    logger.error("Invalid amount [{}] specified for device tag [{}].", tagAndAmount[1], tagName);
                    throw new CloudRuntimeException(String.format("Invalid amount specified for tag: %s. Please, specify a valid integer amount.", tagName));
                }
            }

            for (int i = 0; i < amount; i++) {
                tags.add(tagName);
            }
        }

        return tags;
    }

    @Override
    public List<Class<?>> getCommands() {
        return List.of(CreateDeviceOfferingCmd.class,
                ListDeviceOfferingsCmd.class,
                AssignVirtualMachineToDeviceOfferingCmd.class,
                RemoveVirtualMachineFromDeviceOfferingCmd.class,
                UpdateDeviceOfferingCmd.class,
                DeleteDeviceOfferingCmd.class
        );
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
