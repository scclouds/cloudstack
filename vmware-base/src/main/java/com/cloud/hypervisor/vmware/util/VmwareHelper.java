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

package com.cloud.hypervisor.vmware.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import com.cloud.agent.api.to.DiskTO;
import com.cloud.storage.DiskControllerMapping;
import com.cloud.storage.DiskControllerMappingVO;
import com.cloud.storage.Volume;
import com.cloud.utils.exception.CloudRuntimeException;
import com.vmware.vim25.ParaVirtualSCSIController;
import com.vmware.vim25.VirtualController;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualIDEController;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualSCSIController;
import com.vmware.vim25.VirtualSCSISharing;
import org.apache.cloudstack.storage.command.CopyCommand;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.mo.CustomFieldConstants;
import com.cloud.hypervisor.vmware.mo.DatastoreMO;
import com.cloud.hypervisor.vmware.mo.DiskControllerType;
import com.cloud.hypervisor.vmware.mo.HostMO;
import com.cloud.hypervisor.vmware.mo.LicenseAssignmentManagerMO;
import com.cloud.hypervisor.vmware.mo.VirtualEthernetCardType;
import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.utils.LogUtils;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.ExceptionUtil;
import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.GuestOsDescriptor;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.MethodFault;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.OptionValue;
import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.ResourceAllocationInfo;
import com.vmware.vim25.StorageIOAllocationInfo;
import com.vmware.vim25.VirtualCdrom;
import com.vmware.vim25.VirtualCdromIsoBackingInfo;
import com.vmware.vim25.VirtualCdromRemotePassthroughBackingInfo;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualDeviceConnectInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer1BackingInfo;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualDiskMode;
import com.vmware.vim25.VirtualDiskRawDiskMappingVer1BackingInfo;
import com.vmware.vim25.VirtualDiskSparseVer1BackingInfo;
import com.vmware.vim25.VirtualDiskSparseVer2BackingInfo;
import com.vmware.vim25.VirtualE1000;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualEthernetCardNetworkBackingInfo;
import com.vmware.vim25.VirtualEthernetCardOpaqueNetworkBackingInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineSnapshotTree;
import com.vmware.vim25.VirtualPCNet32;
import com.vmware.vim25.VirtualUSBController;
import com.vmware.vim25.VirtualVmxnet2;
import com.vmware.vim25.VirtualVmxnet3;

public class VmwareHelper {
    private static final Logger s_logger = Logger.getLogger(VmwareHelper.class);

    public static final int MAX_SCSI_CONTROLLER_COUNT = 4; //
    public static final int MAX_ALLOWED_DEVICES_SCSI_CONTROLLER = 16; //
    public static final int MAX_SUPPORTED_DEVICES_SCSI_CONTROLLER = MAX_ALLOWED_DEVICES_SCSI_CONTROLLER - 1; // One device node is unavailable for hard disks or SCSI devices
    public static final String MIN_VERSION_UEFI_LEGACY = "5.5";

    private static List<DiskControllerMappingVO> diskControllerMappings;

    public static void setDiskControllerMappings(List<DiskControllerMappingVO> mappings) {
        diskControllerMappings = mappings;
    }

    public static boolean isReservedScsiDeviceNumber(int deviceNumber) {
        // The SCSI controller is assigned to virtual device node (z:7), so that device node is unavailable for hard disks or SCSI devices.
        return (deviceNumber % VmwareHelper.MAX_ALLOWED_DEVICES_SCSI_CONTROLLER) == 7;
    }

    @Nonnull
    private static VirtualDeviceConnectInfo getVirtualDeviceConnectInfo(boolean connected, boolean connectOnStart) {
        VirtualDeviceConnectInfo connectInfo = new VirtualDeviceConnectInfo();
        connectInfo.setAllowGuestControl(true);
        connectInfo.setConnected(connected);
        connectInfo.setStartConnected(connectOnStart);
        return connectInfo;
    }

    @Nonnull
    private static VirtualEthernetCard createVirtualEthernetCard(VirtualEthernetCardType deviceType) {
        VirtualEthernetCard nic;
        switch (deviceType) {
            case E1000:
                nic = new VirtualE1000();
                break;

            case PCNet32:
                nic = new VirtualPCNet32();
                break;

            case Vmxnet2:
                nic = new VirtualVmxnet2();
                break;

            case Vmxnet3:
                nic = new VirtualVmxnet3();
                break;

            default:
                assert (false);
                nic = new VirtualE1000();
        }
        return nic;
    }

    public static VirtualDevice prepareNicOpaque(VirtualMachineMO vmMo, VirtualEthernetCardType deviceType, String portGroupName,
            String macAddress, int contextNumber, boolean connected, boolean connectOnStart) throws Exception {
        assert(vmMo.getRunningHost().hasOpaqueNSXNetwork());

        VirtualEthernetCard nic = createVirtualEthernetCard(deviceType);

        VirtualEthernetCardOpaqueNetworkBackingInfo nicBacking = new VirtualEthernetCardOpaqueNetworkBackingInfo();
        nicBacking.setOpaqueNetworkId("br-int");
        nicBacking.setOpaqueNetworkType("nsx.network");

        nic.setBacking(nicBacking);

        nic.setAddressType("Manual");
        nic.setConnectable(getVirtualDeviceConnectInfo(connected, connectOnStart));
        nic.setMacAddress(macAddress);
        nic.setKey(-contextNumber);
        return nic;
    }

