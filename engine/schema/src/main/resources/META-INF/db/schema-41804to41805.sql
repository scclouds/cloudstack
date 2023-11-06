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

-- Schema upgrade from 4.18.0.4 to 4.18.0.5

-- Adjustments for Whitelabel GUI functionality
CREATE TABLE IF NOT EXISTS `cloud`.`gui_themes_details` (
    `id` bigint(20) unsigned NOT NULL auto_increment,
    `gui_theme_id` bigint(20) unsigned NOT NULL COMMENT 'Foreign key referencing the GUI theme on `gui_themes` table.',
    `type` varchar(100) DEFAULT NULL COMMENT 'The type of GUI theme details. Valid options are: `account`, `domain` and `commonName`',
    `value` text COMMENT 'The value of the `type` details. Can be an UUID (account or domain) or internet common name.',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_gui_themes_details__gui_theme_id` FOREIGN KEY (`gui_theme_id`) REFERENCES `gui_themes`(`id`)
);

ALTER TABLE `cloud`.`gui_themes` ADD COLUMN `recursive_domains` tinyint(1) DEFAULT 0 COMMENT 'Defines whether the subdomains of the informed domains are considered. Default value is false.';

CREATE OR REPLACE
VIEW `cloud`.`gui_themes_view` AS
SELECT
    `cloud`.`gui_themes`.`id` AS `id`,
    `cloud`.`gui_themes`.`uuid` AS `uuid`,
    `cloud`.`gui_themes`.`name` AS `name`,
    `cloud`.`gui_themes`.`description` AS `description`,
    `cloud`.`gui_themes`.`css` AS `css`,
    `cloud`.`gui_themes`.`json_configuration` AS `json_configuration`,
    (SELECT group_concat(gtd.`value` separator ',') FROM `cloud`.`gui_themes_details` gtd WHERE gtd.`type` = 'commonName' AND gtd.gui_theme_id = `cloud`.`gui_themes`.`id`) common_names,
    (SELECT group_concat(gtd.`value` separator ',') FROM `cloud`.`gui_themes_details` gtd WHERE gtd.`type` = 'domain' AND gtd.gui_theme_id = `cloud`.`gui_themes`.`id`) domains,
    (SELECT group_concat(gtd.`value` separator ',') FROM `cloud`.`gui_themes_details` gtd WHERE gtd.`type` = 'account' AND gtd.gui_theme_id = `cloud`.`gui_themes`.`id`) accounts,
    `cloud`.`gui_themes`.`recursive_domains` AS `recursive_domains`,
    `cloud`.`gui_themes`.`is_public` AS `is_public`,
    `cloud`.`gui_themes`.`created` AS `created`,
    `cloud`.`gui_themes`.`removed` AS `removed`
FROM `cloud`.`gui_themes` LEFT JOIN `cloud`.`gui_themes_details` ON `cloud`.`gui_themes_details`.`gui_theme_id` = `cloud`.`gui_themes`.`id`
GROUP BY `cloud`.`gui_themes`.`id`;
