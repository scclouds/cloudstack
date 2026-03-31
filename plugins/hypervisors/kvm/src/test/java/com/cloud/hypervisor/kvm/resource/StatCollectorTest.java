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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVmStatsForMetricCollectionAnswer;
import com.cloud.agent.api.GetVmStatsForMetricCollectionCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.hypervisor.kvm.resource.wrapper.LibvirtRequestWrapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainBlockStats;
import org.libvirt.DomainInfo;
import org.libvirt.DomainInterfaceStats;
import org.libvirt.LibvirtException;
import org.libvirt.MemoryStatistic;
import org.libvirt.NodeInfo;
import org.libvirt.jna.virDomainMemoryStats;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StatCollectorTest {

    @InjectMocks
    @Spy
    private StatCollector statCollector = Mockito.spy(new StatCollector());

    @Spy
    private LibvirtComputingResource libvirtComputingResourceSpy = Mockito.spy(new LibvirtComputingResource());


    @Mock
    Connect connMock;
    @Mock
    Domain domainMock;
    @Mock
    DomainBlockStats domainBlockStatsMock;
    @Mock
    DomainInfo domainInfoMock;
    @Mock
    DomainInterfaceStats domainInterfaceStatsMock;
    @Mock
    Map<String, LibvirtExtendedVmStatsEntry> vmStatsMock = new HashMap<>();

    private final static String VM_NAME = "test";

    @Test
    public void testMemoryFreeInKBsDomainReturningOfSomeMemoryStatistics() throws LibvirtException {
        if (!System.getProperty("os.name").equals("Linux")) {
            return;
        }

        MemoryStatistic[] mem = createMemoryStatisticFreeMemory100();
        Domain domainMock = getDomainConfiguredToReturnMemoryStatistic(mem);
        long memoryFreeInKBs = statCollector.getMemoryFreeInKBs(domainMock);

        Assert.assertEquals(100, memoryFreeInKBs);
    }

    @Test
    public void testMemoryFreeInKBsDomainReturningNoMemoryStatistics() throws LibvirtException {

        Domain domainMock = getDomainConfiguredToReturnMemoryStatistic(null);
        long memoryFreeInKBs = statCollector.getMemoryFreeInKBs(domainMock);

        Assert.assertEquals(-1, memoryFreeInKBs);
    }

    @Test
    public void getMemoryFreeInKBsTestDomainReturningIncompleteArray() throws LibvirtException {
        if (!System.getProperty("os.name").equals("Linux")) {
            return;
        }

        MemoryStatistic[] mem = createMemoryStatisticFreeMemory100();
        mem[0].setTag(0);
        Domain domainMock = getDomainConfiguredToReturnMemoryStatistic(mem);
        long memoryFreeInKBs = statCollector.getMemoryFreeInKBs(domainMock);

        Assert.assertEquals(-1, memoryFreeInKBs);
    }

    private MemoryStatistic[] createMemoryStatisticFreeMemory100() {
        virDomainMemoryStats stat = new virDomainMemoryStats();
        stat.val = 100;
        stat.tag = 4;

        MemoryStatistic[] mem = new MemoryStatistic[1];
        mem[0] = new MemoryStatistic(stat);
        return mem;
    }

    private Domain getDomainConfiguredToReturnMemoryStatistic(MemoryStatistic[] mem) throws LibvirtException {
        Domain domainMock = Mockito.mock(Domain.class);
        when(domainMock.memoryStats(20)).thenReturn(mem);
        return domainMock;
    }

    @Test
    public void calculateVmMetricsTestOldStatsIsNotNullCalculatesUtilization() throws LibvirtException {
        prepareVmInfoForGetVmCurrentStats();
        LibvirtExtendedVmStatsEntry oldStats = statCollector.getVmCurrentStats(domainMock);
        Map<String, LibvirtExtendedVmStatsEntry> vmStats = new HashMap<>();
        vmStats.put(domainMock.getName(), oldStats);
        ReflectionTestUtils.setField(statCollector, "vmStats", vmStats);


        domainInfoMock.cpuTime *= 3;
        domainInterfaceStatsMock.rx_bytes *= 3;
        domainInterfaceStatsMock.tx_bytes *= 3;
        domainBlockStatsMock.rd_req *= 3;
        domainBlockStatsMock.rd_bytes *= 3;
        domainBlockStatsMock.wr_req *= 3;
        domainBlockStatsMock.wr_bytes *= 3;
        LibvirtExtendedVmStatsEntry newStats = statCollector.getVmCurrentStats(domainMock);

        VmStatsEntry metrics = statCollector.calculateVmMetrics(domainMock);

        Assert.assertEquals(domainInfoMock.nrVirtCpu, metrics.getNumCPUs());
        Assert.assertEquals(domainInfoMock.maxMem, (long) metrics.getMemoryKBs());
        Assert.assertEquals(statCollector.getMemoryFreeInKBs(domainMock), (long) metrics.getIntFreeMemoryKBs());
        Assert.assertEquals(domainInfoMock.memory, (long) metrics.getTargetMemoryKBs());
        Assert.assertTrue(metrics.getCPUUtilization() > 0);
        Assert.assertEquals(newStats.getNetworkReadKBs() - oldStats.getNetworkReadKBs(), metrics.getNetworkReadKBs(), 0);
        Assert.assertEquals(newStats.getNetworkWriteKBs() - oldStats.getNetworkWriteKBs(), metrics.getNetworkWriteKBs(), 0);
        Assert.assertEquals(newStats.getDiskReadIOs() - oldStats.getDiskReadIOs(), metrics.getDiskReadIOs(), 0);
        Assert.assertEquals(newStats.getDiskWriteIOs() - oldStats.getDiskWriteIOs(), metrics.getDiskWriteIOs(), 0);
        Assert.assertEquals(newStats.getDiskReadKBs() - oldStats.getDiskReadKBs(), metrics.getDiskReadKBs(), 0);
        Assert.assertEquals(newStats.getDiskWriteKBs() - oldStats.getDiskWriteKBs(), metrics.getDiskWriteKBs(), 0);
    }

    private void prepareVmInfoForGetVmCurrentStats() throws LibvirtException {
        final NodeInfo nodeInfo = new NodeInfo();
        nodeInfo.cpus = 8;
        nodeInfo.memory = 8 * 1024 * 1024;
        nodeInfo.sockets = 2;
        nodeInfo.threads = 2;
        nodeInfo.model = "Foo processor";

        doReturn(VM_NAME).when(domainMock).getName();
        doReturn(connMock).when(domainMock).getConnect();
        domainInfoMock.cpuTime = 500L;
        domainInfoMock.nrVirtCpu = 4;
        domainInfoMock.memory = 2048;
        domainInfoMock.maxMem = 4096;
        when(domainMock.getInfo()).thenReturn(domainInfoMock);
        final MemoryStatistic[] domainMem = new MemoryStatistic[2];
        domainMem[0] = Mockito.mock(MemoryStatistic.class);

        domainInterfaceStatsMock.rx_bytes = 1000L;
        domainInterfaceStatsMock.tx_bytes = 2000L;
        doReturn(domainInterfaceStatsMock).when(domainMock).interfaceStats(any());
        doReturn(List.of(new LibvirtVMDef.InterfaceDef())).when(libvirtComputingResourceSpy).getInterfaces(connMock, VM_NAME);

        domainBlockStatsMock.rd_req = 3000L;
        domainBlockStatsMock.rd_bytes = 4000L;
        domainBlockStatsMock.wr_req = 5000L;
        domainBlockStatsMock.wr_bytes = 6000L;
        doReturn(domainBlockStatsMock).when(domainMock).blockStats(any());
        doReturn(List.of(new LibvirtVMDef.DiskDef())).when(libvirtComputingResourceSpy).getDisks(connMock, VM_NAME);

        Map<String, Long> vmNames = new HashMap<>();
        vmNames.put(domainMock.getName(), 0L);
        ReflectionTestUtils.setField(statCollector, "vmNames", vmNames);


    }

    @Test
    public void getVmCurrentStatsTestIfStatsAreAsExpected() throws LibvirtException {
        prepareVmInfoForGetVmCurrentStats();

        LibvirtExtendedVmStatsEntry vmStatsEntry = statCollector.getVmCurrentStats(domainMock);

        Assert.assertEquals(domainInfoMock.cpuTime, vmStatsEntry.getCpuTime());
        Assert.assertEquals((double) domainInterfaceStatsMock.rx_bytes / 1024, vmStatsEntry.getNetworkReadKBs(), 0);
        Assert.assertEquals((double) domainInterfaceStatsMock.tx_bytes / 1024, vmStatsEntry.getNetworkWriteKBs(), 0);
        Assert.assertEquals(domainBlockStatsMock.rd_req, vmStatsEntry.getDiskReadIOs(), 0);
        Assert.assertEquals((double) domainBlockStatsMock.rd_bytes / 1024, vmStatsEntry.getDiskReadKBs(), 0);
        Assert.assertEquals(domainBlockStatsMock.wr_req, vmStatsEntry.getDiskWriteIOs(), 0);
        Assert.assertEquals((double) domainBlockStatsMock.wr_bytes / 1024, vmStatsEntry.getDiskWriteKBs(), 0);
        Assert.assertNotNull(vmStatsEntry.getTimestamp());
    }

    @Test
    public void getVmCurrentCpuStatsTestIfStatsAreAsExpected() throws LibvirtException {
        prepareVmInfoForGetVmCurrentStats();

        LibvirtExtendedVmStatsEntry vmStatsEntry = new LibvirtExtendedVmStatsEntry();
        statCollector.getVmCurrentCpuStats(domainMock, vmStatsEntry);

        Assert.assertEquals(domainInfoMock.cpuTime, vmStatsEntry.getCpuTime());
    }

    @Test
    public void getVmCurrentNetworkStatsTestIfStatsAreAsExpected() throws LibvirtException {
        prepareVmInfoForGetVmCurrentStats();
        doReturn(VM_NAME).when(statCollector).vmToString(Mockito.any());

        LibvirtExtendedVmStatsEntry vmStatsEntry = new LibvirtExtendedVmStatsEntry();
        statCollector.getVmCurrentNetworkStats(domainMock, vmStatsEntry);

        Assert.assertEquals((double) domainInterfaceStatsMock.rx_bytes / 1024, vmStatsEntry.getNetworkReadKBs(), 0);
        Assert.assertEquals((double) domainInterfaceStatsMock.tx_bytes / 1024, vmStatsEntry.getNetworkWriteKBs(), 0);
    }

    @Test
    public void getVmCurrentDiskStatsTestIfStatsAreAsExpected() throws LibvirtException {
        prepareVmInfoForGetVmCurrentStats();

        LibvirtExtendedVmStatsEntry vmStatsEntry = new LibvirtExtendedVmStatsEntry();
        statCollector.getVmCurrentDiskStats(domainMock, vmStatsEntry);

        Assert.assertEquals(domainBlockStatsMock.rd_req, vmStatsEntry.getDiskReadIOs(), 0);
        Assert.assertEquals((double) domainBlockStatsMock.rd_bytes / 1024, vmStatsEntry.getDiskReadKBs(), 0);
        Assert.assertEquals(domainBlockStatsMock.wr_req, vmStatsEntry.getDiskWriteIOs(), 0);
        Assert.assertEquals((double) domainBlockStatsMock.wr_bytes / 1024, vmStatsEntry.getDiskWriteKBs(), 0);
    }

    @Test
    public void calculateVmMetricsTestOldStatsIsNullDoesNotCalculateUtilization() throws LibvirtException {
        prepareVmInfoForGetVmCurrentStats();

        VmStatsEntry metrics = statCollector.calculateVmMetrics(domainMock);

        Assert.assertEquals(domainInfoMock.nrVirtCpu, metrics.getNumCPUs());
        Assert.assertEquals(domainInfoMock.maxMem, (long) metrics.getMemoryKBs());
        Assert.assertEquals(statCollector.getMemoryFreeInKBs(domainMock), (long) metrics.getIntFreeMemoryKBs());
        Assert.assertEquals(domainInfoMock.memory, (long) metrics.getTargetMemoryKBs());
        Assert.assertEquals(0, metrics.getCPUUtilization(), 0);
        Assert.assertEquals(0, metrics.getNetworkReadKBs(), 0);
        Assert.assertEquals(0, metrics.getNetworkWriteKBs(), 0);
        Assert.assertEquals(0, metrics.getDiskReadKBs(), 0);
        Assert.assertEquals(0, metrics.getDiskReadIOs(), 0);
        Assert.assertEquals(0, metrics.getDiskWriteKBs(), 0);
        Assert.assertEquals(0, metrics.getDiskWriteIOs(), 0);
    }

    @Test
    public void testGetVmStatsCommand() {
        VmStatsEntry metrics = new VmStatsEntry();

        final Map<String, Long> vms = new HashMap<>();
        for (long i = 0; i < 5; i++) {
            vms.put("VM" + i, i);
            statCollector.addMetrics(metrics);
        }
        doReturn(statCollector).when(libvirtComputingResourceSpy).getStatCollector();

        final GetVmStatsForMetricCollectionCommand command = new GetVmStatsForMetricCollectionCommand(vms, "e8d6b4d0-bc6d-4613-b8bb-cb9e0600f3c0", "hostname");

        final LibvirtRequestWrapper wrapper = LibvirtRequestWrapper.getInstance();
        Assert.assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, libvirtComputingResourceSpy);
        Assert.assertTrue(answer.getResult());
        Assert.assertEquals(((GetVmStatsForMetricCollectionAnswer)answer).getVmStats().size(), vms.size());
    }
}
