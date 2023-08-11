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

-- Schema upgrade from 4.18.0.2 to 4.18.0.3

-- Make `usage_item_id` nullable.
ALTER TABLE `cloud_usage`.`quota_usage` MODIFY COLUMN `usage_item_id` bigint(20) unsigned NULL;

-- Make `zone_id` nullable.
ALTER TABLE `cloud_usage`.`quota_usage` MODIFY COLUMN zone_id bigint(20) unsigned NULL;

-- Add resource ID to Quota Usage table.
ALTER TABLE `cloud_usage`.`quota_usage` ADD `resource_id` bigint(20) unsigned NULL;

-- Add `processing_period` to Quota Tariff
ALTER TABLE `cloud_usage`.`quota_tariff` ADD `processing_period` varchar(20) DEFAULT 'BY_ENTRY' NOT NULL;

-- Add `execute_on` to Quota Tariff
ALTER TABLE `cloud_usage`.`quota_tariff` ADD `execute_on` int DEFAULT NULL NULL;

-- Set removed state for all removed accounts
UPDATE `cloud`.`account` SET state='removed' WHERE `removed` IS NOT NULL;

DROP VIEW IF EXISTS `cloud_usage`.`quota_usage_view`;
CREATE VIEW `cloud_usage`.`quota_usage_view` AS
select
    `qu`.`id` AS `id`,
    `qu`.`usage_item_id` AS `usage_item_id`,
    `qu`.`zone_id` AS `zone_id`,
    `qu`.`account_id` AS `account_id`,
    `qu`.`domain_id` AS `domain_id`,
    `qu`.`usage_type` AS `usage_type`,
    `qu`.`quota_used` AS `quota_used`,
    `qu`.`start_date` AS `start_date`,
    `qu`.`end_date` AS `end_date`,
    case when `cu`.`usage_id` is null then `qu`.`resource_id` else `cu`.`usage_id` end AS `resource_id`,
    case when `cu`.`network_id` is null and `qu`.`usage_type` in (4, 5) then `qu`.`resource_id` else `cu`.`network_id` end AS `network_id`,
    case when `cu`.`offering_id` is null and `qu`.`usage_type` in (13, 28) then `qu`.`resource_id` else `cu`.`offering_id` end AS `offering_id`
from
    (`cloud_usage`.`quota_usage` `qu`
        left join `cloud_usage`.`cloud_usage` `cu` on
        (`cu`.`id` = `qu`.`usage_item_id`));
