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

package com.cloud.hypervisor.kvm.resource;

import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import com.cloud.hypervisor.Hypervisor;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockStats;
import org.libvirt.DomainInfo;
import org.libvirt.DomainInterfaceStats;
import org.libvirt.LibvirtException;
import org.libvirt.MemoryStatistic;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StatCollector implements Runnable{
    protected Logger logger = LogManager.getLogger(getClass());

    protected long vmStatsInterval = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.VM_STATS_INTERVAL);

    protected LibvirtComputingResource libvirtComputingResource = new LibvirtComputingResource();

    /**
     * Map of the stats of each VM, will be used during metric calculation.
     */
    private final Map<String, LibvirtExtendedVmStatsEntry> vmStats = new ConcurrentHashMap<>();

    /**
     * List of VM metrics records. This list is cleared every time a Management Server processes the records. If the connection with the Management Server is lost, the records will
     * keep being stored until the limit defined by the {@link #MAXSTOREDSTATS} variable is reached for each VM.
     * */
    private final List<VmStatsEntry> vmMetrics = new ArrayList<>();

    /**
     * list of names, provided by the Management Server, of the VMs that will have their metrics collected.
     * */
    private Map<String, Long> vmNames = new HashMap<>();

    /**
     * Maximum amount of metrics records that can be stored in memory for each VM. When this number is exceeded, the agent will no longer collect metrics until a Management Server
     * processes these metrics and clears the list.
     * */
    private static final int MAXSTOREDSTATS = 1000;

    /**
     * Since the memoryStats method returns an array that isn't ordered, we pass a big number to get all the array and then search for the information we want.
     * */
    private static final int NUMMEMSTATS = 20;

    /**
     * Unused memory's tag to search in the array returned by the Domain.memoryStats() method.
     * */
    private static final int UNUSEDMEMORY = 4;

    protected void addMetrics(VmStatsEntry metrics ) {
        if (metrics == null) {
            return;
        }
        synchronized (vmMetrics) {
            vmMetrics.add(metrics);
        }
    }

    public List<VmStatsEntry> getVmMetrics() {
        synchronized (vmMetrics) {
            List<VmStatsEntry> metrics = new ArrayList<>(vmMetrics);
            vmMetrics.clear();
            return metrics;
        }
    }

    public void updateVmNames(Map<String, Long> names) {
        vmNames =  names;
    }


    /**
     * Returns metrics for the period since this function was last called for the specified VM.
     * @param domain the VM Domain.
     * @return metrics for the period since last time this function was called for the VM.
     * @throws LibvirtException
     */
    protected VmStatsEntry calculateVmMetrics(Domain domain)  throws LibvirtException {
        final VmStatsEntry metrics = new VmStatsEntry();
        final DomainInfo info = domain.getInfo();
        final String vmAsString = vmToString(domain);

        LibvirtExtendedVmStatsEntry newStats = getVmCurrentStats(domain);
        LibvirtExtendedVmStatsEntry oldStats = vmStats.get(domain.getName());
        vmStats.put(domain.getName(), newStats);

        metrics.setEntityType("vm");
        logger.trace("Writing VM [{}]'s CPU and memory information into the metrics.", vmAsString);
        metrics.setNumCPUs(info.nrVirtCpu);
        metrics.setMemoryKBs(info.maxMem);
        metrics.setTargetMemoryKBs(info.memory);
        logger.trace("Trying to get free memory for VM [{}].", vmAsString);
        metrics.setIntFreeMemoryKBs(getMemoryFreeInKBs(domain));
        if (oldStats != null) {
            long elapsedTime = Calendar.getInstance().getTimeInMillis() - oldStats.getTimestamp().getTimeInMillis();

            logger.debug("Old stats exist for VM [{}]; therefore, the utilization will be calculated.", vmAsString);
            logger.trace("Calculating CPU utilization for VM [{}].", vmAsString);
            double utilization = (info.cpuTime - oldStats.getCpuTime()) / ((double) elapsedTime * 1000000 * info.nrVirtCpu);
            if (utilization > 0) {
                metrics.setCPUUtilization(utilization * 100);
            }

            logger.trace("Calculating network utilization for VM [{}].", vmAsString);
            final double deltarx = newStats.getNetworkReadKBs() - oldStats.getNetworkReadKBs();
            if (deltarx > 0) {
                metrics.setNetworkReadKBs(deltarx);
            }
            final double deltatx = newStats.getNetworkWriteKBs() - oldStats.getNetworkWriteKBs();
            if (deltatx > 0) {
                metrics.setNetworkWriteKBs(deltatx);
            }

            logger.trace("Calculating disk utilization for VM [{}].", vmAsString);
            final double deltaiord = newStats.getDiskReadIOs() - oldStats.getDiskReadIOs();
            if (deltaiord > 0) {
                metrics.setDiskReadIOs(deltaiord);
            }
            final double deltaiowr = newStats.getDiskWriteIOs() - oldStats.getDiskWriteIOs();
            if (deltaiowr > 0) {
                metrics.setDiskWriteIOs(deltaiowr);
            }
            final double deltabytesrd = newStats.getDiskReadKBs() - oldStats.getDiskReadKBs();
            if (deltabytesrd > 0) {
                metrics.setDiskReadKBs(deltabytesrd);
            }
            final double deltabyteswr = newStats.getDiskWriteKBs() - oldStats.getDiskWriteKBs();
            if (deltabyteswr > 0) {
                metrics.setDiskWriteKBs(deltabyteswr);
            }
        }

        metrics.setVmId(vmNames.get(domain.getName()));
        metrics.setTimestamp(newStats.getTimestamp());

        String metricsAsString= new ReflectionToStringBuilder(metrics, ToStringStyle.JSON_STYLE).setExcludeFieldNames("vmId", "vmUuid").toString();
        logger.debug("Calculated metrics for VM [{}]: [{}].", vmAsString, metricsAsString);

        return metrics;
    }

    protected LibvirtExtendedVmStatsEntry getVmCurrentStats(final Domain dm) throws LibvirtException {
        final LibvirtExtendedVmStatsEntry stats = new LibvirtExtendedVmStatsEntry();

        getVmCurrentCpuStats(dm, stats);
        getVmCurrentNetworkStats(dm, stats);
        getVmCurrentDiskStats(dm, stats);

        logger.debug("Retrieved statistics for VM [{}]: [{}].", vmToString(dm), stats);
        stats.setTimestamp(Calendar.getInstance());
        return stats;
    }

    /**
     * Passes a VM's current CPU statistics into the provided LibvirtExtendedVmStatsEntry.
     * @param dm domain of the VM.
     * @param stats LibvirtExtendedVmStatsEntry that will receive the current CPU statistics.
     * @throws LibvirtException
     */
    protected void getVmCurrentCpuStats(final Domain dm, final LibvirtExtendedVmStatsEntry stats) throws LibvirtException {
        logger.trace("Getting CPU stats for VM [{}].", vmToString(dm));
        stats.setCpuTime(dm.getInfo().cpuTime);
    }

    /**
     * Passes a VM's current network statistics into the provided LibvirtExtendedVmStatsEntry.
     * @param dm domain of the VM.
     * @param stats LibvirtExtendedVmStatsEntry that will receive the current network statistics.
     * @throws LibvirtException
     */
    protected void getVmCurrentNetworkStats(final Domain dm, final LibvirtExtendedVmStatsEntry stats) throws LibvirtException {
        final String vmAsString = vmToString(dm);
        logger.trace("Getting network stats for VM [{}].", vmAsString);
        final List<LibvirtVMDef.InterfaceDef> vifs = libvirtComputingResource.getInterfaces(dm.getConnect(), dm.getName());
        logger.debug("Found [{}] network interface(s) for VM [{}].", vifs.size(), vmAsString);
        double rx = 0;
        double tx = 0;
        for (final LibvirtVMDef.InterfaceDef vif : vifs) {
            final DomainInterfaceStats ifStats = dm.interfaceStats(vif.getDevName());
            rx += ifStats.rx_bytes;
            tx += ifStats.tx_bytes;
        }
        stats.setNetworkReadKBs(rx / 1024);
        stats.setNetworkWriteKBs(tx / 1024);
    }

    /**
     * Passes a VM's current disk statistics into the provided LibvirtExtendedVmStatsEntry.
     * @param dm domain of the VM.
     * @param stats LibvirtExtendedVmStatsEntry that will receive the current disk statistics.
     * @throws LibvirtException
     */
    protected void getVmCurrentDiskStats(final Domain dm, final LibvirtExtendedVmStatsEntry stats) throws LibvirtException {
        final String vmAsString = vmToString(dm);
        logger.trace("Getting disk stats for VM [{}].", vmAsString);
        final List<LibvirtVMDef.DiskDef> disks = libvirtComputingResource.getDisks(dm.getConnect(), dm.getName());
        logger.debug("Found [{}] disk(s) for VM [{}].", disks.size(), vmAsString);
        long io_rd = 0;
        long io_wr = 0;
        double bytes_rd = 0;
        double bytes_wr = 0;
        for (final LibvirtVMDef.DiskDef disk : disks) {
            if (disk.getDeviceType() == LibvirtVMDef.DiskDef.DeviceType.CDROM || disk.getDeviceType() == LibvirtVMDef.DiskDef.DeviceType.FLOPPY) {
                logger.debug("Ignoring disk [{}] in VM [{}]'s stats since its deviceType is [{}].", disk.toString().replace("\n", ""), vmAsString, disk.getDeviceType());
                continue;
            }
            final DomainBlockStats blockStats = dm.blockStats(disk.getDiskLabel());
            io_rd += blockStats.rd_req;
            io_wr += blockStats.wr_req;
            bytes_rd += blockStats.rd_bytes;
            bytes_wr += blockStats.wr_bytes;
        }
        stats.setDiskReadIOs(io_rd);
        stats.setDiskWriteIOs(io_wr);
        stats.setDiskReadKBs(bytes_rd / 1024);
        stats.setDiskWriteKBs(bytes_wr / 1024);
    }

    /**
     * This method retrieves the memory statistics from the domain given as parameters.
     * If no memory statistic is found, it will return {@link NumberUtils#LONG_MINUS_ONE} as the value of free memory in the domain.
     * If it can retrieve the domain memory statistics, it will return the free memory statistic; that means, it returns the value at the first position of the array returned by {@link Domain#memoryStats(int)}.
     *
     * @return the amount of free memory in KBs
     */
    protected long getMemoryFreeInKBs(Domain dm) throws LibvirtException {
        MemoryStatistic[] memoryStats = dm.memoryStats(NUMMEMSTATS);
        logger.trace("Retrieved memory statistics (information about tags can be found on the libvirt documentation): {}.",
                () -> Stream.of(memoryStats).map(stat -> stat.toString().trim().replace("\n", ",")).collect(Collectors.joining("},{", "[{", "}]")));

        long freeMemory = NumberUtils.LONG_MINUS_ONE;

        if (ArrayUtils.isEmpty(memoryStats)){
            return freeMemory;
        }

        for (int i = 0; i < memoryStats.length; i++) {
            if(memoryStats[i].getTag() == UNUSEDMEMORY) {
                freeMemory = memoryStats[i].getValue();
                break;
            }
        }

        if (freeMemory == NumberUtils.LONG_MINUS_ONE){
            logger.warn("Couldn't retrieve free memory, returning -1.");
        }
        return freeMemory;
    }

    protected String vmToString(Domain dm) throws LibvirtException {
        return String.format("{\"name\":\"%s\",\"uuid\":\"%s\"}", dm.getName(), dm.getUUIDString());
    }

    protected void calculateVmStats() {
        try {
            Connect conn = LibvirtConnection.getConnectionByType(Hypervisor.HypervisorType.KVM.toString());
            for (String vmName : vmNames.keySet()) {
                try {
                    Domain domain = libvirtComputingResource.getDomain(conn, vmName);
                    VmStatsEntry stats = calculateVmMetrics(domain);
                    addMetrics(stats);
                } catch (LibvirtException e) {
                    logger.warn("Could not get metrics for VM [{}].", vmName, e);
                }
            }
            conn.close();
        } catch (final LibvirtException e) {
            logger.warn("Failed to list domains.", e);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(vmStatsInterval);
            } catch (InterruptedException e) {
                logger.debug("[ignored] Interrupted between metric collections.", e);
            }
            if (vmNames.isEmpty()) {
                logger.debug("VM name map for metric collection is empty, skipping.");
                continue;
            }
            if (vmMetrics.size() >= MAXSTOREDSTATS * vmNames.size()) {
                logger.warn("Maximum number of stored metric records [{}] for each VM reached. Skipping metric calculation.", MAXSTOREDSTATS);
                continue;
            }
            calculateVmStats();
        }
    }
}
