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
-- Schema upgrade from 4.16.0.14 to 4.16.0.15
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
