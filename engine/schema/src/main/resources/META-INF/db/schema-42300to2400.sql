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

-- Multiqueue for VRs
-- Add 'public_multiqueue_number' and 'private_multiqueue_number' columns to the 'cloud.network_offerings' table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings', 'public_multiqueue_number', 'INT DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.network_offerings', 'private_multiqueue_number', 'INT DEFAULT NULL');
-- Add 'public_multiqueue_number' and 'private_gateway_multiqueue_number' columns to the 'cloud.vpc_offerings' table
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vpc_offerings', 'public_multiqueue_number', 'INT DEFAULT NULL');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vpc_offerings', 'private_gateway_multiqueue_number', 'INT DEFAULT NULL');
