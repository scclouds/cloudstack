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

-- Schema upgrade from 4.20.0.2 to 4.20.0.3

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.firewall_rules', 'removed', 'datetime DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.load_balancer_vm_map', 'removed', 'datetime DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.load_balancer_cert_map', 'removed', 'datetime DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.load_balancer_healthcheck_policies', 'removed', 'datetime DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.load_balancer_stickiness_policies', 'removed', 'datetime DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.global_load_balancer_lb_rule_map', 'removed', 'datetime DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.elastic_lb_vm_map', 'removed', 'datetime DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.tungsten_lb_health_monitor', 'removed', 'datetime DEFAULT NULL');

ALTER TABLE `cloud`.`load_balancer_vm_map`
DROP KEY `load_balancer_id`,
ADD UNIQUE KEY `load_balancer_id` (`load_balancer_id`, `instance_id`, `instance_ip`, `removed`);

ALTER TABLE `cloud`.`global_load_balancer_lb_rule_map`
DROP KEY `gslb_rule_id`,
ADD UNIQUE KEY `gslb_rule_id` (`gslb_rule_id`, `lb_rule_id`, `removed`);