    public static void updateNicDevice(VirtualDevice nic, ManagedObjectReference morNetwork, String portGroupName) throws Exception {
        VirtualEthernetCardNetworkBackingInfo nicBacking = new VirtualEthernetCardNetworkBackingInfo();
        nicBacking.setDeviceName(portGroupName);
        nicBacking.setNetwork(morNetwork);
        nic.setBacking(nicBacking);
    }

    public static void updateDvNicDevice(VirtualDevice nic, ManagedObjectReference morNetwork, String dvSwitchUuid) throws Exception {
        final VirtualEthernetCardDistributedVirtualPortBackingInfo dvPortBacking = new VirtualEthernetCardDistributedVirtualPortBackingInfo();
        final DistributedVirtualSwitchPortConnection dvPortConnection = new DistributedVirtualSwitchPortConnection();

        dvPortConnection.setSwitchUuid(dvSwitchUuid);
        dvPortConnection.setPortgroupKey(morNetwork.getValue());
        dvPortBacking.setPort(dvPortConnection);
        nic.setBacking(dvPortBacking);
    }

    public static VirtualDevice prepareNicDevice(VirtualMachineMO vmMo, ManagedObjectReference morNetwork, VirtualEthernetCardType deviceType, String portGroupName,
            String macAddress, int contextNumber, boolean connected, boolean connectOnStart) throws Exception {

        VirtualEthernetCard nic = createVirtualEthernetCard(deviceType);

        VirtualEthernetCardNetworkBackingInfo nicBacking = new VirtualEthernetCardNetworkBackingInfo();
        nicBacking.setDeviceName(portGroupName);
        nicBacking.setNetwork(morNetwork);
        nic.setBacking(nicBacking);

        nic.setAddressType("Manual");
        nic.setConnectable(getVirtualDeviceConnectInfo(connected, connectOnStart));
        nic.setMacAddress(macAddress);
        nic.setKey(-contextNumber);
        return nic;
    }

    public static VirtualDevice prepareDvNicDevice(VirtualMachineMO vmMo, ManagedObjectReference morNetwork, VirtualEthernetCardType deviceType, String dvPortGroupName,
            String dvSwitchUuid, String macAddress, int contextNumber, boolean connected, boolean connectOnStart) throws Exception {

        VirtualEthernetCard nic = createVirtualEthernetCard(deviceType);

        final VirtualEthernetCardDistributedVirtualPortBackingInfo dvPortBacking = new VirtualEthernetCardDistributedVirtualPortBackingInfo();
        final DistributedVirtualSwitchPortConnection dvPortConnection = new DistributedVirtualSwitchPortConnection();

        dvPortConnection.setSwitchUuid(dvSwitchUuid);
        dvPortConnection.setPortgroupKey(morNetwork.getValue());
        dvPortBacking.setPort(dvPortConnection);
        nic.setBacking(dvPortBacking);

        nic.setAddressType("Manual");
        nic.setConnectable(getVirtualDeviceConnectInfo(connected, connectOnStart));
        nic.setMacAddress(macAddress);
        nic.setKey(-contextNumber);
        return nic;
    }

