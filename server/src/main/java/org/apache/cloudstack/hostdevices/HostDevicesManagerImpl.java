package org.apache.cloudstack.hostdevices;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ScanDevicesCommand;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hostdevices.HostDeviceVO;
import com.cloud.hostdevices.dao.HostDeviceDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.org.Cluster;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.cloudstack.api.command.admin.hostdevices.ScanHostDevicesCmd;
import org.apache.cloudstack.api.command.admin.hostdevices.UpdateHostDeviceCmd;
import org.apache.cloudstack.api.command.user.hostdevices.ListHostDevicesCmd;
import org.apache.cloudstack.api.response.HostDeviceResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.utils.libvirt.mappers.serialization.LibvirtDeviceDeserializer;
import org.apache.cloudstack.utils.libvirt.model.LibvirtDevice;
import org.apache.commons.lang3.ObjectUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HostDevicesManagerImpl extends ManagerBase implements org.apache.cloudstack.hostdevices.HostDevicesManager {
    @Inject
    AgentManager agentManager;
    @Inject
    HostDao hostDao;
    @Inject
    ClusterDao clusterDao;
    @Inject
    HostDeviceDao hostDeviceDao;
    @Inject
    AccountManager accountManager;
    @Inject
    VMInstanceDao virtualMachineDao;
    @Inject
    AccountDao accountDao;
    @Inject
    DomainDao domainDao;

    private static final ObjectMapper MAPPER = createMapper();

    public HostDevicesManagerImpl() {
    }

    @Override
    public void scanHostDevice(ScanHostDevicesCmd cmd) {
        Long hostId = cmd.getHostId();
        Long clusterId = cmd.getClusterId();
        Long zoneId = cmd.getZoneId();
        Account caller = CallContext.current().getCallingAccount();

        if (!caller.getType().equals(Account.Type.ADMIN)) {
            logger.error("Cancelling devices scan because caller is not ROOT admin.");
            throw new InvalidParameterValueException("Scanning host devices is not allowed for non-ROOT Admins.");
        }

        List<HostVO> hostsListForDeviceScan = getHostsListForDeviceScan(zoneId, clusterId, hostId);

        if (hostsListForDeviceScan == null) {
            logger.error("At least one of the parameters (zoneId, clusterId, hostId) must be provided.");
            throw new InvalidParameterValueException("Failed to retrieve hosts for device scan.");
        }

        logger.debug("Selected {} hosts for device scan: {}", hostsListForDeviceScan.size(), hostsListForDeviceScan.stream().map(HostVO::getId));

        ScanDevicesCommand command = new ScanDevicesCommand();

        for (HostVO host : hostsListForDeviceScan) {
            try {
                logger.debug("Sending ScanDevicesCommand to host with ID {}", host.getId());
                Answer answer = agentManager.send(host.getId(), command);

                if (!answer.getResult()) {
                    logger.error("Some error occurred while trying to scan devices from host {}: {}", host.getId(), answer.getDetails());
                    throw new CloudRuntimeException("Failure to scan devices of host with ID " + host.getUuid() + " due to " + answer.getDetails());
                }

                List<? extends LibvirtDevice> returnedDevices = MAPPER.readValue(answer.getDetails(), MAPPER.getTypeFactory().constructCollectionType(List.class, LibvirtDevice.class));
                compareIncomingDevicesWithExistingOnes(returnedDevices, host);
            } catch (Exception e) {
                logger.error("Failed to send ScanDevicesCommand to host with ID {}: {}", host.getId(), e.getMessage());
                throw new CloudRuntimeException("Failure to scan devices of host with ID " + host.getUuid() + ". Please check the logs.");
            }
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

    private void compareIncomingDevicesWithExistingOnes(List<? extends LibvirtDevice> incomingDevices, HostVO host) {
        List<HostDeviceVO> currentDevices = hostDeviceDao.listHostDevicesByHostId(host.getId());
        logger.debug("The following host devices are registered for host with ID {}: {}", host.getId(), currentDevices);

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

    private void handleMissingDevices(List<HostDeviceVO> registeredDevices, List<HostDeviceVO> incomingDevices) {
        logger.debug("Checking for devices that were listed in the database, but were not returned by the host. Only Attached ones will be considered as missing.");
        List<HostDeviceVO> missingDevices = registeredDevices
                .stream()
                .filter(rd -> incomingDevices
                        .stream()
                        .noneMatch(id -> id.getPciName().equals(rd.getPciName())))
                .filter(d -> d.getInstanceId() != null)
                .collect(Collectors.toList());

        if (missingDevices.isEmpty()) {
            logger.debug("No missing devices.");
            return;
        }

        logger.debug("Found the following missing devices. {}", missingDevices);
        // TODO: implement alert creation and mail sending

        for (HostDeviceVO device : missingDevices) {
            device.setState(HostDevice.State.Missing);
            hostDeviceDao.persist(device);
        }

        incomingDevices.removeIf(id -> missingDevices.stream().anyMatch(md -> md.getPciName().equals(id.getPciName())));
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
        logger.debug("Saving unregistered devices to the database.");

        for (HostDeviceVO device : unregisteredDevices) {
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
    public List<? extends HostDevice> listHostDevices(ListHostDevicesCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long hostDeviceId = cmd.getId();
        Long accountId = cmd.getAccountId();
        Long hostId = cmd.getHostId();
        Long virtualMachineId = cmd.getVirtualMachineId();
        String deviceTag = cmd.getDeviceTag();
        String stringDeviceType = cmd.getType();
        String stringDeviceState = cmd.getState();

        HostDevice.State state = null;
        if (stringDeviceState != null) {
            state = HostDevice.State.getFromString(stringDeviceState);

            if (state == null) {
                logger.debug("Invalid state [{}] provided for host device listing. Supported devices are: {}", stringDeviceState, Arrays.toString(HostDevice.State.values()));
                throw new InvalidParameterValueException("Invalid state " + stringDeviceState + " provided for host device listing.");
            }
        }

        HostDevice.Type type = null;
        if (stringDeviceType != null) {
            type = HostDevice.Type.getFromString(stringDeviceType);

            if (type == null) {
                logger.debug("Invalid type [{}] provided for host device listing. Supported devices are: {}", stringDeviceType, Arrays.toString(HostDevice.Type.values()));
                throw new InvalidParameterValueException("Invalid type " + stringDeviceType + " provided for host device listing.");
            }
        }

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

        return hostDeviceDao.listHostDevices(hostDeviceId, accountId, domainIds, hostId, virtualMachineId, deviceTag, state, type);
    }

    @Override
    public HostDeviceResponse generateHostDeviceResponse(HostDevice device) {
        HostDeviceResponse res = new HostDeviceResponse();

        res.setId(device.getUuid());
        res.setDisplayName(device.getDisplayName());
        res.setPciName(device.getPciName());
        res.setPciDomain(device.getPciDomain());
        res.setPciClass(device.getPciClass());
        res.setPciSlot(device.getPciSlot());
        res.setPciFunction(device.getPciFunction());
        res.setVendorId(device.getPciVendorId());
        res.setDeviceId(device.getPciDeviceId());
        res.setCreated(device.getCreated());
        res.setRemoved(device.getRemoved());
        res.setState(device.getState().toString());
        res.setType(device.getType().toString());
        res.setDeviceTag(res.getDeviceTag());

        if (device.getInstanceId() != null) {
            VirtualMachine vm = virtualMachineDao.findById(device.getInstanceId());
            if (vm != null) {
                res.setInstanceId(vm.getUuid());
            }

            Account account = accountManager.getActiveAccountById(device.getInstanceId());
            if (account != null) {
                res.setAccountId(account.getUuid());

                Domain domain = domainDao.findById(account.getDomainId());
                if (domain != null) {
                    res.setDomainId(domain.getUuid());
                }
            }
        }

        Host host = hostDao.findById(device.getHostId());
        if (host != null) {
            res.setHostId(host.getUuid());
        }

        return res;
    }

    @Override
    public HostDevice updateHostDevice(UpdateHostDeviceCmd updateHostDeviceCmd) {
        Boolean enabled = updateHostDeviceCmd.getEnabled();
        String displayName = updateHostDeviceCmd.getDisplayName();
        String tag = updateHostDeviceCmd.getTag();
        String type = updateHostDeviceCmd.getType();

        if (ObjectUtils.allNull(enabled, displayName, tag, type)) {
            throw new InvalidParameterValueException("At least one of the following parameters must be provided: enabled, displayName, tags, type");
        }

        HostDeviceVO device = hostDeviceDao.findById(updateHostDeviceCmd.getDeviceId());

        if (device == null) {
            logger.debug("Host device with ID {} was not found", updateHostDeviceCmd.getDeviceId());
            throw new InvalidParameterValueException("Host device with id " + updateHostDeviceCmd.getDeviceId() + " was not found.");
        }

        if (!device.canBeUpdated()) {
            logger.error("Current device state is {}. Only devices in Disabled or Free state can be updated.", device.getState());
            throw new InvalidParameterValueException(String.format("Devices in state %s cannot be updated. Valid states for update are %s and %s.", device.getState(), HostDevice.State.Disabled, HostDevice.State.Free));
        }

        HostDevice.Type newDeviceType = null;
        if (type != null) {
            newDeviceType = HostDevice.Type.getFromString(type);
            if (newDeviceType == null) {
                throw new InvalidParameterValueException(String.format("Invalid host device type: %s. Supported types are: %s", type, Arrays.toString(HostDevice.Type.values())));
            }
        }

        device.updateData(enabled, displayName, tag, newDeviceType);
        hostDeviceDao.persist(device);

        return device;
    }

    @Override
    public String getConfigComponentName() {
        return HostDevicesManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[0];
    }

    @Override
    public List<Class<?>> getCommands() {
        return List.of(ScanHostDevicesCmd.class, ListHostDevicesCmd.class, UpdateHostDeviceCmd.class);
    }
}
