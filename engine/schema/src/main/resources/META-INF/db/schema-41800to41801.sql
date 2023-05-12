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
-- Schema upgrade from 4.18.0.0 to 4.18.0.1
--;
-- create_public_parameter_on_roles. #6960

ALTER TABLE `cloud`.`roles` ADD COLUMN IF NOT EXISTS `public_role` tinyint(1) NOT NULL DEFAULT '1'
COMMENT 'Indicates whether the role will be visible to all users (public) or only to root admins (private). If this parameter is not specified during the creation of the role its value will be defaulted to true (public).';

-- IP quarantine #7378
CREATE TABLE IF NOT EXISTS `cloud`.`quarantined_ips` (
                                                         `id` bigint(20) unsigned NOT NULL auto_increment,
                                                         `uuid` varchar(255) UNIQUE,
                                                         `public_ip_address_id` bigint(20) unsigned NOT NULL COMMENT 'ID of the quarantined public IP address, foreign key to `user_ip_address` table',
                                                         `previous_owner_id` bigint(20) unsigned NOT NULL COMMENT 'ID of the previous owner of the public IP address, foreign key to `user_ip_address` table',
                                                         `created` datetime NOT NULL,
                                                         `removed` datetime DEFAULT NULL,
                                                         `end_date` datetime NOT NULL,
                                                         `removal_reason` VARCHAR(255) DEFAULT NULL,
                                                         PRIMARY KEY (`id`),
                                                         CONSTRAINT `fk_quarantined_ips__public_ip_address_id` FOREIGN KEY(`public_ip_address_id`) REFERENCES `cloud`.`user_ip_address`(`id`),
                                                         CONSTRAINT `fk_quarantined_ips__previous_owner_id` FOREIGN KEY(`previous_owner_id`) REFERENCES `cloud`.`account`(`id`)
);

-- re add removed column from !171
ALTER TABLE `cloud`.`vm_instance` ADD COLUMN IF NOT EXISTS `backup_volumes` text DEFAULT NULL COMMENT 'details of backedup volumes';

-- [Veeam] disable jobs but keep backups #6589
ALTER TABLE `cloud`.`backups` ADD IF NOT EXISTS backup_volumes TEXT NULL COMMENT 'details of backedup volumes';

-- Populate column backup_volumes in table backups with a GSON
-- formed by concatenating the UUID, type, size, path and deviceId
-- of the volumes of VMs that have some backup offering.
-- Required for the restore process of a backup using Veeam
-- The Gson result can be in one of this formats:
-- When VM has only ROOT disk: [{"uuid":"<uuid>","type":"<type>","size":<size>,"path":"<path>","deviceId":<deviceId>}]
-- When VM has more tha one disk: [{"uuid":"<uuid>","type":"<type>","size":<size>,"path":"<path>","deviceId":<deviceId>}, {"uuid":"<uuid>","type":"<type>","size":<size>,"path":"<path>","deviceId":<deviceId>}, <>]
UPDATE `cloud`.`backups` b INNER JOIN `cloud`.`vm_instance` vm ON b.vm_id = vm.id SET b.backup_volumes = (SELECT CONCAT("[", GROUP_CONCAT( CONCAT("{\"uuid\":\"", v.uuid, "\",\"type\":\"", v.volume_type, "\",\"size\":", v.`size`, ",\"path\":\"", v.path, "\",\"deviceId\":", v.device_id, "}") SEPARATOR ","), "]") FROM `cloud`.`volumes` v WHERE v.instance_id = vm.id);

ALTER TABLE `cloud`.`vm_instance` ADD IF NOT EXISTS backup_name varchar(255) NULL COMMENT 'backup job name when using Veeam provider';

UPDATE `cloud`.`vm_instance` vm INNER JOIN `cloud`.`backup_offering` bo ON vm.backup_offering_id = bo.id SET vm.backup_name = CONCAT(vm.instance_name, "-CSBKP-", vm.uuid);

-- [Usage] Create VPC billing #7235
CREATE TABLE IF NOT EXISTS `cloud_usage`.`usage_vpc` (
                                                         `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
                                                         `vpc_id` bigint(20) unsigned NOT NULL,
                                                         `zone_id` bigint(20) unsigned NOT NULL,
                                                         `account_id` bigint(20) unsigned NOT NULL,
                                                         `domain_id` bigint(20) unsigned NOT NULL,
                                                         `state` varchar(100) DEFAULT NULL,
                                                         `created` datetime NOT NULL,
                                                         `removed` datetime DEFAULT NULL,
                                                         PRIMARY KEY (`id`)
) ENGINE=InnoDB CHARSET=utf8;

ALTER TABLE `cloud_usage`.`cloud_usage` ADD IF NOT EXISTS state VARCHAR(100) DEFAULT NULL;

-- [Usage] Create network billing #7236
CREATE TABLE IF NOT EXISTS `cloud_usage`.`usage_networks` (
                                                              `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
                                                              `network_offering_id` bigint(20) unsigned NOT NULL,
                                                              `zone_id` bigint(20) unsigned NOT NULL,
                                                              `network_id` bigint(20) unsigned NOT NULL,
                                                              `account_id` bigint(20) unsigned NOT NULL,
                                                              `domain_id` bigint(20) unsigned NOT NULL,
                                                              `state` varchar(100) DEFAULT NULL,
                                                              `removed` datetime DEFAULT NULL,
                                                              `created` datetime NOT NULL,
                                                              PRIMARY KEY (`id`)
) ENGINE=InnoDB CHARSET=utf8;

