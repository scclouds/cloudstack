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

import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.api.command.admin.hostdevices.CreateDeviceOfferingCmd;
import org.apache.cloudstack.api.command.admin.hostdevices.UpdateDeviceOfferingCmd;
import org.apache.cloudstack.api.command.user.hostdevices.ListDeviceOfferingsCmd;
import org.apache.cloudstack.api.response.DeviceOfferingResponse;
import org.apache.cloudstack.framework.config.Configurable;

import java.util.List;

public interface DeviceOfferingManager extends Configurable, Manager, PluggableService {
    DeviceOffering createDeviceOffering(CreateDeviceOfferingCmd cmd);

    boolean assignVirtualMachineToDeviceOffering(Long virtualMachineId, Long deviceOfferingId);

    boolean removeVirtualMachineFromDeviceOffering(Long virtualMachineId, Long deviceOfferingId);

    Pair<List<? extends DeviceOffering>, Integer> listDeviceOfferings(ListDeviceOfferingsCmd listDeviceOfferingsCmd);

    DeviceOfferingResponse generateDeviceOfferingResponse(DeviceOffering offering);

    void unassignVmFromOfferings(Long vmId);

    boolean isVmAssignedToDeviceOfferings(VirtualMachine vm);

    DeviceOffering updateDeviceOffering(UpdateDeviceOfferingCmd updateDeviceOfferingCmd);

    List<? extends DeviceOffering> getDeviceOfferingsByVmId(Long vmId);

    boolean canAccountAccessOffering(DeviceOffering deviceOffering, Account newAccount);

    boolean deleteOffering(Long id);
}
