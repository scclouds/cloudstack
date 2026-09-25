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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ScanDevicesCommand;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Resource;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hostdevices.DeviceOfferingVO;
import com.cloud.hostdevices.HostDeviceVO;
import com.cloud.hostdevices.dao.DeviceOfferingDao;
import com.cloud.hostdevices.dao.DeviceOfferingDeviceTagDao;
import com.cloud.hostdevices.dao.HostDeviceDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.org.Cluster;
import com.cloud.resourcelimit.CheckedReservation;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.Pair;
import com.cloud.utils.UuidUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.cloudstack.alert.AlertService.AlertType;
import org.apache.cloudstack.api.command.admin.hostdevices.ScanHostDevicesCmd;
import org.apache.cloudstack.api.command.admin.hostdevices.UpdateHostDeviceCmd;
import org.apache.cloudstack.api.command.user.hostdevices.ListHostDevicesCmd;
import org.apache.cloudstack.api.response.HostDeviceResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.reservation.dao.ReservationDao;
import org.apache.cloudstack.utils.libvirt.mappers.serialization.LibvirtDeviceDeserializer;
import org.apache.cloudstack.utils.libvirt.model.LibvirtDevice;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.ThreadContext;

import javax.inject.Inject;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HostDevicesManagerImpl extends ManagerBase implements HostDevicesManager {
    @Inject
    AgentManager agentManager;
    @Inject
    HostDao hostDao;
    @Inject
    ClusterDao clusterDao;
    @Inject
    ClusterDetailsDao clusterDetailsDao;
    @Inject
    HostDeviceDao hostDeviceDao;
    @Inject
    AccountManager accountManager;
    @Inject
    VMInstanceDao virtualMachineDao;
    @Inject
    DomainDao domainDao;
    @Inject
    HostDetailsDao hostDetailsDao;
    @Inject
    private DeviceOfferingDao deviceOfferingDao;
    @Inject
    private DeviceOfferingDeviceTagDao deviceOfferingDeviceTagDao;
    @Inject
    private ResourceLimitService resourceLimitMgr;
    @Inject
    private ReservationDao reservationDao;
    @Inject
    private AlertManager alertManager;

    private ScheduledExecutorService scanScheduledExecutor;
    private static final String LOGCONTEXTID = "logcontextid";
    private static final long INITIAL_DELAY_IN_SECONDS = 60L;
    private static final long AUTOMATIC_SCAN_TASK_INTERVAL_IN_SECONDS = 300L;
    private static final ObjectMapper MAPPER = createMapper();

    public HostDevicesManagerImpl() {
    }

    @Override
    public boolean start() {
        super.start();

        scanScheduledExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("AutomaticDeviceScanScheduler"));
        scanScheduledExecutor.scheduleAtFixedRate(this::triggerAutomaticScanForClusters,
                INITIAL_DELAY_IN_SECONDS,
                AUTOMATIC_SCAN_TASK_INTERVAL_IN_SECONDS,
                TimeUnit.SECONDS
        );

        return true;
    }

    @Override
    public boolean stop() {
        if (scanScheduledExecutor != null) {
            scanScheduledExecutor.shutdownNow();
        }

        return super.stop();
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_HOST_DEVICE_SCAN, eventDescription = "scanning host devices")
    public void scanHostDevice(ScanHostDevicesCmd cmd) {
        Long hostId = cmd.getHostId();
        Long clusterId = cmd.getClusterId();
        Long zoneId = cmd.getZoneId();
        Account caller = CallContext.current().getCallingAccount();

        if (!caller.getType().equals(Account.Type.ADMIN)) {
            logger.error("Cancelling devices scan because caller is not ROOT admin.");
            throw new PermissionDeniedException("Scanning host devices is not allowed for non-ROOT Admins.");
        }

        List<HostVO> hostsListForDeviceScan = getHostsListForDeviceScan(zoneId, clusterId, hostId);

        if (hostsListForDeviceScan == null) {
            logger.error("At least one of the parameters (zoneId, clusterId, hostId) must be provided.");
            throw new InvalidParameterValueException("Failed to retrieve hosts for device scan.");
        }

        logger.debug("Selected {} hosts for device scan: {}", hostsListForDeviceScan.size(), hostsListForDeviceScan.stream().map(HostVO::getId).collect(Collectors.toList()));

        Map<String, String> hostFailures = scanHostDevices(hostsListForDeviceScan);

        if (!hostFailures.isEmpty()) {
            throw new CloudRuntimeException(String.format("Failed to scan the devices of %d out of %d hosts: %s", hostFailures.size(), hostsListForDeviceScan.size(), hostFailures));
        }
    }

    protected List<HostVO> getHostsListForDeviceScan(Long zoneId, Long clusterId, Long hostId) {
        List<HostVO> hostsForScan = new ArrayList<>();

        if (hostId != null) {
            HostVO host = hostDao.findUpAndRoutingHypervisorHostById(hostId, Hypervisor.HypervisorType.KVM);
            if (host == null) {
                logger.debug("Host with ID {} was either not found, is not in UP state, is not of Routing type or is not a KVM host.", hostId);
                throw new InvalidParameterValueException("Host with id " + hostId + " not found or is not suitable for device scan.");
            }

            clusterId = host.getClusterId();
            zoneId = host.getDataCenterId();
            hostsForScan.add(host);
        }

        if (!hostsForScan.isEmpty()) {
            return hostsForScan;
        }

        if (clusterId != null) {
            Cluster cluster = clusterDao.findById(clusterId);

            if (cluster == null) {
                logger.debug("Cluster with ID {} was not found", clusterId);
                throw new InvalidParameterValueException("Cluster with id " + clusterId + " is not suitable for device scan.");
            }

            if (cluster.getHypervisorType() != Hypervisor.HypervisorType.KVM) {
                logger.debug("Cluster {} is not valid for device scan because it is not a KVM cluster", cluster.getId());
                throw new InvalidParameterValueException("Cluster with id " + clusterId + " is not suitable for device scan.");
            }

            hostsForScan = hostDao.listAllRoutingHostsUpInClusters(List.of(clusterId), Hypervisor.HypervisorType.KVM);
            return hostsForScan;
        }

        if (zoneId != null) {
            List<ClusterVO> clusters = clusterDao.listByDcHyType(zoneId, Hypervisor.HypervisorType.KVM.toString());

            if (clusters.isEmpty()) {
                logger.debug("No suitable KVM clusters found in zone with ID {}", zoneId);
                throw new InvalidParameterValueException("No suitable KVM clusters found in zone with id " + zoneId);
            }

            logger.debug("Found the following clusters for device scan {}", clusters);
            hostsForScan = hostDao.listAllRoutingHostsUpInClusters(clusters.stream().map(ClusterVO::getId).collect(Collectors.toList()), Hypervisor.HypervisorType.KVM);

            if (hostsForScan.isEmpty()) {
                logger.debug("No suitable KVM hosts found in zone with ID {}", zoneId);
                throw new InvalidParameterValueException("No suitable KVM hosts found in zone with id " + zoneId);
            }

            return hostsForScan;
        }

        return null;
    }

    protected void scanHostDevices(HostVO host) {
        logger.debug("Sending ScanDevicesCommand to host with ID {}", host.getId());

        Answer answer;
        try {
            answer = agentManager.send(host.getId(), new ScanDevicesCommand());
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException(String.format("Failure to scan devices of host with ID %s due to %s", host.getUuid(), e.getMessage()), e);
        }

        if (answer == null || !answer.getResult()) {
            String details = answer == null ? "no answer was received from the host" : answer.getDetails();
            throw new CloudRuntimeException(String.format("Failure to scan devices of host with ID %s due to %s", host.getUuid(), details));
        }

        List<? extends LibvirtDevice> returnedDevices;
        try {
            returnedDevices = MAPPER.readValue(answer.getDetails(), MAPPER.getTypeFactory().constructCollectionType(List.class, LibvirtDevice.class));
        } catch (IOException e) {
            throw new CloudRuntimeException(String.format("Failure to scan devices of host with ID %s because the returned devices could not be read: %s", host.getUuid(), e.getMessage()), e);
        }

        compareIncomingDevicesWithExistingOnes(returnedDevices, host);
        logger.info("Finished executing device scan for host {}", host.getId());
    }

    protected Map<String, String> scanHostDevices(List<HostVO> hosts) {
        Map<String, String> hostFailures = new LinkedHashMap<>();

        for (HostVO host : hosts) {
            try {
                scanHostDevices(host);
            } catch (Exception e) {
                logger.error("Failed to scan devices of host with ID {}.", host.getId(), e);
                hostFailures.put(host.getName(), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        }

        return hostFailures;
    }

    private void compareIncomingDevicesWithExistingOnes(List<? extends LibvirtDevice> incomingDevices, HostVO host) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                hostDao.lockRow(host.getId(), true);

                List<HostDeviceVO> currentDevices = hostDeviceDao.listHostDevicesByHostId(host.getId());
                logger.debug("The following host devices are registered for host with ID {}: {}", host.getId(), currentDevices.stream().map(HostDeviceVO::getPciName).collect(Collectors.toList()));

                List<HostDeviceVO> mappedIncomingDevices = incomingDevices.stream().map(d -> HostDeviceVO.mapLibvirtDevice(d, host.getId())).collect(Collectors.toList());

                if (currentDevices.isEmpty()) {
                    logger.info("As no device is saved for the host yet, we will save all the devices returned by the agent to the database.");

                    for (HostDeviceVO device : mappedIncomingDevices) {
                        hostDeviceDao.persist(device);
                    }

                    return;
                }

                logger.debug("Handling devices that are not registered on the database");

                handleMissingDevices(currentDevices, mappedIncomingDevices);

                handleUnregisteredDevices(currentDevices, mappedIncomingDevices);
            }
        });
    }

    private void handleMissingDevices(List<HostDeviceVO> registeredDevices, List<HostDeviceVO> incomingDevices) {
        logger.debug("Checking for devices that were listed in the database, but were not returned by the host. Only Attached ones will be considered as missing.");
        List<HostDeviceVO> missingDevices = registeredDevices
                .stream()
                .filter(registered -> incomingDevices
                        .stream()
                        .noneMatch(incoming -> incoming.getPciName().equals(registered.getPciName())))
                .filter(d -> d.getInstanceId() != null)
                .collect(Collectors.toList());

        if (missingDevices.isEmpty()) {
            logger.debug("No missing devices.");
            return;
        }

        logger.debug("Found the following missing devices. {}", missingDevices);

        for (HostDeviceVO device : missingDevices) {
            device.setState(HostDevice.State.Missing);
            hostDeviceDao.update(device.getId(), device);
        }

        logger.debug("Sending alerts to operators about missing devices.");
        sendAlertAboutMissingDevices(missingDevices);
    }

    private void handleUnregisteredDevices(List<HostDeviceVO> currentDevices, List<HostDeviceVO> incomingDevices) {
        logger.debug("Checking for devices returned by the host, but not registered to the database to save them.");
        List<HostDeviceVO> unregisteredDevices = incomingDevices
                .stream()
                .filter(id -> currentDevices
                        .stream()
                        .noneMatch(c -> c.getPciName().equals(id.getPciName())))
                .collect(Collectors.toList());
        logger.debug("Found the following unregistered devices: {}", unregisteredDevices);

        for (HostDeviceVO device : unregisteredDevices) {
            logger.debug("Saving unregistered device [{}] to the database.", device.getPciName());
            hostDeviceDao.persist(device);
        }
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("LibvirtDeviceModule");
        module.addDeserializer(LibvirtDevice.class, new LibvirtDeviceDeserializer());
        mapper.registerModule(module);
        return mapper;
    }

    @Override
    public Pair<List<? extends HostDevice>, Integer> listHostDevices(ListHostDevicesCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long hostDeviceId = cmd.getId();
        Long accountId = cmd.getAccountId();
        Long hostId = cmd.getHostId();
        Long virtualMachineId = cmd.getVirtualMachineId();
        String deviceTag = cmd.getDeviceTag();
        String stringDeviceType = cmd.getType();
        String stringDeviceState = cmd.getState();

        HostDevice.State state = stringDeviceState == null ? null : parseEnumIgnoreCase(HostDevice.State.class, stringDeviceState);
        HostDevice.Type type = stringDeviceType == null ? null : parseEnumIgnoreCase(HostDevice.Type.class, stringDeviceType);

        if (hostId != null) {
            Host host = hostDao.findById(hostId);

            if (host == null) {
                logger.debug("Host with ID {} was not found", hostId);
                throw new InvalidParameterValueException("Host with id " + hostId + " was not found.");
            }
        }

        if (virtualMachineId != null) {
            VirtualMachine vm = virtualMachineDao.findById(virtualMachineId);

            if (vm == null) {
                logger.debug("Virtual machine with ID {} was not found", virtualMachineId);
                throw new InvalidParameterValueException("Virtual machine with id " + virtualMachineId + " was not found.");
            }

            accountManager.checkAccess(caller, null, true, vm);
        }

        Account account = caller;
        if (accountId != null) {
            account = accountManager.getActiveAccountById(accountId);

            if (account == null) {
                logger.debug("Account with ID {} was not found", accountId);
                throw new InvalidParameterValueException("Account with id " + accountId + " was not found.");
            }

            accountManager.checkAccess(caller, null, true, account);
        }

        Pair<Long, List<Long>> accountIdDomainsList = accountManager.finalizeListingFiltersBasedOnRecursiveAndListAll(false, cmd.listAll(), false, account.getId(), new ArrayList<>(List.of(account.getId())));
        accountId = accountIdDomainsList.first();
        List<Long> domainIds = accountIdDomainsList.second();

        Filter filter = new Filter(HostDeviceVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Pair<List<HostDeviceVO>, Integer> result = hostDeviceDao.listHostDevices(hostDeviceId, accountId, domainIds, hostId, virtualMachineId, deviceTag, state, type, filter);

        return new Pair<>(result.first(), result.second());
    }

    @Override
    public HostDeviceResponse generateHostDeviceResponse(HostDevice device) {
        // TODO ERIK: colocar builder com response de admin e de user
        HostDeviceResponse res = new HostDeviceResponse();

        res.setId(device.getUuid());
        res.setDisplayName(device.getDisplayName());
        res.setState(device.getState().toString());
        res.setType(device.getType().toString());
        res.setDeviceTag(device.getDeviceTag());

        if (device.getInstanceId() != null) {
            VirtualMachine vm = virtualMachineDao.findById(device.getInstanceId());
            if (vm != null) {
                res.setVirtualMachineId(vm.getUuid());
                res.setVirtualMachineName(vm.getName());
            }
        }

        if (device.getAccountId() != null) {
            Account account = accountManager.getActiveAccountById(device.getAccountId());
            if (account != null) {
                res.setAccountId(account.getUuid());
                res.setAccount(account.getAccountName());

                Domain domain = domainDao.findById(account.getDomainId());
                if (domain != null) {
                    res.setDomainId(domain.getUuid());
                    res.setDomain(domain.getName());
                }
            }
        }

        Account caller = CallContext.current().getCallingAccount();
        if (caller.getType().equals(Account.Type.ADMIN)) {
            res.setPciName(device.getPciName());
            res.setPciDomain(device.getPciDomain());
            res.setPciClass(device.getPciClass());
            res.setPciSlot(device.getPciSlot());
            res.setPciFunction(device.getPciFunction());
            res.setVendorId(device.getPciVendorId());
            res.setDeviceId(device.getPciDeviceId());
            res.setCreated(device.getCreated());
            res.setRemoved(device.getRemoved());
            res.setOneTimeUse(device.getOneTimeUse());

            Host host = hostDao.findById(device.getHostId());
            if (host != null) {
                res.setHostId(host.getUuid());
                res.setHostname(host.getName());
            }
        }

        res.setObjectName("hostdevices");

        return res;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_HOST_DEVICE_UPDATE, eventDescription = "updating host device")
    public HostDevice updateHostDevice(UpdateHostDeviceCmd updateHostDeviceCmd) {
        Boolean enabled = updateHostDeviceCmd.getEnabled();
        String displayName = updateHostDeviceCmd.getDisplayName();
        String tag = updateHostDeviceCmd.getTag();
        String type = updateHostDeviceCmd.getType();

        if (ObjectUtils.allNull(enabled, displayName, tag, type)) {
            throw new InvalidParameterValueException("At least one of the following parameters must be provided: enabled, displayName, tags, type");
        }

        if (tag != null && tag.isBlank()) {
            logger.error("Cancelling host device update because the informed device tag is blank.");
            throw new InvalidParameterValueException("The device tag cannot be blank.");
        }

        HostDevice.Type newDeviceType = type == null ? null : parseEnumIgnoreCase(HostDevice.Type.class, type);

        return Transaction.execute((TransactionCallback<HostDeviceVO>) status -> {
            HostDeviceVO device = hostDeviceDao.lockRow(updateHostDeviceCmd.getDeviceId(), true);

            if (device == null) {
                logger.debug("Host device with ID {} was not found", updateHostDeviceCmd.getDeviceId());
                throw new InvalidParameterValueException("Host device with id " + updateHostDeviceCmd.getDeviceId() + " was not found.");
            }

            if (!device.canBeUpdated()) {
                logger.error("Could not update host device state. Current device state is {} and invalid states are {}.", device.getState(), HostDevice.INVALID_UPDATE_STATES);
                throw new InvalidParameterValueException(String.format("Could not update device because it is in state [%s] and updating devices in states %s is not allowed.", device.getState(), HostDevice.INVALID_UPDATE_STATES));
            }

            if (enabled != null) {
                device.setState(enabled ? HostDevice.State.Free : HostDevice.State.Disabled);
            }

            if (displayName != null && !displayName.isBlank()) {
                device.setDisplayName(displayName);
            }

            if (tag != null) {
                device.setDeviceTag(tag);
            }

            if (newDeviceType != null) {
                device.setType(newDeviceType);
            }

            if (updateHostDeviceCmd.getOneTimeUse() != null) {
                device.setOneTimeUse(updateHostDeviceCmd.getOneTimeUse());
            }

            hostDeviceDao.update(device.getId(), device);

            return device;
        });
    }

    private <E extends Enum<E>> E parseEnumIgnoreCase(Class<E> enumClass, String value) {
        E parsedValue = EnumUtils.getEnumIgnoreCase(enumClass, value);

        if (parsedValue == null) {
            String supportedValues = Arrays.toString(enumClass.getEnumConstants());
            throw new InvalidParameterValueException(String.format("Invalid value provided %s. Supported values are: %s.", value, supportedValues));
        }

        return parsedValue;
    }

    @Override
    public void releaseHostDevicesForVm(Long vmId) {
        VirtualMachine vm = getVirtualMachineOrThrow(vmId);

        List<HostDeviceVO> releasedDevices = Transaction.execute(
                (TransactionCallback<List<HostDeviceVO>>) status ->
                        releaseDevices(vm, hostDeviceDao.listAndLockHostDevicesByVmId(vmId)));

        // TODO ERIK: talvez seja overkill, mas nao me parece correto prender o fluxo até que o alerta seja enviado. rever isso
        logger.debug("Sending alerts to operators about host devices that were released from VM with ID {} and require cleanup.", vmId);
        sendAlertAboutCleanupDevices(releasedDevices);
    }

    @Override
    public void releaseHostDevicesNotRequiredByOfferings(Long vmId, List<? extends DeviceOffering> remainingOfferings) {
        VirtualMachine vm = getVirtualMachineOrThrow(vmId);
        Map<String, Integer> requiredDevicesPerTag = DeviceOfferingHelper.getDeviceOfferingToAmountMap(deviceOfferingDeviceTagDao.getDeviceOfferingsTags(remainingOfferings));

        List<HostDeviceVO> releasedDevices = Transaction.execute(
                (TransactionCallback<List<HostDeviceVO>>) status -> {
                    List<HostDeviceVO> vmDevices = hostDeviceDao.listAndLockHostDevicesByVmId(vmId);
                    Map<String, Integer> surplusDevicesPerTag = DeviceOfferingHelper.getExceedingTagAmounts(countDevicesPerTag(vmDevices), requiredDevicesPerTag);

                    return releaseDevices(vm, selectDevicesToRelease(vmDevices, surplusDevicesPerTag));
                });

        logger.debug("Sending alerts to operators about host devices that were released from VM with ID {} and require cleanup.", vmId);
        sendAlertAboutCleanupDevices(releasedDevices);
    }

    private void sendAlertAboutCleanupDevices(List<HostDeviceVO> devices) {
        List<HostDeviceVO> filteredDevices = filterDevicesForAlert(devices, HostDevice.State.NeedsCleanup);

        if (filteredDevices.isEmpty()) {
            return;
        }

        String subject = "Cleanup operation needed";
        String body = "This host device was released from a VM and requires manual normalization to be available to users again.";
        sendAlertAboutDevices(filteredDevices, AlertType.ALERT_TYPE_HOST_DEVICE_NEEDS_CLEANUP, subject, body);
    }

    private void sendAlertAboutMissingDevices(List<HostDeviceVO> devices) {
        List<HostDeviceVO> filteredDevices = filterDevicesForAlert(devices, HostDevice.State.Missing);

        if (filteredDevices.isEmpty()) {
            return;
        }

        String subject = "Missing host device found during scan";
        String body = "This host device was not found during the host scan and requires attention. Please check the physical device for any issues.";
        sendAlertAboutDevices(filteredDevices, AlertType.ALERT_TYPE_HOST_DEVICE_MISSING, subject, body);
    }

    private List<HostDeviceVO> filterDevicesForAlert(List<HostDeviceVO> devices, HostDevice.State stateFilter) {
        if (CollectionUtils.isEmpty(devices)) {
            logger.debug("No host devices were received for alert sending. Skipping alert sending.");
            return new ArrayList<>();
        }

        List<HostDeviceVO> filteredDevices = devices.stream().filter(d -> d.getState().equals(stateFilter)).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(filteredDevices)) {
            logger.debug("No host devices that match [{}] state filter were found. Skipping alert sending.");
            return new ArrayList<>();
        }

        logger.debug("Found {} host devices that match the [{}] filter. Sending alert to operators.", filteredDevices.size(), stateFilter);
        return filteredDevices;
    }

    private void sendAlertAboutDevices(List<HostDeviceVO> devices, AlertType alertType, String baseSubject, String baseBody) {
        List<Long> hostIds = devices.stream().map(HostDeviceVO::getHostId).collect(Collectors.toList());
        List<HostVO> devicesHosts = hostDao.listByIds(hostIds);
        Map<Long, HostVO> idToHost = devicesHosts.stream().collect(Collectors.toMap(HostVO::getId, Function.identity()));

        for (HostDeviceVO device : devices) {
            String deviceSuffix = String.format(" - Host device [%s]", device.getDisplayName());
            String subject = baseSubject + deviceSuffix;

            HostVO host = idToHost.get(device.getHostId());
            String body = baseBody + String.format("\nDevice [%s] (ID: %s) from host [%s] (ID: %s)", device.getDisplayName(), device.getUuid(), host.getName(), host.getUuid());

            alertManager.sendAlert(alertType, host.getDataCenterId(), host.getPodId(), subject, body);
            logger.debug("Dispatched [{}] alert to operators about host device [{}].", alertType, device.getDisplayName());
        }
    }

    private Map<String, Integer> countDevicesPerTag(List<HostDeviceVO> devices) {
        return devices.stream().collect(Collectors.groupingBy(HostDeviceVO::getDeviceTag, Collectors.summingInt(device -> 1)));
    }

    private VirtualMachine getVirtualMachineOrThrow(Long vmId) {
        VirtualMachine vm = virtualMachineDao.findById(vmId);

        if (vm == null) {
            logger.debug("Virtual machine with ID {} was not found", vmId);
            throw new CloudRuntimeException("Virtual machine with id " + vmId + " was not found.");
        }

        return vm;
    }

    private List<HostDeviceVO> selectDevicesToRelease(List<HostDeviceVO> vmDevices, Map<String, Integer> tagToAmount) {
        Map<String, Integer> selectedAmountPerTag = new HashMap<>();
        List<HostDeviceVO> selectedDevices = new ArrayList<>();

        for (HostDeviceVO device : vmDevices) {
            String tag = device.getDeviceTag();

            if (selectedAmountPerTag.getOrDefault(tag, 0) < tagToAmount.getOrDefault(tag, 0)) {
                selectedAmountPerTag.merge(tag, 1, Integer::sum);
                selectedDevices.add(device);
            }
        }

        return selectedDevices;
    }

    private List<HostDeviceVO> releaseDevices(VirtualMachine vm, List<HostDeviceVO> devices) {
        if (CollectionUtils.isEmpty(devices)) {
            logger.debug("No host devices to be released from VM with ID {}. Skipping devices release process.", vm.getId());
            return new ArrayList<>();
        }

        logger.info("The following devices will be released from VM {}: {}", vm.getId(), devices.stream().map(HostDeviceVO::getPciName).collect(Collectors.toList()));

        List<HostDeviceVO> devicesOutsideAttachedState = devices.stream().filter(d -> !HostDevice.State.Attached.equals(d.getState())).collect(Collectors.toList());
        if (!devicesOutsideAttachedState.isEmpty()) {
            logger.error("The following devices are not in Attached state: {}. Cancelling device releasing process.", devicesOutsideAttachedState.stream().map(HostDeviceVO::getPciName).collect(Collectors.toList()));
            throw new CloudRuntimeException("There are inconsistent devices attached to this VM. Please, normalize them before release.");
        }

        for (HostDeviceVO dev : devices) {
            dev.setAccountId(null);
            dev.setDomainId(null);
            dev.setInstanceId(null);
            HostDevice.State nextState = dev.getOneTimeUse() ? HostDevice.State.NeedsCleanup : HostDevice.State.Free;
            dev.setState(nextState);
            hostDeviceDao.update(dev.getId(), dev);
        }

        long amount = devices.size();
        resourceLimitMgr.decrementResourceCount(vm.getAccountId(), Resource.ResourceType.host_device, amount);

        return devices;
    }

    @Override
    public void putHostDevicesInMaintenanceMode(Long hostId) {
        HostVO host = hostDao.findById(hostId);

        if (host == null) {
            logger.debug("Host with ID {} was not found", hostId);
            throw new CloudRuntimeException("Host with id " + hostId + " was not found.");
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                List<HostDeviceVO> devices = hostDeviceDao.listAndLockHostDevicesByHostIdAndState(hostId, HostDevice.State.Free);
                if (CollectionUtils.isEmpty(devices)) {
                    logger.debug("No host devices found for host with ID {} to be put in maintenance mode.", hostId);
                    return;
                }

                logger.info("The following devices will be put in maintenance mode for host {}: {}", hostId, devices.stream().map(HostDeviceVO::getPciName).collect(Collectors.toList()));
                Map<String, String> deviceNameToStateMap = devices.stream().collect(Collectors.toMap(HostDeviceVO::getPciName, d -> d.getState().toString()));

                for (HostDeviceVO dev : devices) {
                    dev.setState(HostDevice.State.HostInMaintenance);
                    hostDeviceDao.update(dev.getId(), dev);
                }

                hostDetailsDao.persist(hostId, deviceNameToStateMap);
            }
        });
    }

    @Override
    public void removeHostDevicesFromMaintenanceMode(long hostId) {
        HostVO host = hostDao.findById(hostId);

        if (host == null) {
            logger.debug("Host with ID {} was not found", hostId);
            throw new CloudRuntimeException("Host with id " + hostId + " was not found.");
        }

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                List<HostDeviceVO> devices = hostDeviceDao.listAndLockHostDevicesByHostIdAndState(hostId, HostDevice.State.HostInMaintenance);

                if (CollectionUtils.isEmpty(devices)) {
                    logger.debug("No host devices in maintenance found for host with ID {}", hostId);
                    return;
                }

                Map<String, String> hostDetails = hostDetailsDao.findDetails(hostId);

                for (HostDeviceVO dev : devices) {
                    String pciName = dev.getPciName();
                    String previousState = hostDetails.get(pciName);
                    if (previousState == null) {
                        logger.warn("Could not find host device [{}] last state before maintenance mode. Ignoring device during state normalization.", pciName);
                        continue;
                    }

                    dev.setState(HostDevice.State.valueOf(previousState));
                    hostDeviceDao.update(dev.getId(), dev);
                    hostDetailsDao.removeDetailByHostAndName(hostId, pciName);
                }
            }
        });
    }

    @Override
    public void updateVMHostDevicesOwnership(Long vmId, Account oldAccount, Account newAccount) {
        VirtualMachine vm = virtualMachineDao.findById(vmId);

        if (vm == null) {
            logger.debug("Virtual machine with ID {} was not found", vmId);
            throw new CloudRuntimeException("Virtual machine with id " + vmId + " was not found.");
        }

        logger.info("Updating ownership of host devices for VM {} to account {}.", vmId, newAccount.getUuid());

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                List<HostDeviceVO> hostDevices = hostDeviceDao.listAndLockHostDevicesByVmId(vmId);

                if (CollectionUtils.isEmpty(hostDevices)) {
                    logger.debug("No host devices found for VM with ID {}. Skipping devices ownership update.", vmId);
                    return;
                }

                for (HostDeviceVO device : hostDevices) {
                    device.setAccountId(newAccount.getId());
                    device.setDomainId(newAccount.getDomainId());
                    hostDeviceDao.update(device.getId(), device);
                    logger.debug("Updated ownership of host device {} to account {}.", device.getPciName(), newAccount.getId());
                }

                long amount = hostDevices.size();
                resourceLimitMgr.decrementResourceCount(oldAccount.getId(), Resource.ResourceType.host_device, amount);
                resourceLimitMgr.incrementResourceCount(newAccount.getId(), Resource.ResourceType.host_device, amount);
            }
        });
    }

    @Override
    public boolean reserveDevicesForVm(Long vmId, Long selectedHostId) {
        VMInstanceVO vm = virtualMachineDao.findById(vmId);

        if (vm == null) {
            logger.debug("Virtual machine with ID {} was not found", vmId);
            throw new CloudRuntimeException("Virtual machine with id " + vmId + " was not found.");
        }

        HostVO host = hostDao.findById(selectedHostId);

        if (host == null) {
            logger.debug("Host with ID {} was not found", selectedHostId);
            throw new CloudRuntimeException("Host with id " + selectedHostId + " was not found.");
        }

        List<DeviceOfferingVO> vmAssignedOfferings = deviceOfferingDao.listVirtualMachineDeviceOfferings(vmId);

        if (CollectionUtils.isEmpty(vmAssignedOfferings)) {
            logger.debug("No device offerings are assigned to VM {}. Therefore, the reservation process will be skipped.", vmId);
            return true;
        }

        List<HostDeviceVO> assignedDevices = hostDeviceDao.listHostDevicesByVmId(vmId);

        if (assignedDevices.stream().anyMatch(device -> !selectedHostId.equals(device.getHostId()))) {
            logger.error("VM {} holds devices {} that are not in host {}. It cannot be started in this host.", vmId, assignedDevices.stream().map(HostDeviceVO::getPciName).collect(Collectors.toList()), selectedHostId);
            throw new CloudRuntimeException(String.format("VM %s is bound to devices that are not in host %s, therefore it cannot be started in it.", vm.getUuid(), host.getUuid()));
        }

        Map<String, Integer> offeringsTags = DeviceOfferingHelper.getDeviceOfferingToAmountMap(deviceOfferingDeviceTagDao.getDeviceOfferingsTags(vmAssignedOfferings));
        Map<String, Integer> missingDevicesPerTag = DeviceOfferingHelper.getExceedingTagAmounts(offeringsTags, countDevicesPerTag(assignedDevices));

        if (missingDevicesPerTag.isEmpty()) {
            logger.debug("VM {} already holds the devices required by its device offerings. Therefore, the reservation process will be skipped.", vmId);
            return true;
        }

        int missingDevicesAmount = DeviceOfferingHelper.countOfferingTagsAmount(missingDevicesPerTag);

        logger.info("The following device offerings are assigned to VM {}: {}. Trying to reserve the {} missing matching devices in host {}.",
                vmId,
                vmAssignedOfferings.stream().map(DeviceOfferingVO::getUuid).collect(Collectors.toList()),
                missingDevicesAmount,
                selectedHostId);

        Account owner = accountManager.getActiveAccountById(vm.getAccountId());

        if (owner == null) {
            throw new CloudRuntimeException("Account with id " + vm.getAccountId() + " was not found.");
        }

        try (CheckedReservation hostDeviceReservation = new CheckedReservation(owner, Resource.ResourceType.host_device, null, (long) missingDevicesAmount, reservationDao, resourceLimitMgr)) {
            return Transaction.execute((TransactionCallback<Boolean>) status -> {
                List<HostDeviceVO> availableDevices = hostDeviceDao.listHostDevicesAvailableForAllocation(selectedHostId, new ArrayList<>(missingDevicesPerTag.keySet()));

                if (CollectionUtils.isEmpty(availableDevices)) {
                    logger.debug("No available host devices found for host with ID {}", selectedHostId);
                    return false;
                }

                List<HostDeviceVO> devicesToReserve = selectDevicesToReserve(availableDevices, missingDevicesPerTag, vmId);

                if (devicesToReserve == null) {
                    return false;
                }

                for (HostDeviceVO device : devicesToReserve) {
                    device.setAccountId(vm.getAccountId());
                    device.setDomainId(vm.getDomainId());
                    device.setInstanceId(vmId);
                    device.setState(HostDevice.State.Attached);
                    hostDeviceDao.update(device.getId(), device);
                    logger.debug("Reserved host device [{} - {}] for VM {}.", device.getDisplayName(), device.getPciName(), vm.getUuid());
                }

                long amount = devicesToReserve.size();
                resourceLimitMgr.incrementResourceCount(vm.getAccountId(), Resource.ResourceType.host_device, amount);

                return true;
            });
        } catch (ResourceAllocationException e) {
            logger.debug("Account [{}] cannot allocate {} more host devices: {}", owner.getUuid(), missingDevicesAmount, e.getMessage());
            throw new CloudRuntimeException(e.getMessage(), e);
        }
    }

    protected List<HostDeviceVO> selectDevicesToReserve(List<HostDeviceVO> availableDevices, Map<String, Integer> requiredDevicesPerTag, Long vmId) {
        Map<String, List<HostDeviceVO>> availableDevicesPerTag = availableDevices.stream().collect(Collectors.groupingBy(HostDeviceVO::getDeviceTag));
        List<HostDeviceVO> selectedDevices = new ArrayList<>();

        for (Map.Entry<String, Integer> requirement : requiredDevicesPerTag.entrySet()) {
            String deviceTag = requirement.getKey();
            int requiredAmount = requirement.getValue();
            List<HostDeviceVO> candidates = availableDevicesPerTag.getOrDefault(deviceTag, new ArrayList<>());

            if (candidates.size() < requiredAmount) {
                logger.debug("The host has {} available devices with tag [{}], but VM {} requires {}.", candidates.size(), deviceTag, vmId, requiredAmount);
                return null;
            }

            selectedDevices.addAll(candidates.subList(0, requiredAmount));
        }

        return selectedDevices;
    }

    @Override
    public boolean hasHostDevicesReservedForVm(Long vmId) {
        return CollectionUtils.isNotEmpty(hostDeviceDao.listHostDevicesByVmId(vmId));
    }

    @Override
    public boolean doesHostMatchVmDeviceOfferings(Host host, Long virtualMachineId) {
        return doesHostMatchDeviceOfferingTags(host, deviceOfferingDao.listVirtualMachineDeviceOfferings(virtualMachineId), virtualMachineId);
    }

    @Override
    public boolean doesHostMatchDeviceOfferingTags(Host host, List<? extends DeviceOffering> deviceOfferings, Long virtualMachineId) {
        if (CollectionUtils.isEmpty(deviceOfferings)) {
            logger.debug("No device offerings were informed, therefore host {} satisfies the device offering requirements.", host.getId());
            return true;
        }

        Map<String, Integer> deviceOfferingsTags = DeviceOfferingHelper.getDeviceOfferingToAmountMap(deviceOfferingDeviceTagDao.getDeviceOfferingsTags(deviceOfferings));

        if (virtualMachineId != null) {
            List<HostDeviceVO> vmDevices = hostDeviceDao.listHostDevicesByVmId(virtualMachineId);

            boolean areDevicesInAnotherHost = vmDevices.stream().anyMatch(device -> !Long.valueOf(host.getId()).equals(device.getHostId()));

            if (areDevicesInAnotherHost) {
                logger.debug("VM {} holds devices {} that are not in host {}, therefore this host does not satisfy the device offering requirements.", virtualMachineId, vmDevices.stream().map(HostDeviceVO::getPciName).collect(Collectors.toList()), host.getId());
                return false;
            }
        }

        List<HostDeviceVO> hostDevices = hostDeviceDao.listHostDevicesForOfferingAndVmCheck(host.getId(), new ArrayList<>(deviceOfferingsTags.keySet()), virtualMachineId);

        int necessaryDeviceAmount = DeviceOfferingHelper.countOfferingTagsAmount(deviceOfferingsTags);
        if (hostDevices.size() < necessaryDeviceAmount) {
            logger.debug("Host {} has {} candidate devices, which is less than the {} devices required by the device offerings.", host.getId(), hostDevices.size(), necessaryDeviceAmount);
            return false;
        }

        List<String> hostDevicesTags = hostDevices.stream().map(HostDeviceVO::getDeviceTag).collect(Collectors.toList());

        return validateHostDevicesAgainstDeviceOfferings(deviceOfferingsTags, hostDevicesTags);
    }

    protected boolean validateHostDevicesAgainstDeviceOfferings(Map<String, Integer> deviceOfferingsTags, List<String> hostDevicesTags) {
        Map<String, Integer> devicesTagsCountMap = new HashMap<>();

        for (String tag : hostDevicesTags) {
            devicesTagsCountMap.merge(tag, 1, Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : deviceOfferingsTags.entrySet()) {
            String tag = entry.getKey();
            int required = entry.getValue();
            int returned = devicesTagsCountMap.getOrDefault(tag, 0);

            if (returned < required) {
                return false;
            }
        }

        return true;
    }

    private void triggerAutomaticScanForClusters() {
        ThreadContext.put(LOGCONTEXTID, UuidUtils.first(UUID.randomUUID().toString()));

        try {
            scanClustersWithAutomaticScanEnabled();
        } catch (Exception e) {
            logger.error("Unexpected failure during the automatic host device scan task.", e);
        } finally {
            ThreadContext.remove(LOGCONTEXTID);
        }
    }

    private void scanClustersWithAutomaticScanEnabled() {
        List<ClusterVO> clusters = clusterDao.listAll()
                .stream()
                .filter(c -> Hypervisor.HypervisorType.KVM.equals(c.getHypervisorType()))
                .filter(c -> HostDeviceAutomaticScanEnabled.valueIn(c.getId()))
                .collect(Collectors.toList());

        logger.info("Automatic device scan task started. Found {} clusters with automatic scan enabled.", clusters.size());

        for (ClusterVO cluster : clusters) {
            boolean shouldExecute = Transaction.execute((TransactionCallback<Boolean>) status -> {
                clusterDao.lockRow(cluster.getId(), true);

                Integer scanInterval = HostDeviceAutomaticScanInterval.valueIn(cluster.getId());
                long now = System.currentTimeMillis() / 1000L;

                ClusterDetailsVO clusterLastExecution = clusterDetailsDao.findDetail(cluster.getId(), HostDevicesManager.LAST_HOST_DEVICE_SCAN_EXECUTION_TIMESTAMP);

                if (clusterLastExecution != null) {
                    logger.debug("Found the following last execution timestamp for cluster {}: {}", cluster.getId(), clusterLastExecution.getValue());
                    long lastExecutionTimestamp = Long.parseLong(clusterLastExecution.getValue());

                    if (now - lastExecutionTimestamp < scanInterval) {
                        logger.info("Skipping automatic device scan for cluster {} because the last execution was {} seconds ago, which is less than the configured interval of {} seconds.", cluster.getId(), now - lastExecutionTimestamp, scanInterval);
                        return false;
                    }

                    logger.debug("Updating last execution timestamp for cluster {} to {}", cluster.getId(), now);
                    clusterLastExecution.setValue(String.valueOf(now));
                    clusterDetailsDao.update(clusterLastExecution.getId(), clusterLastExecution);

                    return true;
                }

                logger.debug("This is the first execution for cluster {}. Creating new entry for it with current timestamp.", cluster.getId());
                clusterDetailsDao.persist(cluster.getId(), HostDevicesManager.LAST_HOST_DEVICE_SCAN_EXECUTION_TIMESTAMP, String.valueOf(now));

                return true;
            });

            if (!shouldExecute) {
                continue;
            }

            List<HostVO> hosts = hostDao.listAllRoutingHostsUpInClusters(List.of(cluster.getId()), Hypervisor.HypervisorType.KVM);

            if (CollectionUtils.isEmpty(hosts)) {
                logger.info("No suitable KVM hosts found in cluster {} for automatic device scan.", cluster.getId());
                continue;
            }

            logger.info("Scanning host devices of {} hosts in cluster {}.", hosts.size(), cluster.getId());
            Map<String, String> failureReasonByHostUuid = scanHostDevices(hosts);

            if (!failureReasonByHostUuid.isEmpty()) {
                logger.warn("The automatic device scan of cluster {} failed for {} out of {} hosts.", cluster.getId(), failureReasonByHostUuid.size(), hosts.size());
            }
        }
    }

    @Override
    public String getConfigComponentName() {
        return HostDevicesManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                DefaultMaxAccountHostDevices,
                DefaultMaxDomainHostDevices,
                DefaultMaxProjectHostDevices,
                HostDeviceAutomaticScanEnabled,
                HostDeviceAutomaticScanInterval
        };
    }

    @Override
    public List<Class<?>> getCommands() {
        return List.of(ScanHostDevicesCmd.class, ListHostDevicesCmd.class, UpdateHostDeviceCmd.class);
    }
}