    // vmdkDatastorePath: [datastore name] vmdkFilePath
    public static VirtualDevice prepareDiskDevice(VirtualMachineMO vmMo, VirtualDisk device, int controllerKey, String vmdkDatastorePathChain[],
                                                  ManagedObjectReference morDs, int deviceNumber, int contextNumber, Long maxIops) throws Exception {
        s_logger.debug(LogUtils.logGsonWithoutException("Trying to prepare disk device to virtual machine [%s], using the following details: Virtual device [%s], "
                + "ManagedObjectReference [%s], ControllerKey [%s], VMDK path chain [%s], DeviceNumber [%s], ContextNumber [%s] and max IOPS [%s].",
                vmMo, device, morDs, controllerKey, vmdkDatastorePathChain, deviceNumber, contextNumber, maxIops));
        assert (vmdkDatastorePathChain != null);
        assert (vmdkDatastorePathChain.length >= 1);

        VirtualDisk disk;
        VirtualDiskFlatVer2BackingInfo backingInfo;
        if (device != null) {
            disk = device;
            backingInfo = (VirtualDiskFlatVer2BackingInfo)disk.getBacking();
        } else {
            disk = new VirtualDisk();
            backingInfo = new VirtualDiskFlatVer2BackingInfo();
            backingInfo.setDatastore(morDs);
            backingInfo.setDiskMode(VirtualDiskMode.PERSISTENT.value());
            disk.setBacking(backingInfo);

            // TODO: isso tem q adaptar? talvez tirar e ja mando pra ca aas keys
            int ideControllerKey = vmMo.getIDEDeviceControllerKey();
            if (controllerKey < 0)
                controllerKey = ideControllerKey;
            if (deviceNumber < 0) {
                deviceNumber = vmMo.getNextDeviceNumber(controllerKey);
            }

            disk.setControllerKey(controllerKey);
            disk.setKey(-contextNumber);
            disk.setUnitNumber(deviceNumber);

            if (maxIops != null && maxIops > 0) {
                s_logger.debug(LogUtils.logGsonWithoutException("Defining [%s] as the max IOPS of disk [%s].", maxIops, disk));
                StorageIOAllocationInfo storageIOAllocationInfo = new StorageIOAllocationInfo();
                storageIOAllocationInfo.setLimit(maxIops);
                disk.setStorageIOAllocation(storageIOAllocationInfo);
            }

            VirtualDeviceConnectInfo connectInfo = new VirtualDeviceConnectInfo();
            connectInfo.setConnected(true);
            connectInfo.setStartConnected(true);
            disk.setConnectable(connectInfo);
        }

        backingInfo.setFileName(vmdkDatastorePathChain[0]);
        if (vmdkDatastorePathChain.length > 1) {
            String[] parentDisks = Arrays.copyOfRange(vmdkDatastorePathChain, 1, vmdkDatastorePathChain.length);
            setParentBackingInfo(backingInfo, morDs, parentDisks);
        }

        s_logger.debug(LogUtils.logGsonWithoutException("Prepared disk device, to attach to virtual machine [%s], has the following details: Virtual device [%s], "
                + "ManagedObjectReference [%s], ControllerKey [%s], VMDK path chain [%s], DeviceNumber [%s], ContextNumber [%s] and max IOPS [%s], is: [%s].",
                vmMo, device, morDs, controllerKey, vmdkDatastorePathChain, deviceNumber, contextNumber, maxIops, disk));
        return disk;
    }

    // vmdkDatastorePath: [datastore name] vmdkFilePath, create delta disk based on disk from template
    public static VirtualDevice prepareDiskDevice(VirtualMachineMO vmMo, int controllerKey, String vmdkDatastorePath, int sizeInMb, ManagedObjectReference morDs,
            VirtualDisk templateDisk, int deviceNumber, int contextNumber) throws Exception {

        assert (templateDisk != null);
        VirtualDeviceBackingInfo parentBacking = templateDisk.getBacking();
        assert (parentBacking != null);

        // TODO Not sure if we need to check if the disk in template and the new disk needs to share the
        // same datastore
        VirtualDisk disk = new VirtualDisk();
        if (parentBacking instanceof VirtualDiskFlatVer1BackingInfo) {
            VirtualDiskFlatVer1BackingInfo backingInfo = new VirtualDiskFlatVer1BackingInfo();
            backingInfo.setDiskMode(((VirtualDiskFlatVer1BackingInfo)parentBacking).getDiskMode());
            backingInfo.setDatastore(morDs);
            backingInfo.setFileName(vmdkDatastorePath);
            backingInfo.setParent((VirtualDiskFlatVer1BackingInfo)parentBacking);
            disk.setBacking(backingInfo);
        } else if (parentBacking instanceof VirtualDiskFlatVer2BackingInfo) {
            VirtualDiskFlatVer2BackingInfo backingInfo = new VirtualDiskFlatVer2BackingInfo();
            backingInfo.setDiskMode(((VirtualDiskFlatVer2BackingInfo)parentBacking).getDiskMode());
            backingInfo.setDatastore(morDs);
            backingInfo.setFileName(vmdkDatastorePath);
            backingInfo.setParent((VirtualDiskFlatVer2BackingInfo)parentBacking);
            disk.setBacking(backingInfo);
        } else if (parentBacking instanceof VirtualDiskRawDiskMappingVer1BackingInfo) {
            VirtualDiskRawDiskMappingVer1BackingInfo backingInfo = new VirtualDiskRawDiskMappingVer1BackingInfo();
            backingInfo.setDiskMode(((VirtualDiskRawDiskMappingVer1BackingInfo)parentBacking).getDiskMode());
            backingInfo.setDatastore(morDs);
            backingInfo.setFileName(vmdkDatastorePath);
            backingInfo.setParent((VirtualDiskRawDiskMappingVer1BackingInfo)parentBacking);
            disk.setBacking(backingInfo);
        } else if (parentBacking instanceof VirtualDiskSparseVer1BackingInfo) {
            VirtualDiskSparseVer1BackingInfo backingInfo = new VirtualDiskSparseVer1BackingInfo();
            backingInfo.setDiskMode(((VirtualDiskSparseVer1BackingInfo)parentBacking).getDiskMode());
            backingInfo.setDatastore(morDs);
            backingInfo.setFileName(vmdkDatastorePath);
            backingInfo.setParent((VirtualDiskSparseVer1BackingInfo)parentBacking);
            disk.setBacking(backingInfo);
        } else if (parentBacking instanceof VirtualDiskSparseVer2BackingInfo) {
            VirtualDiskSparseVer2BackingInfo backingInfo = new VirtualDiskSparseVer2BackingInfo();
            backingInfo.setDiskMode(((VirtualDiskSparseVer2BackingInfo)parentBacking).getDiskMode());
            backingInfo.setDatastore(morDs);
            backingInfo.setFileName(vmdkDatastorePath);
            backingInfo.setParent((VirtualDiskSparseVer2BackingInfo)parentBacking);
            disk.setBacking(backingInfo);
        } else {
            throw new Exception("Unsupported disk backing: " + parentBacking.getClass().getCanonicalName());
        }

        int ideControllerKey = vmMo.getIDEDeviceControllerKey();
        if (controllerKey < 0)
            controllerKey = ideControllerKey;
        disk.setControllerKey(controllerKey);
        if (deviceNumber < 0) {
            deviceNumber = vmMo.getNextDeviceNumber(controllerKey);
        }

        disk.setKey(-contextNumber);
        disk.setUnitNumber(deviceNumber);
        disk.setCapacityInKB(sizeInMb * 1024);

        VirtualDeviceConnectInfo connectInfo = new VirtualDeviceConnectInfo();
        connectInfo.setConnected(true);
        connectInfo.setStartConnected(true);
        disk.setConnectable(connectInfo);
        return disk;
    }

