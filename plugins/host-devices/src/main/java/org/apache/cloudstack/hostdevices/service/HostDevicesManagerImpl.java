package org.apache.cloudstack.hostdevices.service;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ScanDevicesCommand;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.org.Cluster;
import com.cloud.user.Account;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.hostdevices.api.command.ScanHostDevicesCmd;
import org.apache.cloudstack.hostdevices.persistence.HostDevice;
import org.apache.cloudstack.hostdevices.persistence.HostDeviceDao;
import org.apache.cloudstack.hostdevices.persistence.HostDeviceVO;
import org.apache.cloudstack.kvm.libvirt.mappers.serialization.LibvirtDeviceDeserializer;
import org.apache.cloudstack.kvm.libvirt.model.LibvirtDevice;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HostDevicesManagerImpl extends ManagerBase implements HostDevicesManager {
    @Inject
    AgentManager agentManager;
    @Inject
    HostDao hostDao;
    @Inject
    ClusterDao clusterDao;
    @Inject
    HostDeviceDao hostDeviceDao;

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
    public String getConfigComponentName() {
        return HostDevicesService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[0];
    }

    @Override
    public List<Class<?>> getCommands() {
        return List.of(ScanHostDevicesCmd.class);
    }
}
