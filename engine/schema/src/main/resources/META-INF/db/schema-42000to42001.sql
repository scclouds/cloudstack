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
GROUP BY
  rp.role_id;

INSERT INTO `cloud`.`role_permissions` (uuid, role_id, rule, permission, sort_order)
SELECT uuid(), role_id, 'quotaCreditsList', permission, sort_order
FROM `cloud`.`role_permissions` rp
WHERE rp.rule = 'quotaStatement'
AND NOT EXISTS(SELECT 1 FROM cloud.role_permissions rp_ WHERE rp.role_id = rp_.role_id AND rp_.rule = 'quotaCreditsList');

-- Add last_id to the volumes table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.volumes', 'last_id', 'bigint(20) unsigned DEFAULT NULL');