    private static void setParentBackingInfo(VirtualDiskFlatVer2BackingInfo backingInfo, ManagedObjectReference morDs, String[] parentDatastorePathList) {

        VirtualDiskFlatVer2BackingInfo parentBacking = new VirtualDiskFlatVer2BackingInfo();
        parentBacking.setDatastore(morDs);
        parentBacking.setDiskMode(VirtualDiskMode.PERSISTENT.value());

        if (parentDatastorePathList.length > 1) {
            String[] nextDatastorePathList = new String[parentDatastorePathList.length - 1];
            for (int i = 0; i < parentDatastorePathList.length - 1; i++)
                nextDatastorePathList[i] = parentDatastorePathList[i + 1];
            setParentBackingInfo(parentBacking, morDs, nextDatastorePathList);
        }
        parentBacking.setFileName(parentDatastorePathList[0]);

        backingInfo.setParent(parentBacking);
    }

    @SuppressWarnings("unchecked")
    private static void setParentBackingInfo(VirtualDiskFlatVer2BackingInfo backingInfo, Pair<String, ManagedObjectReference>[] parentDatastorePathList) {

        VirtualDiskFlatVer2BackingInfo parentBacking = new VirtualDiskFlatVer2BackingInfo();
        parentBacking.setDatastore(parentDatastorePathList[0].second());
        parentBacking.setDiskMode(VirtualDiskMode.PERSISTENT.value());

        if (parentDatastorePathList.length > 1) {
            Pair<String, ManagedObjectReference>[] nextDatastorePathList = new Pair[parentDatastorePathList.length - 1];
            for (int i = 0; i < parentDatastorePathList.length - 1; i++)
                nextDatastorePathList[i] = parentDatastorePathList[i + 1];
            setParentBackingInfo(parentBacking, nextDatastorePathList);
        }
        parentBacking.setFileName(parentDatastorePathList[0].first());

        backingInfo.setParent(parentBacking);
    }

    public static Pair<VirtualDevice, Boolean> prepareIsoDevice(VirtualMachineMO vmMo, String isoDatastorePath, ManagedObjectReference morDs, boolean connect,
            boolean connectAtBoot, int deviceNumber, int contextNumber) throws Exception {

        boolean newCdRom = false;
        VirtualCdrom cdRom = (VirtualCdrom)vmMo.getIsoDevice();
        if (cdRom == null) {
            newCdRom = true;
            cdRom = new VirtualCdrom();

            assert (vmMo.getIDEDeviceControllerKey() >= 0);
            cdRom.setControllerKey(vmMo.getIDEDeviceControllerKey());
            if (deviceNumber < 0)
                deviceNumber = vmMo.getNextIDEDeviceNumber();

            cdRom.setUnitNumber(deviceNumber);
            cdRom.setKey(-contextNumber);
        }

        VirtualDeviceConnectInfo cInfo = new VirtualDeviceConnectInfo();
        cInfo.setConnected(connect);
        cInfo.setStartConnected(connectAtBoot);
        cdRom.setConnectable(cInfo);

        if (isoDatastorePath != null) {
            VirtualCdromIsoBackingInfo backingInfo = new VirtualCdromIsoBackingInfo();
            backingInfo.setFileName(isoDatastorePath);
            backingInfo.setDatastore(morDs);
            cdRom.setBacking(backingInfo);
        } else {
            VirtualCdromRemotePassthroughBackingInfo backingInfo = new VirtualCdromRemotePassthroughBackingInfo();
            backingInfo.setDeviceName("");
            cdRom.setBacking(backingInfo);
        }

        return new Pair<VirtualDevice, Boolean>(cdRom, newCdRom);
    }