ALTER TABLE `cloud_usage`.`cloud_usage` ADD COLUMN IF NOT EXISTS state VARCHAR(100) DEFAULT NULL;

-- Fix backup dates #6473
-- Drop all backup records and change date column type to DATETIME. The data will be resynchronized automatically later;
DELETE FROM `cloud`.`backups`
WHERE exists(SELECT
                 COLUMN_NAME,
                 DATA_TYPE
             FROM INFORMATION_SCHEMA.COLUMNS
             WHERE TABLE_SCHEMA = 'cloud'
               AND TABLE_NAME = 'backups'
               AND COLUMN_NAME = 'date'
               AND DATA_TYPE = 'varchar');

ALTER TABLE `cloud`.`backups`
    MODIFY COLUMN `date` DATETIME;

-- Flexible tags
ALTER TABLE `cloud`.`storage_pool_tags` ADD COLUMN IF NOT EXISTS is_tag_a_rule int(1) UNSIGNED DEFAULT 0;

ALTER TABLE `cloud`.`storage_pool_tags` MODIFY tag text NOT NULL;

CREATE OR REPLACE
    VIEW `cloud`.`storage_pool_view` AS
SELECT
    `storage_pool`.`id` AS `id`,
    `storage_pool`.`uuid` AS `uuid`,
    `storage_pool`.`name` AS `name`,
    `storage_pool`.`status` AS `status`,
    `storage_pool`.`path` AS `path`,
    `storage_pool`.`pool_type` AS `pool_type`,
    `storage_pool`.`host_address` AS `host_address`,
    `storage_pool`.`created` AS `created`,
    `storage_pool`.`removed` AS `removed`,
    `storage_pool`.`capacity_bytes` AS `capacity_bytes`,
    `storage_pool`.`capacity_iops` AS `capacity_iops`,
    `storage_pool`.`scope` AS `scope`,
    `storage_pool`.`hypervisor` AS `hypervisor`,
    `storage_pool`.`storage_provider_name` AS `storage_provider_name`,
    `storage_pool`.`parent` AS `parent`,
    `cluster`.`id` AS `cluster_id`,
    `cluster`.`uuid` AS `cluster_uuid`,
    `cluster`.`name` AS `cluster_name`,
    `cluster`.`cluster_type` AS `cluster_type`,
    `data_center`.`id` AS `data_center_id`,
    `data_center`.`uuid` AS `data_center_uuid`,
    `data_center`.`name` AS `data_center_name`,
    `data_center`.`networktype` AS `data_center_type`,
    `host_pod_ref`.`id` AS `pod_id`,
    `host_pod_ref`.`uuid` AS `pod_uuid`,
    `host_pod_ref`.`name` AS `pod_name`,
    `storage_pool_tags`.`tag` AS `tag`,
    `storage_pool_tags`.`is_tag_a_rule` AS `is_tag_a_rule`,
    `op_host_capacity`.`used_capacity` AS `disk_used_capacity`,
    `op_host_capacity`.`reserved_capacity` AS `disk_reserved_capacity`,
    `async_job`.`id` AS `job_id`,
    `async_job`.`uuid` AS `job_uuid`,
    `async_job`.`job_status` AS `job_status`,
    `async_job`.`account_id` AS `job_account_id`
FROM
    ((((((`cloud`.`storage_pool`
        LEFT JOIN `cloud`.`cluster` ON ((`storage_pool`.`cluster_id` = `cluster`.`id`)))
        LEFT JOIN `cloud`.`data_center` ON ((`storage_pool`.`data_center_id` = `data_center`.`id`)))
        LEFT JOIN `cloud`.`host_pod_ref` ON ((`storage_pool`.`pod_id` = `host_pod_ref`.`id`)))
        LEFT JOIN `cloud`.`storage_pool_tags` ON (((`storage_pool_tags`.`pool_id` = `storage_pool`.`id`))))
        LEFT JOIN `cloud`.`op_host_capacity` ON (((`storage_pool`.`id` = `op_host_capacity`.`host_id`)
        AND (`op_host_capacity`.`capacity_type` IN (3 , 9)))))
        LEFT JOIN `cloud`.`async_job` ON (((`async_job`.`instance_id` = `storage_pool`.`id`)
        AND (`async_job`.`instance_type` = 'StoragePool')
        AND (`async_job`.`job_status` = 0))));


