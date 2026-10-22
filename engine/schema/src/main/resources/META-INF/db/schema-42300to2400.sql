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
-- Schema upgrade from 4.23.0.0 to 24.0.0
--;

alter table `cloud`.`host` modify column `public_ip_address` varchar(45);
alter table `cloud`.`console_proxy` modify column `public_ip_address` varchar(45);
alter table `cloud`.`console_proxy` modify column `public_netmask` varchar(50);
alter table `cloud`.`secondary_storage_vm` modify column `public_ip_address` varchar(45);
alter table `cloud`.`secondary_storage_vm` modify column `public_netmask` varchar(50);

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.host_pod_ref','ip6_gateway', 'VARCHAR(45) DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.host_pod_ref','ip6_cidr', 'VARCHAR(50) DEFAULT NULL');

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.dc_storage_network_ip_range','ip6_gateway', 'VARCHAR(45) DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.dc_storage_network_ip_range','ip6_cidr', 'VARCHAR(50) DEFAULT NULL');
alter table `cloud`.`dc_storage_network_ip_range` modify column `start_ip` char(40) DEFAULT NULL COMMENT 'start ip address';
alter table `cloud`.`dc_storage_network_ip_range` modify column `end_ip` char(40) DEFAULT NULL COMMENT 'end ip address';
alter table `cloud`.`dc_storage_network_ip_range` modify column `gateway` varchar(15) DEFAULT NULL COMMENT 'gateway ip address';
alter table `cloud`.`dc_storage_network_ip_range` modify column `netmask` varchar(15) DEFAULT NULL COMMENT 'netmask for storage network';