    public static VirtualDisk getRootDisk(VirtualDisk[] disks) {
        if (disks.length == 1)
            return disks[0];

        // TODO : for now, always return the first disk as root disk
        return disks[0];
    }

    public static ManagedObjectReference findSnapshotInTree(List<VirtualMachineSnapshotTree> snapTree, String findName) {
        assert (findName != null);

        ManagedObjectReference snapMor = null;
        if (snapTree == null)
            return snapMor;

        for (int i = 0; i < snapTree.size() && snapMor == null; i++) {
            VirtualMachineSnapshotTree node = snapTree.get(i);

            if (node.getName().equals(findName)) {
                snapMor = node.getSnapshot();
            } else {
                List<VirtualMachineSnapshotTree> childTree = node.getChildSnapshotList();
                snapMor = findSnapshotInTree(childTree, findName);
            }
        }
        return snapMor;
    }

    public static byte[] composeDiskInfo(List<Ternary<String, String, String>> diskInfo, int disksInChain, boolean includeBase) throws IOException {

        BufferedWriter out = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            out = new BufferedWriter(new OutputStreamWriter(bos,"UTF-8"));

            out.write("disksInChain=" + disksInChain);
            out.newLine();

            out.write("disksInBackup=" + diskInfo.size());
            out.newLine();

            out.write("baseDiskIncluded=" + includeBase);
            out.newLine();

            int seq = disksInChain - 1;
            for (Ternary<String, String, String> item : diskInfo) {
                out.write(String.format("disk%d.fileName=%s", seq, item.first()));
                out.newLine();

                out.write(String.format("disk%d.baseFileName=%s", seq, item.second()));
                out.newLine();

                if (item.third() != null) {
                    out.write(String.format("disk%d.parentFileName=%s", seq, item.third()));
                    out.newLine();
                }
                seq--;
            }

            out.newLine();
        } finally {
            if (out != null)
                out.close();
        }