ALTER TABLE `cloud`.`host_tags` ADD COLUMN IF NOT EXISTS is_tag_a_rule int(1) UNSIGNED DEFAULT 0;

ALTER TABLE `cloud`.`host_tags` MODIFY tag text NOT NULL;

CREATE OR REPLACE VIEW `cloud`.`host_view` AS
SELECT
    host.id,
    host.uuid,
    host.name,
    host.status,
    host.disconnected,
    host.type,
    host.private_ip_address,
    host.version,
    host.hypervisor_type,
    host.hypervisor_version,
    host.capabilities,
    host.last_ping,
    host.created,
    host.removed,
    host.resource_state,
    host.mgmt_server_id,
    host.cpu_sockets,
    host.cpus,
    host.speed,
    host.ram,
    cluster.id cluster_id,
    cluster.uuid cluster_uuid,
    cluster.name cluster_name,
    cluster.cluster_type,
    data_center.id data_center_id,
    data_center.uuid data_center_uuid,
    data_center.name data_center_name,
    data_center.networktype data_center_type,
    host_pod_ref.id pod_id,
    host_pod_ref.uuid pod_uuid,
    host_pod_ref.name pod_name,
    GROUP_CONCAT(DISTINCT(host_tags.tag)) AS tag,
    `host_tags`.`is_tag_a_rule` AS `is_tag_a_rule`,
    guest_os_category.id guest_os_category_id,
    guest_os_category.uuid guest_os_category_uuid,
    guest_os_category.name guest_os_category_name,
    mem_caps.used_capacity memory_used_capacity,
    mem_caps.reserved_capacity memory_reserved_capacity,
    cpu_caps.used_capacity cpu_used_capacity,
    cpu_caps.reserved_capacity cpu_reserved_capacity,
    async_job.id job_id,
    async_job.uuid job_uuid,
    async_job.job_status job_status,
    async_job.account_id job_account_id,
    oobm.enabled AS `oobm_enabled`,
    oobm.power_state AS `oobm_power_state`,
    ha_config.enabled AS `ha_enabled`,
    ha_config.ha_state AS `ha_state`,
    ha_config.provider AS `ha_provider`,
    `last_annotation_view`.`annotation` AS `annotation`,
    `last_annotation_view`.`created` AS `last_annotated`,
    `user`.`username` AS `username`
FROM
    `cloud`.`host`
        LEFT JOIN
    `cloud`.`cluster` ON host.cluster_id = cluster.id
        LEFT JOIN
    `cloud`.`data_center` ON host.data_center_id = data_center.id
        LEFT JOIN
    `cloud`.`host_pod_ref` ON host.pod_id = host_pod_ref.id
        LEFT JOIN
    `cloud`.`host_details` ON host.id = host_details.host_id
        AND host_details.name = 'guest.os.category.id'
        LEFT JOIN
    `cloud`.`guest_os_category` ON guest_os_category.id = CONVERT ( host_details.value, UNSIGNED )
        LEFT JOIN
    `cloud`.`host_tags` ON host_tags.host_id = host.id
        LEFT JOIN
    `cloud`.`op_host_capacity` mem_caps ON host.id = mem_caps.host_id
        AND mem_caps.capacity_type = 0
        LEFT JOIN
    `cloud`.`op_host_capacity` cpu_caps ON host.id = cpu_caps.host_id
        AND cpu_caps.capacity_type = 1
        LEFT JOIN
    `cloud`.`async_job` ON async_job.instance_id = host.id
        AND async_job.instance_type = 'Host'
        AND async_job.job_status = 0
        LEFT JOIN
    `cloud`.`oobm` ON oobm.host_id = host.id
        left join
    `cloud`.`ha_config` ON ha_config.resource_id=host.id
        and ha_config.resource_type='Host'
        LEFT JOIN
    `cloud`.`last_annotation_view` ON `last_annotation_view`.`entity_uuid` = `host`.`uuid`
        LEFT JOIN
    `cloud`.`user` ON `user`.`uuid` = `last_annotation_view`.`user_uuid`
GROUP BY
    `host`.`id`;


-- Via Java the configuration is inserted after the upgrade. Therefore, in order to derive the 'usage.timezone' value from `usage.aggregation.timezone' and
-- `usage.execution.timezone', we need to insert the configuration manually.

