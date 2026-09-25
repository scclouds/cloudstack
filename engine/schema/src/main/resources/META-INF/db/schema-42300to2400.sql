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
-- Schema upgrade from 4.23.0.0 to 24.0.0
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
  `one_time_use` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Whether the device will stay unavailable after releasing for manual cleanup or not',
  `created` datetime NOT NULL COMMENT 'Device creation timestamp',
  `removed` datetime DEFAULT NULL COMMENT 'Device removal timestamp',
  `state` varchar(255) NOT NULL COMMENT 'Device state',
  `type` varchar(255) NOT NULL COMMENT 'Devices type, based on its class',
  `instance_id` bigint unsigned DEFAULT NULL COMMENT 'Device allocator instance id. Foreign key that points to the vm_instance table',
  `account_id` bigint unsigned DEFAULT NULL COMMENT 'Device allocator account id. Foreign key that points to the account table',
  `domain_id` bigint unsigned DEFAULT NULL COMMENT 'Device allocator domain id. Foreign key that points to the domain table',
  `host_id` bigint unsigned DEFAULT NULL COMMENT 'Device host id. Foreign key that points to the host table',
  PRIMARY KEY (`id`),
  INDEX `i_host_pci_devices_host_id_state` (`host_id`, `state`),
  INDEX `i_host_pci_devices_device_tag` (`device_tag`),
  CONSTRAINT `fk_host_pci_devices_instance_id` FOREIGN KEY (`instance_id`) REFERENCES `vm_instance` (`id`),
  CONSTRAINT `fk_host_pci_devices_account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_host_pci_devices_domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain` (`id`),
  CONSTRAINT `fk_host_pci_devices_host_id` FOREIGN KEY (`host_id`) REFERENCES `host` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Device offerings table

CREATE TABLE IF NOT EXISTS `cloud`.`device_offerings` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'Row ID',
  `uuid` varchar(255) NOT NULL COMMENT 'Device offering UUID',
  `name` varchar(255) NOT NULL COMMENT 'Device offering name',
  `description` varchar(255) NOT NULL COMMENT 'Device offering description',
  `state` varchar(255) NOT NULL COMMENT 'Device offering state',
  `created` datetime NOT NULL COMMENT 'Device offering creation timestamp',
  `removed` datetime DEFAULT NULL COMMENT 'Device offering removal timestamp',
  `public` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Whether the offering is available for all users or not. Will always be false if the domain_id attribute is set',
  `domain_id` bigint unsigned DEFAULT NULL COMMENT 'The domain that this offering will be available to. Foreign key that points to the domain table',
  `zone_id` bigint unsigned DEFAULT NULL COMMENT 'The zone that this offering will be available to. Foreign key that points to the data_center table',
  PRIMARY KEY (`id`),
  INDEX `i_device_offerings_name` (`name`),
  CONSTRAINT `fk_device_offerings_domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain` (`id`),
  CONSTRAINT `fk_device_offerings_zone_id` FOREIGN KEY (`zone_id`) REFERENCES `data_center` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Device offerings device tags mapping table

CREATE TABLE IF NOT EXISTS `cloud`.`device_offering_device_tags` (
  `id` bigint unsigned NOT NULL auto_increment COMMENT 'Row ID',
  `device_offering_id` bigint unsigned NOT NULL COMMENT 'Device offering id. Foreign key that points to the device_offerings table',
  `device_tag` varchar(255) NOT NULL COMMENT 'Device tag, used to assign it to devices offerings',
  `amount` int NOT NULL COMMENT 'Device tag amount, used to define how many devices should be provisioned',
  PRIMARY KEY (`id`),
  INDEX `i_device_offering_device_tags_device_tag` (`device_tag`),
  CONSTRAINT `uc_device_offering_device_tags` UNIQUE (`device_offering_id`, `device_tag`),
  CONSTRAINT `fk_device_offering_device_tags_device_offering_id` FOREIGN KEY (`device_offering_id`) REFERENCES `device_offerings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- VM Instance device offering mapping table

CREATE TABLE IF NOT EXISTS `cloud`.`vm_instance_device_offerings` (
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'Row ID',
    `vm_instance_id` bigint unsigned NOT NULL COMMENT 'VM instance id. Foreign key that points to the vm_instance table',
    `device_offering_id` bigint unsigned NOT NULL COMMENT 'Device offering id. Foreign key that points to the device_offerings table',
    PRIMARY KEY (`id`),
    CONSTRAINT `uc_vm_instance_device_offerings_vm_offering` UNIQUE (`vm_instance_id`, `device_offering_id`),
    CONSTRAINT `fk_vm_instance_device_offerings_vm_instance_id` FOREIGN KEY (`vm_instance_id`) REFERENCES `vm_instance` (`id`),
    CONSTRAINT `fk_vm_instance_device_offerings_device_offering_id` FOREIGN KEY (`device_offering_id`) REFERENCES `device_offerings` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