        return bos.toByteArray();
    }

    public static OptionValue[] composeVncOptions(OptionValue[] optionsToMerge, boolean enableVnc, String vncPassword, int vncPort, String keyboardLayout) {

        int numOptions = 3;
        boolean needKeyboardSetup = false;
        if (keyboardLayout != null && !keyboardLayout.isEmpty()) {
            numOptions++;
            needKeyboardSetup = true;
        }

        if (optionsToMerge != null)
            numOptions += optionsToMerge.length;

        OptionValue[] options = new OptionValue[numOptions];
        int i = 0;
        if (optionsToMerge != null) {
            for (int j = 0; j < optionsToMerge.length; j++)
                options[i++] = optionsToMerge[j];
        }

        options[i] = new OptionValue();
        options[i].setKey("RemoteDisplay.vnc.enabled");
        options[i++].setValue(enableVnc ? "true" : "false");

        options[i] = new OptionValue();
        options[i].setKey("RemoteDisplay.vnc.password");
        options[i++].setValue(vncPassword);

        options[i] = new OptionValue();
        options[i].setKey("RemoteDisplay.vnc.port");
        options[i++].setValue("" + vncPort);

        if (needKeyboardSetup) {
            options[i] = new OptionValue();
            options[i].setKey("RemoteDisplay.vnc.keymap");
            options[i++].setValue(keyboardLayout);
        }

        return options;
    }

    public static void setVmScaleUpConfig(VirtualMachineConfigSpec vmConfig, int cpuCount, int cpuSpeedMHz, int cpuReservedMhz, int memoryMB, int memoryReserveMB,
            boolean limitCpuUse) {

        // VM config for scaling up
        vmConfig.setMemoryMB((long)memoryMB);
        vmConfig.setNumCPUs(cpuCount);

        ResourceAllocationInfo cpuInfo = new ResourceAllocationInfo();
        if (limitCpuUse) {
            cpuInfo.setLimit((long)(cpuSpeedMHz * cpuCount));
        } else {
            cpuInfo.setLimit(-1L);
        }

        cpuInfo.setReservation((long)cpuReservedMhz);
        vmConfig.setCpuAllocation(cpuInfo);

        ResourceAllocationInfo memInfo = new ResourceAllocationInfo();
        memInfo.setLimit((long)memoryMB);
        memInfo.setReservation((long)memoryReserveMB);
        vmConfig.setMemoryAllocation(memInfo);

    }

    public static void setBasicVmConfig(VirtualMachineConfigSpec vmConfig, int cpuCount, int cpuSpeedMHz, int cpuReservedMhz, int memoryMB, int memoryReserveMB,
                                        String guestOsIdentifier, boolean limitCpuUse, boolean deployAsIs) {

        // VM config basics
        vmConfig.setMemoryMB((long)memoryMB);
        vmConfig.setNumCPUs(cpuCount);

        ResourceAllocationInfo cpuInfo = new ResourceAllocationInfo();
        if (limitCpuUse) {
            cpuInfo.setLimit(((long)cpuSpeedMHz * cpuCount));
        } else {
            cpuInfo.setLimit(-1L);
        }

        cpuInfo.setReservation((long)cpuReservedMhz);
        vmConfig.setCpuAllocation(cpuInfo);
        ResourceAllocationInfo memInfo = new ResourceAllocationInfo();
        memInfo.setLimit((long)memoryMB);
        memInfo.setReservation((long)memoryReserveMB);
        vmConfig.setMemoryAllocation(memInfo);

        if (!deployAsIs) {
            // Deploy as-is uses the cloned VM guest OS
            vmConfig.setGuestId(guestOsIdentifier);
        }

    }

    public static VirtualDevice prepareUSBControllerDevice() {
        s_logger.debug("Preparing USB controller(EHCI+UHCI) device");
        VirtualUSBController usbController = new VirtualUSBController(); //EHCI+UHCI
        usbController.setEhciEnabled(true);
        usbController.setAutoConnectDevices(true);

        return usbController;
    }

    public static PerfMetricId createPerfMetricId(PerfCounterInfo counterInfo, String instance) {
        PerfMetricId metricId = new PerfMetricId();
        metricId.setCounterId(counterInfo.getKey());
        metricId.setInstance(instance);
        return metricId;
    }

    public static String getDiskDeviceFileName(VirtualDisk diskDevice) {
        VirtualDeviceBackingInfo backingInfo = diskDevice.getBacking();
        if (backingInfo instanceof VirtualDiskFlatVer2BackingInfo) {
            final String vmdkName = ((VirtualDiskFlatVer2BackingInfo)backingInfo).getFileName().replace(".vmdk", "");
            if (vmdkName.contains("/")) {
                return vmdkName.split("/", 2)[1];
            }
            return vmdkName;
        }
        return null;
    }

    public static ManagedObjectReference getDiskDeviceDatastore(VirtualDisk diskDevice) throws Exception {
        VirtualDeviceBackingInfo backingInfo = diskDevice.getBacking();
        assert (backingInfo instanceof VirtualDiskFlatVer2BackingInfo);
        return ((VirtualDiskFlatVer2BackingInfo)backingInfo).getDatastore();
    }

    public static Object getPropValue(ObjectContent oc, String name) {
        List<DynamicProperty> props = oc.getPropSet();

        for (DynamicProperty prop : props) {
            if (prop.getName().equalsIgnoreCase(name))
                return prop.getVal();
        }

        return null;
    }

    public static String getFileExtension(String fileName, String defaultExtension) {
        int pos = fileName.lastIndexOf('.');
        if (pos < 0)
            return defaultExtension;

        return fileName.substring(pos);
    }

    public static boolean isSameHost(String ipAddress, String destName) {
        // TODO : may need to do DNS lookup to compare IP address exactly
        return ipAddress.equals(destName);
    }

    public static String getExceptionMessage(Throwable e) {
        return getExceptionMessage(e, false);
    }

    public static String getExceptionMessage(Throwable e, boolean printStack) {
        //TODO: in vim 5.1, exceptions do not have a base exception class, MethodFault becomes a FaultInfo that we can only get
        // from individual exception through getFaultInfo, so we have to use reflection here to get MethodFault information.
        try {
            Class<? extends Throwable> cls = e.getClass();
            Method mth = cls.getDeclaredMethod("getFaultInfo", (Class<?>)null);
            if (mth != null) {
                Object fault = mth.invoke(e, (Object[])null);
                if (fault instanceof MethodFault) {
                    final StringWriter writer = new StringWriter();
                    writer.append("Exception: " + fault.getClass().getName() + "\n");
                    writer.append("message: " + ((MethodFault)fault).getFaultMessage() + "\n");

                    if (printStack) {
                        writer.append("stack: ");
                        e.printStackTrace(new PrintWriter(writer));
                    }
                    return writer.toString();
                }
            }
        } catch (Exception ex) {
            s_logger.info("[ignored]"
                    + "failed to get message for exception: " + e.getLocalizedMessage());
        }

        return ExceptionUtil.toString(e, printStack);
    }

    public static VirtualMachineMO pickOneVmOnRunningHost(List<VirtualMachineMO> vmList, boolean bFirstFit) throws Exception {
        List<VirtualMachineMO> candidates = new ArrayList<VirtualMachineMO>();

        for (VirtualMachineMO vmMo : vmList) {
            HostMO hostMo = vmMo.getRunningHost();
            if (hostMo.isHyperHostConnected())
                candidates.add(vmMo);
        }

        if (candidates.size() == 0)
            return null;

        if (bFirstFit)
            return candidates.get(0);

        Random random = new Random();
        return candidates.get(random.nextInt(candidates.size()));
    }

    public static boolean isDvPortGroup(ManagedObjectReference networkMor) {
        return "DistributedVirtualPortgroup".equalsIgnoreCase(networkMor.getType());
    }

    public static boolean isFeatureLicensed(VmwareHypervisorHost hyperHost, String featureKey) throws Exception {
        boolean hotplugSupportedByLicense = false;
        String licenseName;
        LicenseAssignmentManagerMO licenseAssignmentMgrMo;

        licenseAssignmentMgrMo = hyperHost.getLicenseAssignmentManager();
        // Check if license supports the feature
        hotplugSupportedByLicense = licenseAssignmentMgrMo.isFeatureSupported(featureKey, hyperHost.getMor());
        // Fetch license name
        licenseName = licenseAssignmentMgrMo.getHostLicenseName(hyperHost.getMor());

        if (!hotplugSupportedByLicense) {
            throw new Exception("hotplug feature is not supported by license : [" + licenseName + "] assigned to host : " + hyperHost.getHyperHostName());
        }

        return hotplugSupportedByLicense;
    }

    public static String getVCenterSafeUuid(DatastoreMO dsMo) throws Exception{
        // Object name that is greater than 32 is not safe in vCenter
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        if (dsMo.getDatastoreType().equalsIgnoreCase("VVOL")) {
            return CustomFieldConstants.CLOUD_UUID + "-" + uuid;
        }
        return uuid;
    }

    public static String getRecommendedDiskControllerFromDescriptor(GuestOsDescriptor guestOsDescriptor) throws Exception {
        String recommendedController = guestOsDescriptor.getRecommendedDiskController();

        // By-pass auto detected PVSCSI controller to use LsiLogic Parallel instead
        if (recommendedController.equals(ParaVirtualSCSIController.class.getSimpleName())) {
            return VirtualLsiLogicController.class.getSimpleName();
        }

        return recommendedController;
    }

    public static String trimSnapshotDeltaPostfix(String name) {
        String[] tokens = name.split("-");
        if (tokens.length > 1 && tokens[tokens.length - 1].matches("[0-9]{6,}")) {
            List<String> trimmedTokens = new ArrayList<String>();
            for (int i = 0; i < tokens.length - 1; i++)
                trimmedTokens.add(tokens[i]);
            return StringUtils.join(trimmedTokens, "-");
        }
        return name;
    }

    public static boolean isControllerOsRecommended(DiskControllerMapping diskControllerMapping) {
        return DiskControllerType.osdefault.toString().equals(diskControllerMapping.getName());
    }

    public static XMLGregorianCalendar getXMLGregorianCalendar(final Date date, final int offsetSeconds) throws DatatypeConfigurationException {
        if (offsetSeconds > 0) {
            date.setTime(date.getTime() - offsetSeconds * 1000);
        }
        final GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(date);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
    }

    public static HostMO getHostMOFromHostName(final VmwareContext context, final String hostName) {
        HostMO host = null;
        if (StringUtils.isNotBlank(hostName) && hostName.contains("@")) {
            String hostMorInfo = hostName.split("@")[0];
            if (hostMorInfo.contains(":")) {
                ManagedObjectReference morHost = new ManagedObjectReference();
                morHost.setType(hostMorInfo.split(":")[0]);
                morHost.setValue(hostMorInfo.split(":")[1]);
                host = new HostMO(context, morHost);
            }
        }
        return host;
    }

    public static DiskControllerMappingVO getDiskControllerMapping(String name, String controllerReference) {
        for (DiskControllerMappingVO mapping : diskControllerMappings) {
            if (mapping.getName().equals(name) || mapping.getControllerReference().equals(controllerReference)) {
                return mapping;
            }
        }
        s_logger.debug(String.format("No corresponding disk controller mapping found for name [%s] and controller reference [%s].", name, controllerReference));
        throw new CloudRuntimeException("Specified disk controller is invalid.");
    }

    public static List<DiskControllerMappingVO> getAllDiskControllerMappingsExceptOsdefault() {
        List<DiskControllerMappingVO> mappings = ObjectUtils.isEmpty(diskControllerMappings) ? CopyCommand.getDiskControllerMappings() : diskControllerMappings;
        return mappings.stream()
                .filter(c -> !VmwareHelper.isControllerOsRecommended(c))
                .collect(Collectors.toList());
    }

    public static Set<DiskControllerMappingVO> getRequiredDiskControllers(Pair<DiskControllerMappingVO, DiskControllerMappingVO> controllerInfo,
                                                                      boolean isSystemVM) {
        Set<DiskControllerMappingVO> requiredDiskControllers = new HashSet<>();
        requiredDiskControllers.add(controllerInfo.first());
        if (!isSystemVM) {
            requiredDiskControllers.add(controllerInfo.second());
        }
        return requiredDiskControllers;
    }

    public static Pair<DiskControllerMappingVO, DiskControllerMappingVO> convertRecommendedDiskControllers(Pair<DiskControllerMappingVO, DiskControllerMappingVO> controllerInfo,
                                                                                                           VirtualMachineMO vmMo,
                                                                                                           VmwareHypervisorHost host,
                                                                                                           String guestOsIdentifier) throws Exception {
        if (VmwareHelper.isControllerOsRecommended(controllerInfo.first()) && VmwareHelper.isControllerOsRecommended(controllerInfo.second())) {
            String recommendedDiskControllerClassName = vmMo != null ? vmMo.getRecommendedDiskController(null) : host.getRecommendedDiskController(guestOsIdentifier);
            DiskControllerMappingVO recommendedDiskController = getAllDiskControllerMappingsExceptOsdefault().stream()
                    .filter(c -> c.getControllerReference().contains(recommendedDiskControllerClassName))
                    .findFirst()
                    .orElseThrow(() -> new CloudRuntimeException("Unable to find the recommended disk controller."));
            return new Pair<>(recommendedDiskController, recommendedDiskController);
        }
        return controllerInfo;
    }

    public static void addDiskControllersToVmConfigSpec(VirtualMachineConfigSpec vmConfigSpec, Set<DiskControllerMappingVO> requiredDiskControllers,
                                                        boolean isSystemVm) throws Exception {
        int currentKey = -1;

        for (DiskControllerMappingVO diskController : requiredDiskControllers) {
            Class<?> controllerClass = Class.forName(diskController.getControllerReference());
            if (controllerClass == VirtualIDEController.class) {
                continue;
            }
            for (int bus = 0; bus < diskController.getMaxControllerCount(); bus++) {
                VirtualController controller = (VirtualController) controllerClass.newInstance();
                controller.setBusNumber(bus);
                controller.setKey(currentKey);
                currentKey--;

                if (controller instanceof VirtualSCSIController) {
                    ((VirtualSCSIController) controller).setSharedBus(VirtualSCSISharing.NO_SHARING);
                }

                VirtualDeviceConfigSpec controllerConfigSpec = new VirtualDeviceConfigSpec();
                controllerConfigSpec.setDevice(controller);
                controllerConfigSpec.setOperation(VirtualDeviceConfigSpecOperation.ADD);
                vmConfigSpec.getDeviceChange().add(controllerConfigSpec);

                if (isSystemVm) {
                    break;
                }
            }
        }
    }

    public static Pair<DiskControllerMappingVO, DiskControllerMappingVO> getDiskControllersFromVmSettings(String rootDiskControllerDetail,
                                                                                                          String dataDiskControllerDetail,
                                                                                                          boolean isSystemVm) {
        String updatedRootDiskControllerDetail = isSystemVm ? "lsilogic" : ObjectUtils.defaultIfNull(rootDiskControllerDetail, "osdefault");
        String updatedDataDiskControllerDetail = ObjectUtils.defaultIfNull(dataDiskControllerDetail, updatedRootDiskControllerDetail);

        DiskControllerMappingVO specifiedRootDiskController = getDiskControllerMapping(updatedRootDiskControllerDetail, null);
        DiskControllerMappingVO specifiedDataDiskController = getDiskControllerMapping(updatedDataDiskControllerDetail, null);

        return chooseRequiredDiskControllers(specifiedRootDiskController, specifiedDataDiskController);
    }

    protected static Pair<DiskControllerMappingVO, DiskControllerMappingVO> chooseRequiredDiskControllers(DiskControllerMappingVO rootDiskController, DiskControllerMappingVO dataDiskController) {
        if (isControllerOsRecommended(rootDiskController) && isControllerOsRecommended(dataDiskController)) {
            s_logger.debug("Choosing 'osrecommended' for both disk controllers.");
            return new Pair<>(rootDiskController, dataDiskController);
        }
        if (isControllerOsRecommended(rootDiskController)) {
            s_logger.debug(String.format("Root disk controller is 'osrecommended', but data disk controller is [%s]; therefore, we will only use the controllers specified for the data disk.",
                    dataDiskController.getName()));
            return new Pair<>(dataDiskController, dataDiskController);
        }
        if (isControllerOsRecommended(dataDiskController)) {
            s_logger.debug(String.format("Data disk controller is 'osrecommended', but root disk controller is [%s]; therefore, we will only use the controllers specified for the root disk.",
                    rootDiskController.getName()));
            return new Pair<>(rootDiskController, rootDiskController);
        }

        if (rootDiskController.getBusName().equals(dataDiskController.getBusName())) {
            s_logger.debug("Root and data disk controllers share the same bus type; therefore, we will only use the controllers specified for the root disk.");
            return new Pair<>(rootDiskController, rootDiskController);
        }
        s_logger.debug("Root and data disk controllers do not share the same bus type; therefore, we will use both of them on the virtual machine.");
        return new Pair<>(rootDiskController, dataDiskController);
    }

    public static DiskControllerMappingVO getControllerBasedOnDiskType(Pair<DiskControllerMappingVO, DiskControllerMappingVO> controllerInfo,
                                                    DiskTO disk) {
        if (disk.getType() == Volume.Type.ROOT || disk.getDiskSeq() == 0) {
            s_logger.debug("Chose disk controller for vol " + disk.getType() + " -> " + controllerInfo.first()
                    + ", based on root disk controller settings at global configuration setting.");
            return controllerInfo.first();
        }
        s_logger.debug("Chose disk controller for vol " + disk.getType() + " -> " + controllerInfo.second()
                + ", based on default data disk controller setting i.e. Operating system recommended."); // Need to bring in global configuration setting & template level setting.
        return controllerInfo.second();
    }
}
