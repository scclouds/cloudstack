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
-- Schema upgrade from 4.20.0.0 to 4.20.0.1
--;

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

CREATE TABLE IF NOT EXISTS `cloud_usage`.`quota_usage_detail` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `tariff_id` bigint(20) unsigned NOT NULL COMMENT 'ID of the tariff of the quota usage detail calculated, foreign key to tariff table',
    `quota_usage_id` bigint(20) unsigned NOT NULL COMMENT 'ID of the aggregation of quota usage details, foreign key to quota usage table',
    `quota_used` decimal(20,8) NOT NULL COMMENT 'Amount of quota used',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_quota_usage_detail__tariff_id` FOREIGN KEY (`tariff_id`) REFERENCES `cloud_usage`.`quota_tariff` (`id`),
    CONSTRAINT `fk_quota_usage_detail__quota_usage_id` FOREIGN KEY (`quota_usage_id`) REFERENCES `cloud_usage`.`quota_usage` (`id`));

INSERT INTO cloud.role_permissions (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'quotaStatementDetails', permission, sort_order
FROM cloud.role_permissions rp
WHERE rule = 'quotaStatement'
  AND NOT EXISTS(SELECT 1 FROM cloud.role_permissions rp_ WHERE rp.role_id = rp_.role_id AND rp_.rule = 'quotaStatementDetails');

--- Add 2FA permissions to default roles
INSERT INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission)
SELECT
  UUID(),
  rp.role_id,
  'listUserTwoFactorAuthenticatorProviders',
  'ALLOW'
FROM
  `cloud`.`role_permissions` rp
  INNER JOIN `cloud`.`roles` r ON rp.role_id = r.id
  AND r.is_default = TRUE
  AND r.name != 'Root Admin'
  AND r.removed IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp2 WHERE rp2.role_id = rp.role_id AND rule = 'listUserTwoFactorAuthenticatorProviders'
  )
GROUP BY
  rp.role_id;

INSERT INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission)
SELECT
  UUID(),
  rp.role_id,
  'setupUserTwoFactorAuthentication',
  'ALLOW'
FROM
  `cloud`.`role_permissions` rp
  INNER JOIN cloud.roles r ON rp.role_id = r.id
  AND r.is_default = TRUE
  AND r.name != 'Root Admin'
  AND r.removed IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp2 WHERE rp2.role_id = rp.role_id AND rule = 'setupUserTwoFactorAuthentication'
  )
GROUP BY
  rp.role_id;

INSERT INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission)
SELECT
  UUID(),
  rp.role_id,
  'validateUserTwoFactorAuthenticationCode',
  'ALLOW'
FROM
  `cloud`.`role_permissions` rp
  INNER JOIN cloud.roles r ON rp.role_id = r.id
  AND r.is_default = TRUE
  AND r.name != 'Root Admin'
  AND r.removed IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp2 WHERE rp2.role_id = rp.role_id AND rule = 'validateUserTwoFactorAuthenticationCode'
  )
GROUP BY
  rp.role_id;

--- KVM Incremental Snapshots
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.snapshot_store_ref', 'kvm_checkpoint_path', 'varchar(255)');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.snapshot_store_ref', 'end_of_chain', 'int(1) unsigned');

INSERT INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'quotaCreditsList', permission, sort_order
FROM `cloud`.`role_permissions` rp
WHERE rp.rule = 'quotaStatement'
AND NOT EXISTS(SELECT 1 FROM cloud.role_permissions rp_ WHERE rp.role_id = rp_.role_id AND rp_.rule = 'quotaCreditsList');

-- Add last_id to the volumes table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'last_id', 'bigint(20) unsigned DEFAULT NULL');

-- Restaurar configurações 'usage.execution.timezone' e 'usage.aggregation.timezone'. As configurações são inseridas após
-- o upgrade. Assim, é necessário inserir as configurações manualmente para derivar o valor da 'usage.timezone'.

INSERT INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `description`, `default_value`, `updated`, `scope`, `is_dynamic`, `group_id`, `subgroup_id`, `display_text`)
SELECT 'Usage', 'DEFAULT', 'management-server', 'usage.aggregation.timezone', 'GMT', 'The timezone to use for usage stats aggregation', 'GMT', NULL, NULL, 0, 7, 22, 'Usage aggregation timezone'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `cloud`.`configuration` WHERE `name` = 'usage.aggregation.timezone');

INSERT INTO `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `description`, `default_value`, `updated`, `scope`, `is_dynamic`, `group_id`, `subgroup_id`, `display_text`)
SELECT 'Usage', 'DEFAULT', 'management-server', 'usage.execution.timezone', NULL, 'The timezone to use for usage job execution time', NULL, NULL, NULL, 0, 7, 22, 'Usage execution timezone'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `cloud`.`configuration` WHERE `name` = 'usage.execution.timezone');

