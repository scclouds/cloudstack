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

-- Schema upgrade from 4.20.0.2 to 4.20.0.3

--- Disable/enable NICs
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.nics','link_state', 'VARCHAR(10) NOT NULL DEFAULT ''Enabled'' COMMENT ''Indicates the link state of the NIC''');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.firewall_rules', 'removed', 'datetime DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.load_balancer_vm_map', 'removed', 'datetime DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.load_balancer_cert_map', 'removed', 'datetime DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.load_balancer_healthcheck_policies', 'removed', 'datetime DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.load_balancer_stickiness_policies', 'removed', 'datetime DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.global_load_balancer_lb_rule_map', 'removed', 'datetime DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.elastic_lb_vm_map', 'removed', 'datetime DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.tungsten_lb_health_monitor', 'removed', 'datetime DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.nic_secondary_ips','description', 'varchar(2048) DEFAULT NULL');

ALTER TABLE `cloud`.`load_balancer_vm_map`
DROP KEY `load_balancer_id`,
ADD UNIQUE KEY `load_balancer_id` (`load_balancer_id`, `instance_id`, `instance_ip`, `removed`);

ALTER TABLE `cloud`.`global_load_balancer_lb_rule_map`
DROP KEY `gslb_rule_id`,
ADD UNIQUE KEY `gslb_rule_id` (`gslb_rule_id`, `lb_rule_id`, `removed`);

-- Transfer value from "vm.stats.remove.batch.size" to "delete.query.batch.size", if the first exists and the latter still has its default value.
UPDATE `cloud`.`configuration` `cfg`
SET `cfg`.`value` = (
    SELECT `nested_cfg`.`value`
    FROM `cloud`.`configuration` `nested_cfg`
    WHERE `nested_cfg`.`name` = 'vm.stats.remove.batch.size'
)
WHERE `cfg`.`name` = 'delete.query.batch.size'
AND `cfg`.`value` = `cfg`.`default_value`
AND EXISTS (
    SELECT *
    FROM `cloud`.`configuration` `exists_check_cfg`
    WHERE `exists_check_cfg`.`name` = 'vm.stats.remove.batch.size'
);

-- Delete legacy "vm.stats.remove.batch.size" if it exists
DELETE FROM `cloud`.`configuration`
WHERE `name` = 'vm.stats.remove.batch.size';

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.networks', 'keep_mac_address_on_public_nic', 'TINYINT(1) NOT NULL DEFAULT 1');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vpc', 'keep_mac_address_on_public_nic', 'TINYINT(1) NOT NULL DEFAULT 1');

UPDATE `cloud`.`networks`
SET `keep_mac_address_on_public_nic` = COALESCE(
    (
        SELECT
            CASE
                WHEN `cfg`.`value` = 'false' THEN 0
                ELSE 1
            END
        FROM `cloud`.`configuration` `cfg`
        WHERE `cfg`.`name` = 'use.same.mac.address.for.public.nic.of.virtual.routers.on.same.network'
    ), 1
);

UPDATE `cloud`.`vpc`
SET `keep_mac_address_on_public_nic` = COALESCE(
    (
        SELECT
            CASE
                WHEN `cfg`.`value` = 'false' THEN 0
                ELSE 1
            END
        FROM `cloud`.`configuration` `cfg`
        WHERE `cfg`.`name` = 'use.same.mac.address.for.public.nic.of.virtual.routers.on.same.network'
    ), 1
);

DELETE FROM `cloud`.`configuration`
WHERE `name` = 'use.same.mac.address.for.public.nic.of.virtual.routers.on.same.network';

-- Set backup offering id back to backup name
UPDATE `cloud`.`vm_instance` vm INNER JOIN `cloud`.`backup_offering` bo ON vm.backup_offering_id = bo.id SET vm.backup_name = CONCAT(vm.instance_name, "-CSBKP-", bo.uuid);

-- Update vmSnapshot.strategies.exclude
UPDATE `cloud`.`configuration`
SET `value` = 'StorageVMSnapshotStrategy'
WHERE `name` = 'vmSnapshot.strategies.exclude' AND `value` IS NULL;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster', 'control_service_offering_id', 'bigint unsigned COMMENT "service offering ID for Control Node(s)"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.kubernetes_cluster', 'worker_service_offering_id', 'bigint unsigned COMMENT "service offering ID for Worker Node(s)"');

ALTER TABLE `cloud`.`kubernetes_cluster` ADD CONSTRAINT `fk_cluster__control_service_offering_id` FOREIGN KEY `fk_cluster__control_service_offering_id`(`control_service_offering_id`) REFERENCES `service_offering`(`id`);
ALTER TABLE `cloud`.`kubernetes_cluster` ADD CONSTRAINT `fk_cluster__worker_service_offering_id` FOREIGN KEY `fk_cluster__worker_service_offering_id`(`worker_service_offering_id`) REFERENCES `service_offering`(`id`);

-- Create native backup tables

CREATE TABLE IF NOT EXISTS `cloud`.`native_backup_pool_ref` (
    `id` bigint NOT NULL UNIQUE AUTO_INCREMENT,
    `backup_id` bigint unsigned NOT NULL COMMENT 'The backup ID. Foreign key that points to the backups table.',
    `storage_pool_id` bigint unsigned NOT NULL COMMENT 'The storage ID. Foreign key that points to the storage_pool table.',
    `volume_id` bigint unsigned NOT NULL COMMENT 'The volumes ID. Foreign key that points to the volumes table.',
    `backup_delta_path` varchar(255) COMMENT 'Path of the created delta.',
    `backup_parent_path` varchar(255) COMMENT 'Path of the created delta parent.',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_native_backup_pool_ref__backup_id` FOREIGN KEY (`backup_id`) REFERENCES `backups`(`id`),
    CONSTRAINT `fk_native_backup_pool_ref__storage_pool_id` FOREIGN KEY (`storage_pool_id`) REFERENCES `storage_pool`(`id`),
    CONSTRAINT `fk_native_backup_pool_ref__volume_id` FOREIGN KEY (`volume_id`) REFERENCES `volumes`(`id`)
);

CREATE TABLE IF NOT EXISTS `cloud`.`native_backup_store_ref` (
    `id` bigint NOT NULL UNIQUE AUTO_INCREMENT,
    `backup_id` bigint unsigned NOT NULL COMMENT 'The backup ID. Foreign key that points to the backups table.',
    `volume_id` bigint unsigned NOT NULL COMMENT 'The volume ID. Foreign key that points to the volumes table.',
    `volume_size` bigint unsigned NOT NULL COMMENT 'The volume size at the time of the backup.',
    `path` varchar(255) COMMENT 'Path of the backup.',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_native_backup_store_ref__backup_id` FOREIGN KEY (`backup_id`) REFERENCES `backups`(`id`),
    CONSTRAINT `fk_native_backup_store_ref__volume_id` FOREIGN KEY (`volume_id`) REFERENCES `volumes`(`id`)
);

CREATE TABLE IF NOT EXISTS `cloud`.`backup_details` (
    `id` bigint NOT NULL UNIQUE AUTO_INCREMENT,
    `backup_id` bigint unsigned NOT NULL COMMENT 'The backups ID. Foreign key that points to the backups table.',
    `name` varchar(255)  NOT NULL COMMENT 'The detail name.',
    `value` varchar(1024) NOT NULL COMMENT 'The detail value.',
    `display` tinyint(1) unsigned NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_backup_details__backup_id` FOREIGN KEY (`backup_id`) REFERENCES `backups`(`id`)
);
