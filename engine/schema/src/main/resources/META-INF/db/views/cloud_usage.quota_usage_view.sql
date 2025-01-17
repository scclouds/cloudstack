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

-- cloud_usage.quota_usage_view source

DROP VIEW IF EXISTS `cloud_usage`.`quota_usage_view`;

CREATE VIEW `cloud_usage`.`quota_usage_view` AS
SELECT  `qu`.`id`,
        `qu`.`usage_item_id`,
        `qu`.`zone_id`,
        `qu`.`account_id`,
        `qu`.`domain_id`,
        `qu`.`usage_type`,
        `qu`.`quota_used`,
        `qu`.`start_date`,
        `qu`.`end_date`,
        CASE WHEN `cu`.`usage_id` IS NULL THEN `qu`.`resource_id` ELSE `cu`.`usage_id` END AS `resource_id`,
        CASE WHEN `cu`.`network_id` IS NULL AND `qu`.`usage_type` IN (4, 5) THEN `qu`.`resource_id` ELSE `cu`.`network_id` END AS `network_id`,
        CASE WHEN `cu`.`offering_id` IS NULL AND `qu`.`usage_type` IN (13, 28) THEN `qu`.`resource_id` ELSE `cu`.`offering_id` END AS `offering_id`
FROM    `cloud_usage`.`quota_usage` `qu`
LEFT JOIN `cloud_usage`.`cloud_usage` `cu` ON (`cu`.`id` = `qu`.`usage_item_id`);