INSERT INTO `cloud`.`configuration` (category, `instance`, component, name, value, description, default_value, `scope`, is_dynamic)
SELECT 'Usage', 'DEFAULT', 'UsageServiceImpl', 'usage.timezone', 'GMT', 'The timezone that will be used in the Usage plugin for executing the usage job and aggregating the stats. The dates in logs in the processes will be presented according to this configuration.', 'GMT', 'Global', 0
FROM cloud.configuration
WHERE NOT EXISTS (SELECT 1 FROM cloud.configuration WHERE name = 'usage.timezone');

UPDATE `cloud`.`configuration` a
INNER JOIN `cloud`.`configuration` b ON b.name = 'usage.execution.timezone'
INNER JOIN `cloud`.`configuration` c ON c.name = 'usage.aggregation.timezone'
SET a.value=IFNULL(IFNULL(b.value,c.value), 'GMT')
WHERE  a.name = 'usage.timezone'
AND NOT EXISTS (SELECT 1 FROM cloud.configuration WHERE name = 'usage.execution.timezone');

DELETE
FROM    `cloud`.`configuration`
WHERE   name in ('usage.execution.timezone', 'usage.aggregation.timezone');

-- Create table to persist quota email template configurations
CREATE TABLE IF NOT EXISTS `cloud_usage`.`quota_email_configuration`(
    `account_id` int(11) NOT NULL,
    `email_template_id` bigint(20) NOT NULL,
    `enabled` int(1) UNSIGNED NOT NULL,
    PRIMARY KEY (`account_id`, `email_template_id`),
    CONSTRAINT `FK_quota_email_configuration_account_id` FOREIGN KEY (`account_id`) REFERENCES `cloud_usage`.`quota_account`(`account_id`),
    CONSTRAINT `FK_quota_email_configuration_email_te1mplate_id` FOREIGN KEY (`email_template_id`) REFERENCES `cloud_usage`.`quota_email_templates`(`id`));

-- Create view for quota summary
DROP VIEW IF EXISTS `cloud_usage`.`quota_summary_view`;
CREATE VIEW `cloud_usage`.`quota_summary_view` AS
SELECT
    cloud_usage.quota_account.account_id AS account_id,
    cloud_usage.quota_account.quota_balance AS quota_balance,
    cloud_usage.quota_account.quota_balance_date AS quota_balance_date,
    cloud_usage.quota_account.quota_enforce AS quota_enforce,
    cloud_usage.quota_account.quota_min_balance AS quota_min_balance,
    cloud_usage.quota_account.quota_alert_date AS quota_alert_date,
    cloud_usage.quota_account.quota_alert_type AS quota_alert_type,
    cloud_usage.quota_account.last_statement_date AS last_statement_date,
    cloud.account.uuid AS account_uuid,
    cloud.account.account_name AS account_name,
    cloud.account.state AS account_state,
    cloud.account.removed AS account_removed,
    cloud.domain.id AS domain_id,
    cloud.domain.uuid AS domain_uuid,
    cloud.domain.name AS domain_name,
    cloud.domain.path AS domain_path,
    cloud.domain.removed AS domain_removed,
    cloud.projects.uuid AS project_uuid,
    cloud.projects.name AS project_name,
    cloud.projects.removed AS project_removed
FROM
    cloud_usage.quota_account
        INNER JOIN cloud.account ON (cloud.account.id = cloud_usage.quota_account.account_id)
        INNER JOIN cloud.domain ON (cloud.domain.id = cloud.account.domain_id)
        LEFT JOIN cloud.projects ON (cloud.account.type = 5 AND cloud.account.id = cloud.projects.project_account_id);

--
CREATE TABLE IF NOT EXISTS `cloud_usage`.`usage_backup_object` (
                                                                   `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
                                                                   `backup_id` bigint(20) unsigned NOT NULL,
                                                                   `backup_offering_id` bigint(20) unsigned NOT NULL,
                                                                   `vm_id` bigint(20) unsigned NOT NULL,
                                                                   `zone_id` bigint(20) unsigned NOT NULL,
                                                                   `domain_id` bigint(20) unsigned NOT NULL,
                                                                   `account_id` bigint(20) unsigned NOT NULL,
                                                                   `size` bigint(20) unsigned NOT NULL,
                                                                   `protected_size` bigint(20) unsigned NOT NULL,
                                                                   `created` datetime DEFAULT NULL,
                                                                   `removed` datetime DEFAULT NULL,
                                                                   PRIMARY KEY (`id`)
);

-- update 'vm.allocation.algorithm' description
UPDATE  configuration
SET     description = "Order in which hosts within a cluster will be considered for VM/volume allocation. The value can be 'random', 'firstfit', 'userdispersing', 'userconcentratedpod_random', 'userconcentratedpod_firstfit', or 'firstfitleastconsumed'."
WHERE   name = 'vm.allocation.algorithm';
