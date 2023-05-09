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

package org.apache.cloudstack.backup;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.google.gson.Gson;
import javax.inject.Inject;

import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.UsageEventVO;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.backup.Backup.Metric;
import org.apache.cloudstack.backup.Backup.RestorePoint;
import org.apache.cloudstack.backup.dao.BackupDao;
import org.apache.cloudstack.backup.dao.BackupOfferingDao;
import org.apache.cloudstack.backup.veeam.VeeamClient;
import org.apache.cloudstack.backup.veeam.api.Job;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.utils.volume.VirtualMachineDiskInfo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.log4j.Logger;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.vmware.VmwareDatacenter;
import com.cloud.hypervisor.vmware.VmwareDatacenterZoneMap;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterDao;
import com.cloud.hypervisor.vmware.dao.VmwareDatacenterZoneMapDao;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;

public class VeeamBackupProvider extends AdapterBase implements BackupProvider, Configurable {

    private static final Logger LOG = Logger.getLogger(VeeamBackupProvider.class);
    private static final Gson GSON = GsonHelper.getGson();
    public static final String BACKUP_IDENTIFIER = "-CSBKP-";

    public ConfigKey<String> VeeamUrl = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.veeam.url", "https://localhost:9398/api/",
            "The Veeam backup and recovery URL.", true, ConfigKey.Scope.Zone);

    private ConfigKey<String> VeeamUsername = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.veeam.username", "administrator",
            "The Veeam backup and recovery username.", true, ConfigKey.Scope.Zone);

    private ConfigKey<String> VeeamPassword = new ConfigKey<>("Secure", String.class,
            "backup.plugin.veeam.password", "",
            "The Veeam backup and recovery password.", true, ConfigKey.Scope.Zone);

    private ConfigKey<Boolean> VeeamValidateSSLSecurity = new ConfigKey<>("Advanced", Boolean.class, "backup.plugin.veeam.validate.ssl", "false",
            "When set to true, this will validate the SSL certificate when connecting to https/ssl enabled Veeam API service.", true, ConfigKey.Scope.Zone);

    private ConfigKey<Integer> VeeamApiRequestTimeout = new ConfigKey<>("Advanced", Integer.class, "backup.plugin.veeam.request.timeout", "300",
            "The Veeam B&R API request timeout in seconds.", true, ConfigKey.Scope.Zone);

    private static ConfigKey<Integer> VeeamRestoreTimeout = new ConfigKey<>("Advanced", Integer.class, "backup.plugin.veeam.restore.timeout", "600",
            "The Veeam B&R API restore backup timeout in seconds.", true, ConfigKey.Scope.Zone);

    @Inject
    private VmwareDatacenterZoneMapDao vmwareDatacenterZoneMapDao;
    @Inject
    private VmwareDatacenterDao vmwareDatacenterDao;
    @Inject
    private BackupDao backupDao;
    @Inject
    private BackupOfferingDao backupOfferingDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private VolumeDao volumeDao;

    protected VeeamClient getClient(final Long zoneId) {
        try {
            return new VeeamClient(VeeamUrl.valueIn(zoneId), VeeamUsername.valueIn(zoneId), VeeamPassword.valueIn(zoneId),
                    VeeamValidateSSLSecurity.valueIn(zoneId), VeeamApiRequestTimeout.valueIn(zoneId), VeeamRestoreTimeout.valueIn(zoneId));
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Failed to parse Veeam API URL: " + e.getMessage());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOG.error("Failed to build Veeam API client due to: ", e);
        }
        throw new CloudRuntimeException("Failed to build Veeam API client");
    }

    public List<BackupOffering> listBackupOfferings(final Long zoneId) {
        List<BackupOffering> policies = new ArrayList<>();
        for (final BackupOffering policy : getClient(zoneId).listJobs()) {
            if (!policy.getName().contains(BACKUP_IDENTIFIER)) {
                policies.add(policy);
            }
        }
        return policies;
    }

    @Override
    public boolean isValidProviderOffering(final Long zoneId, final String uuid) {
        List<BackupOffering> policies = listBackupOfferings(zoneId);
        if (CollectionUtils.isEmpty(policies)) {
            return false;
        }
        for (final BackupOffering policy : policies) {
            if (policy.getExternalId().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    private VmwareDatacenter findVmwareDatacenterForVM(final VirtualMachine vm) {
        if (vm == null || vm.getHypervisorType() != Hypervisor.HypervisorType.VMware) {
            throw new CloudRuntimeException("The Veeam backup provider is only applicable for VMware VMs");
        }
        final VmwareDatacenterZoneMap zoneMap = vmwareDatacenterZoneMapDao.findByZoneId(vm.getDataCenterId());
        if (zoneMap == null) {
            throw new CloudRuntimeException("Failed to find a mapped VMware datacenter for zone id:" + vm.getDataCenterId());
        }
        final VmwareDatacenter vmwareDatacenter = vmwareDatacenterDao.findById(zoneMap.getVmwareDcId());
        if (vmwareDatacenter == null) {
            throw new CloudRuntimeException("Failed to find a valid VMware datacenter mapped for zone id:" + vm.getDataCenterId());
        }
        return vmwareDatacenter;
    }

    private String getGuestBackupName(final String instanceName, final String uuid) {
        return String.format("%s%s%s", instanceName, BACKUP_IDENTIFIER, uuid);
    }

    @Override
    public boolean assignVMToBackupOffering(final VirtualMachine vm, final BackupOffering backupOffering) {
        final VeeamClient client = getClient(vm.getDataCenterId());
        final Job parentJob = client.listJob(backupOffering.getExternalId());
        final String clonedJobName = getGuestBackupName(vm.getInstanceName(), backupOffering.getUuid());

        if (!client.cloneVeeamJob(parentJob, clonedJobName)) {
            LOG.error("Failed to clone pre-defined Veeam job (backup offering) for backup offering ID: " + backupOffering.getExternalId() + " but will check the list of jobs again if it was eventually succeeded.");
        }

        for (final BackupOffering job : client.listJobs()) {
            if (job.getName().equals(clonedJobName)) {
                final Job clonedJob = client.listJob(job.getExternalId());
                if (BooleanUtils.isTrue(clonedJob.getScheduleConfigured()) && !clonedJob.getScheduleEnabled()) {
                    client.toggleJobSchedule(clonedJob.getId());
                }
                LOG.debug("Veeam job (backup offering) for backup offering ID: " + backupOffering.getExternalId() + " found, now trying to assign the VM to the job.");
                final VmwareDatacenter vmwareDC = findVmwareDatacenterForVM(vm);
                if (client.addVMToVeeamJob(job.getExternalId(), vm.getInstanceName(), vmwareDC.getVcenterHost())) {
                    ((VMInstanceVO) vm).setBackupExternalId(job.getExternalId());
                    ((VMInstanceVO) vm).setBackupName(clonedJobName);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean removeVMFromBackupOffering(final VirtualMachine vm, boolean removeBackups) {
        final VeeamClient client = getClient(vm.getDataCenterId());
        final VmwareDatacenter vmwareDC = findVmwareDatacenterForVM(vm);

        final String clonedJobName = vm.getBackupName();
        boolean result = false;

        if (removeBackups) {
            result = client.deleteJobAndBackup(clonedJobName);
        } else {
            result = client.disableJob(clonedJobName);
        }

        if (!result) {
            LOG.warn(String.format("Failed to remove Veeam %s for job: [name: %s].", removeBackups ? "job and backup" : "job", clonedJobName));
            throw new CloudRuntimeException("Failed to delete Veeam B&R job, an operation may be in progress. Please try again after some time.");
        }
        return true;
    }

    @Override
    public boolean willDeleteBackupsOnOfferingRemoval() {
        return true;
    }

    @Override
    public boolean takeBackup(final VirtualMachine vm) {
        final VeeamClient client = getClient(vm.getDataCenterId());
        return client.startBackupJob(vm.getBackupExternalId());
    }

    @Override
    public boolean deleteBackup(Backup backup, boolean forced) {
        VMInstanceVO vm = vmInstanceDao.findByIdIncludingRemoved(backup.getVmId());
        if (vm == null) {
            throw new CloudRuntimeException(String.format("Could not find any VM associated with the Backup [uuid: %s, externalId: %s].", backup.getUuid(), backup.getExternalId()));
        }
        if (!forced) {
            LOG.debug(String.format("Veeam backup provider does not have a safe way to remove a single restore point, which results in all backup chain being removed. "
                    + "More information about this limitation can be found in the links: [%s, %s].", "https://forums.veeam.com/powershell-f26/removing-a-single-restorepoint-t21061.html",
                    "https://helpcenter.veeam.com/docs/backup/vsphere/retention_separate_vms.html?ver=110"));
            throw new CloudRuntimeException("Veeam backup provider does not have a safe way to remove a single restore point, which results in all backup chain being removed. "
                    + "Use forced:true to skip this verification and remove the complete backup chain.");
        }
        VeeamClient client = getClient(vm.getDataCenterId());
        boolean result = client.deleteBackup(backup.getExternalId());
        if (BooleanUtils.isFalse(result)) {
            return false;
        }

        List<Backup> allBackups = backupDao.listByVmId(backup.getZoneId(), backup.getVmId());
        for (Backup b : allBackups) {
            if (b.getId() != backup.getId()) {
                backupDao.remove(b.getId());
            }
        }
        return result;
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, Backup backup) {
        final String restorePointId = backup.getExternalId();
        return getClient(vm.getDataCenterId()).restoreFullVM(vm.getInstanceName(), restorePointId);
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, String volumeUuid, String host, String dataStore, VirtualMachine vm, Boolean startVm) {
        Pair<Boolean, String> result = new Pair<>(false, "");
        final Long zoneId = backup.getZoneId();
        final String restorePointId = backup.getExternalId();

        VMInstanceVO vmVO = vmInstanceDao.findById(backup.getVmId());
        VolumeVO volumeVO = volumeDao.findByUuid(volumeUuid);
        long totalDeviceIds = volumeDao.findByInstance(vm.getId()).stream().mapToLong(VolumeVO::getDeviceId).max().orElse(0L);
        long newDeviceId = totalDeviceIds + 1;
        LOG.debug(String.format("VM [%s] has [%s] deviceIds. Trying to restore volume [%s] using restorePoint [%s] and with [%s] as the new deviceId.", vm.getUuid(),
                totalDeviceIds, volumeUuid, restorePointId, newDeviceId));

        VirtualMachineDiskInfo fromJson = GSON.fromJson(volumeVO.getChainInfo(), VirtualMachineDiskInfo.class);
        String type = fromJson.getControllerFromDeviceBusName().toUpperCase();
        String virtualDeviceNode = StringUtils.substringAfter(fromJson.getDiskDeviceBusName(), ":");
        for (String name : fromJson.getDiskChain()) {
            String diskName = StringUtils.substringAfter(name, "/");
            try {
                result = getClient(zoneId).restoreVolume(volumeUuid, vmVO.getUuid(), restorePointId, host, dataStore, type, virtualDeviceNode, diskName, newDeviceId, vm, startVm);
            } catch (Exception e) {
                LOG.error(String.format("Failed to restore volume [%s] in VM [%s], with type [%s], node [%s] and disk name [%s], using target host [%s] and datastore [%s] due to [%s].",
                        volumeUuid, vmVO.getUuid(), type, virtualDeviceNode, diskName, host, dataStore, e.getMessage()), e);
            }
        }
        return result;
    }

    @Override
    public Map<VirtualMachine, Backup.Metric> getBackupMetrics(final Long zoneId, final List<VirtualMachine> vms) {
        final Map<VirtualMachine, Backup.Metric> metrics = new HashMap<>();
        final Map<String, Backup.Metric> backendMetrics = getClient(zoneId).getBackupMetrics();
        if (backendMetrics.isEmpty()) {
            return metrics;
        }
        List<VMInstanceVO> collect = vmInstanceDao.listByZoneIdAndTypeIncludingRemoved(zoneId, VirtualMachine.Type.User).stream().filter(Objects::nonNull).filter(t -> t.getHypervisorType().equals(Hypervisor.HypervisorType.VMware)).collect(Collectors.toList());

        for (VMInstanceVO vm : collect) {
            if (vm == null || !backendMetrics.containsKey(vm.getInstanceName())) {
                continue;
            }

            Metric metric = backendMetrics.get(vm.getInstanceName());
            LOG.debug(String.format("Metrics for VM [uuid: %s, name: %s] is [backup size: %s, data size: %s].", vm.getUuid(),
                    vm.getInstanceName(), metric.getBackupSize(), metric.getDataSize()));
            metrics.put(vm, metric);
        }
        return metrics;
    }

    private List<Backup.RestorePoint> listRestorePoints(VirtualMachine vm) {
        return getClient(vm.getDataCenterId()).listRestorePoints(vm.getBackupName(), vm.getInstanceName());
    }

    private Backup checkAndUpdateIfBackupEntryExistsForRestorePoint(List<Backup> backupsInDb, Backup.RestorePoint restorePoint, Backup.Metric metric) {
        for (final Backup backup : backupsInDb) {
            if (restorePoint.getId().equals(backup.getExternalId())) {
                if (metric != null) {
                    LOG.debug(String.format("Update backup with [uuid: %s, external id: %s] from [size: %s, protected size: %s] to [size: %s, protected size: %s].",
                            backup.getUuid(), backup.getExternalId(), backup.getSize(), backup.getProtectedSize(), metric.getBackupSize(), metric.getDataSize()));

                    ((BackupVO) backup).setSize(metric.getBackupSize());
                    ((BackupVO) backup).setProtectedSize(metric.getDataSize());
                    backupDao.update(backup.getId(), ((BackupVO) backup));
                }
                return backup;
            }
        }
        return null;
    }

    @Override
    public void syncBackups(VirtualMachine vm, Backup.Metric metric) {
        List<Backup.RestorePoint> restorePoints = listRestorePoints(vm);
        if (CollectionUtils.isEmpty(restorePoints)) {
            LOG.debug(String.format("Can't find any restore point to VM: [uuid: %s, name: %s].", vm.getUuid(), vm.getInstanceName()));
            return;
        }
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                final List<Backup> backupsInDb = backupDao.listByVmId(null, vm.getId());
                final List<Long> removeList = backupsInDb.stream().map(InternalIdentity::getId).collect(Collectors.toList());
                for (final Backup.RestorePoint restorePoint : restorePoints) {
                    if (!(restorePoint.getId() == null || restorePoint.getType() == null || restorePoint.getCreated() == null)) {
                        Backup existingBackupEntry = checkAndUpdateIfBackupEntryExistsForRestorePoint(backupsInDb, restorePoint, metric);
                        if (existingBackupEntry != null) {
                            removeList.remove(existingBackupEntry.getId());
                            continue;
                        }
                        BackupVO backup = new BackupVO();
                        backup.setVmId(vm.getId());
                        backup.setExternalId(restorePoint.getId());
                        backup.setType(restorePoint.getType());
                        backup.setDate(restorePoint.getCreated());
                        backup.setStatus(Backup.Status.BackedUp);
                        if (metric != null) {
                            backup.setSize(metric.getBackupSize());
                            backup.setProtectedSize(metric.getDataSize());
                        }
                        backup.setBackupOfferingId(findBackupOfferingOfBackup(restorePoint, vm));
                        backup.setAccountId(vm.getAccountId());
                        backup.setDomainId(vm.getDomainId());
                        backup.setZoneId(vm.getDataCenterId());

                        LOG.debug(String.format("Creating a new entry in backups: [uuid: %s, vm_id: %s, external_id: %s, type: %s, date: %s, backup_offering_id: %s, account_id: %s, "
                                        + "domain_id: %s, zone_id: %s].", backup.getUuid(), backup.getVmId(), backup.getExternalId(), backup.getType(), backup.getDate(),
                                backup.getBackupOfferingId(), backup.getAccountId(), backup.getDomainId(), backup.getZoneId()));
                        BackupVO persistedBackup = backupDao.persist(backup);

                        if (persistedBackup != null) {
                            Map<String, String> details = new HashMap<>();
                            details.put(UsageEventVO.DynamicParameters.vmId.name(), String.valueOf(vm.getId()));

                            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VM_BACKUP_CREATE, persistedBackup.getAccountId(), persistedBackup.getZoneId(), persistedBackup.getId(),
                                    String.format("Backup %s - VM %s", backup.getUuid(), vm.getUuid()), persistedBackup.getBackupOfferingId(), null, persistedBackup.getSize(),
                                    persistedBackup.getProtectedSize(), Backup.class.getName(), persistedBackup.getUuid(), details);
                        }
                    }
                }
                for (final Long backupIdToRemove : removeList) {
                    LOG.warn(String.format("Removing backup with ID: [%s].", backupIdToRemove));
                    backupDao.remove(backupIdToRemove);
                }
            }
        });
    }

    private long findBackupOfferingOfBackup(RestorePoint restorePoint, VirtualMachine vm) {
        LOG.debug(String.format("Trying to find backup offering of restore point [%s] of VM [%s] using backup UUID [%s].", restorePoint.getId(), vm.getUuid(), restorePoint.getBackupUuid()));
        BackupOffering backupOffering = backupOfferingDao.findByUuid(restorePoint.getBackupUuid());
        if (backupOffering != null) {
            return backupOffering.getId();
        }
        LOG.warn(String.format("Could not find any backup offering with UUID [%s] used by restore point [%s]. " +
                        "Trying to use the ID [%s] data from vm_instance table instead.", restorePoint.getBackupUuid(),
                restorePoint.getId(), vm.getBackupOfferingId()));
        backupOffering = backupOfferingDao.findById(vm.getBackupOfferingId());
        if (backupOffering == null) {
            String errMsg = String.format("Could not find any backup offering with ID [%s] or UUID [%s] used by VM [%s].", vm.getBackupOfferingId(), restorePoint.getBackupUuid(), vm.getUuid());
            LOG.warn(errMsg);
            throw new RuntimeException(errMsg);
        }
        return backupOffering.getId();
    }

    protected String createVolumeInfoFromVolumes(List<String> paths) {
        List<VolumeVO> vmVolumes = new ArrayList<>();
        try {
            for (String diskName : paths) {
                VolumeVO volumeVO = volumeDao.findByPath(diskName);
                if (volumeVO != null) {
                    vmVolumes.add(volumeVO);
                }
            }
            List<Backup.VolumeInfo> list = new ArrayList<>();
            for (VolumeVO vol : vmVolumes) {
                list.add(new Backup.VolumeInfo(vol.getUuid(), vol.getPath(), vol.getVolumeType(), vol.getSize(), vol.getDeviceId()));
            }
            return new Gson().toJson(list.toArray(), Backup.VolumeInfo[].class);
        } catch (Exception e) {
            if (CollectionUtils.isEmpty(vmVolumes) || vmVolumes.get(0).getInstanceId() == null) {
                LOG.error(String.format("Failed to create VolumeInfo of VM [id: null] volumes due to: [%s].", e.getMessage()), e);
            } else {
                LOG.error(String.format("Failed to create VolumeInfo of VM [id: %s] volumes due to: [%s].", vmVolumes.get(0).getInstanceId(), e.getMessage()), e);
            }
            throw e;
        }
    }

    @Override
    public String getConfigComponentName() {
        return BackupService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                VeeamUrl,
                VeeamUsername,
                VeeamPassword,
                VeeamValidateSSLSecurity,
                VeeamApiRequestTimeout,
                VeeamRestoreTimeout
        };
    }

    @Override
    public String getName() {
        return "veeam";
    }

    @Override
    public String getDescription() {
        return "Veeam Backup Plugin";
    }
}
