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

--;
-- Schema upgrade cleanup from 4.22.1.0 to 4.23.0.0
--;

-- KVM host devices table

CREATE TABLE IF NOT EXISTS `cloud`.`host_pci_devices` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'Row ID',
  `uuid` varchar(255) NOT NULL COMMENT 'UUID device UUID',
  `display_name` varchar(255) NOT NULL COMMENT 'Device human readable name',
  `pci_name` varchar(255) NOT NULL COMMENT 'Device unique identifier',
  `pci_class` varchar(255) NOT NULL COMMENT 'Device class',
  `pci_domain` varchar(255) NOT NULL COMMENT 'Device domain',
  `pci_bus` varchar(255) NOT NULL COMMENT 'Device bus',
  `pci_slot` varchar(255) NOT NULL COMMENT 'Device slot',
  `pci_function` varchar(255) NOT NULL COMMENT 'Device function',
  `pci_vendor_id` varchar(255) NOT NULL COMMENT 'Device vendor identifier',
  `pci_device_id` varchar(255) NOT NULL COMMENT 'Device identifier, set by its vendor',
  `device_tag` varchar(255) NOT NULL COMMENT 'Device tag, used to assign it to devices offerings',
  `created` datetime NOT NULL COMMENT 'Device creation timestamp',
  `removed` datetime DEFAULT NULL COMMENT 'Device removal timestamp',
  `state` varchar(255) NOT NULL COMMENT 'Device state',
  `type` varchar(255) NOT NULL COMMENT 'Devices type, based on its class',
  `instance_id` bigint unsigned DEFAULT NULL COMMENT 'Device allocator instance id. Foreign key that points to the vm_instance table',
  `account_id` bigint unsigned DEFAULT NULL COMMENT 'Device allocator account id. Foreign key that points to the account table',
  `host_id` bigint unsigned DEFAULT NULL COMMENT 'Device host id. Foreign key that points to the host table',
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_host_pci_devices_instance_id` FOREIGN KEY (`instance_id`) REFERENCES `vm_instance` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_host_pci_devices_account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_host_pci_devices_host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;