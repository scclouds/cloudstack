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

-- Schema upgrade from 4.18.0.1 to 4.18.0.2

-- Fix comment of column `previous_owner_id` from table `quarantined_ips`
ALTER TABLE `cloud`.`quarantined_ips` MODIFY `previous_owner_id` bigint(20) unsigned NOT NULL COMMENT 'ID of the previous owner of the public IP address, foreign key to `account` table';

-- Add remover account ID to quarantined IPs table.
ALTER TABLE `cloud`.`quarantined_ips`
    ADD COLUMN `remover_account_id` bigint(20) unsigned DEFAULT NULL COMMENT 'ID of the account that removed the IP from quarantine, foreign key to `account` table',
    ADD CONSTRAINT `fk_quarantined_ips__remover_account_id` FOREIGN KEY(`remover_account_id`) REFERENCES `cloud`.`account`(`id`);

-- Invalidate existing console_session records
UPDATE `cloud`.`console_session` SET removed=now();
-- Modify acquired column in console_session to datetime type
ALTER TABLE `cloud`.`console_session` DROP `acquired`, ADD `acquired` datetime COMMENT 'When the session was acquired' AFTER `host_id`;

-- Quota inject tariff result into subsequent ones
ALTER TABLE `cloud_usage`.`quota_tariff` ADD COLUMN `position` bigint(20) NOT NULL DEFAULT 1 COMMENT 'Position in the execution sequence for tariffs of the same type' ;
