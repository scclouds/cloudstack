-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

-- Schema upgrade from 4.18.0.8 to 4.18.0.9

-- Remove on delete cascade from snapshot schedule
ALTER TABLE `cloud`.`snapshot_schedule` DROP CONSTRAINT `fk__snapshot_schedule_async_job_id`;

-- Disk Controller Mappings
CREATE TABLE IF NOT EXISTS `cloud`.`disk_controller_mapping` (
                                                                 `id` bigint(20) unsigned NOT NULL auto_increment,
    `uuid` varchar(255) UNIQUE NOT NULL,
    `name` varchar(255) NOT NULL,
    `controller_reference` varchar(255) NOT NULL,
    `bus_name` varchar(255) NOT NULL,
    `hypervisor` varchar(40) NOT NULL,
    `max_device_count` bigint unsigned DEFAULT NULL,
    `max_controller_count` bigint unsigned DEFAULT NULL,
    `vmdk_adapter_type` varchar(255) DEFAULT NULL,
    `min_hardware_version` varchar(20) DEFAULT NULL,
    `created` datetime NOT NULL,
    `removed` datetime DEFAULT NULL,
    PRIMARY KEY (`id`)
    );

-- Create a procedure to add VMware disk controller mappings
DROP PROCEDURE IF EXISTS `cloud`.`ADD_DISK_CONTROLLER_MAPPING`;

CREATE PROCEDURE `cloud`.`ADD_DISK_CONTROLLER_MAPPING` (
    IN disk_controller_name varchar(255),
    IN disk_controller_reference varchar(255),
    IN disk_controller_bus_name varchar(255),
    IN disk_controller_hypervisor varchar(40),
    IN disk_controller_max_device_count bigint unsigned,
    IN disk_controller_max_controller_count bigint unsigned,
    IN disk_controller_vmdk_adapter_type varchar(255),
    IN disk_controller_min_hardware_version varchar(20)
)
BEGIN
INSERT INTO cloud.disk_controller_mapping (uuid, name, controller_reference, bus_name, hypervisor, max_device_count,
                                           max_controller_count, vmdk_adapter_type, min_hardware_version, created)
SELECT UUID(), disk_controller_name, disk_controller_reference, disk_controller_bus_name, disk_controller_hypervisor,
       disk_controller_max_device_count, disk_controller_max_controller_count, disk_controller_vmdk_adapter_type,
       disk_controller_min_hardware_version, now()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM cloud.disk_controller_mapping WHERE cloud.disk_controller_mapping.name = disk_controller_name
                                                                AND cloud.disk_controller_mapping.hypervisor = disk_controller_hypervisor)
;END;

-- Add VMware's default disk controller mappings
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('osdefault', 'unused', 'unused', 'VMware', NULL, NULL, NULL, NULL);
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('ide', 'com.vmware.vim25.VirtualIDEController', 'ide', 'VMware', 2, 2, 'ide', NULL);
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('scsi', 'com.vmware.vim25.VirtualLsiLogicController', 'scsi', 'VMware', 16, 4, 'lsilogic', NULL);
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('buslogic', 'com.vmware.vim25.VirtualBusLogicController', 'scsi', 'VMware', 16, 4, 'buslogic', NULL);
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('lsilogic', 'com.vmware.vim25.VirtualLsiLogicController', 'scsi', 'VMware', 16, 4, 'lsilogic', NULL);
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('lsisas1068', 'com.vmware.vim25.VirtualLsiLogicSASController', 'scsi', 'VMware', 16, 4, 'lsilogic', NULL);
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('pvscsi', 'com.vmware.vim25.ParaVirtualSCSIController', 'scsi', 'VMware', 16, 4, 'lsilogic', '7');
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('sata', 'com.vmware.vim25.VirtualAHCIController', 'sata', 'VMware', 30, 4, 'ide', '10');
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('ahci', 'com.vmware.vim25.VirtualAHCIController', 'sata', 'VMware', 30, 4, 'ide', '10');
CALL `cloud`.`ADD_DISK_CONTROLLER_MAPPING` ('nvme', 'com.vmware.vim25.VirtualNVMEController', 'nvme', 'VMware', 15, 4, 'ide', '13');