UPDATE `cloud`.`configuration`
SET `value` = (SELECT `value` FROM `cloud`.`configuration` WHERE `name` = 'usage.timezone')
WHERE `name` IN ('usage.execution.timezone', 'usage.aggregation.timezone');

DELETE FROM `cloud`.`configuration`
WHERE `name` = 'usage.timezone';

-- Add posting date to quota credits table.
CALL `cloud_usage`.`IDEMPOTENT_ADD_COLUMN`('cloud_usage.quota_credits', 'posting_date', 'datetime COMMENT "Posting date of the payment"');

UPDATE `cloud_usage`.`quota_credits`
SET `posting_date` = `updated_on`
WHERE `posting_date` IS NULL;

ALTER TABLE `cloud_usage`.`quota_credits` MODIFY COLUMN `posting_date` datetime NOT NULL DEFAULT NOW() COMMENT 'Posting date of the payment';

-- Change deleteEvent and archiveEvent permissions for default roles.
UPDATE `cloud`.`role_permissions` rp, `cloud`.`roles` r
SET rp.`permission` = 'DENY'
WHERE
    rp.`role_id` = r.`id`
  AND (rp.`rule` = 'deleteEvents' OR rp.`rule` = 'archiveEvents')
  AND r.`is_default` = TRUE
  AND r.`name` != 'Root Admin'
  AND r.`removed` IS NULL;

-- Whitelabel GUI
CREATE TABLE IF NOT EXISTS `cloud`.`gui_themes` (
    `id` bigint(20) unsigned NOT NULL auto_increment,
    `uuid` varchar(255) UNIQUE,
    `name` varchar(2048) NOT NULL COMMENT 'A name to identify the theme.',
    `description` varchar(4096) DEFAULT NULL COMMENT 'A description for the theme.',
    `css` text DEFAULT NULL COMMENT 'The CSS to be retrieved and imported into the GUI when matching the theme access configurations.',
    `json_configuration` text DEFAULT NULL COMMENT 'The JSON with the configurations to be retrieved and imported into the GUI when matching the theme access configurations.',
    `recursive_domains` tinyint(1) DEFAULT 0 COMMENT 'Defines whether the subdomains of the informed domains are considered. Default value is false.',
    `is_public` tinyint(1) default 1 COMMENT 'Defines whether a theme can be retrieved by anyone when only the `internet_domains_names` is informed. If the `domain_uuids` or `account_uuids` is informed, it is considered as `false`.',
    `created` datetime NOT NULL,
    `removed` datetime DEFAULT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `cloud`.`gui_themes_details` (
    `id` bigint(20) unsigned NOT NULL auto_increment,
    `gui_theme_id` bigint(20) unsigned NOT NULL COMMENT 'Foreign key referencing the GUI theme on `gui_themes` table.',
    `type` varchar(100) NOT NULL COMMENT 'The type of GUI theme details. Valid options are: `account`, `domain` and `commonName`',
    `value` text NOT NULL COMMENT 'The value of the `type` details. Can be an UUID (account or domain) or internet common name.',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_gui_themes_details__gui_theme_id` FOREIGN KEY (`gui_theme_id`) REFERENCES `gui_themes`(`id`)
);
