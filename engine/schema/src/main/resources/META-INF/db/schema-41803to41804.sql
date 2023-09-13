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

-- Schema upgrade from 4.18.0.3 to 4.18.0.4

-- Whitelabel GUI
CREATE TABLE IF NOT EXISTS `cloud`.`gui_themes` (
  `id` bigint(20) unsigned NOT NULL auto_increment,
  `uuid` varchar(255) UNIQUE,
  `name` varchar(2048) NOT NULL COMMENT 'A name to identify the theme.',
  `description` varchar(4096) DEFAULT NULL COMMENT 'A description for the theme.',
  `css` text DEFAULT NULL COMMENT 'The CSS to be retrieved and imported into the GUI when matching the theme access configurations.',
  `json_configuration` text DEFAULT NULL COMMENT 'The JSON with the configurations to be retrieved and imported into the GUI when matching the theme access configurations.',
  `common_names` text DEFAULT NULL COMMENT 'A set of internet Common Names (CN), fixed of wildcard, separated by comma that can use the theme; e.g.: *acme.com,acme2.com',
  `domain_uuids` text DEFAULT NULL COMMENT 'A set of domain IDs separated by comma that can use the theme.',
  `account_uuids` text DEFAULT NULL COMMENT 'A set of account IDs separated by comma that can use the theme.',
  `is_public` tinyint(1) default 1 COMMENT 'Defines whether a theme can be retrieved by anyone when only the `internet_domains_names` is informed. If the `domain_uuids` or `account_uuids` is informed, it is considered as `false`.',
  `created` datetime NOT NULL,
  `removed` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
);

-- Add posting date to quota credits table.
ALTER TABLE `cloud_usage`.`quota_credits`
    ADD COLUMN `posting_date` datetime NOT NULL DEFAULT NOW() COMMENT 'Posting date of the payment';
UPDATE `cloud_usage`.`quota_credits`
SET `posting_date` = `updated_on`;
