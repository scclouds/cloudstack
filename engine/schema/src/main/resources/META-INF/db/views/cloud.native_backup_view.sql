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

-- VIEW `cloud`.`native_backup_view`;

DROP VIEW IF EXISTS `cloud`.`native_backup_view`;
CREATE VIEW `cloud`.`native_backup_view` AS
SELECT  b.id,
        b.uuid,
        b.vm_id,
        b.type,
        b.date,
        b.status,
        b.backup_offering_id,
        MAX(CASE WHEN bd.name = 'image_store_id' THEN bd.value END) image_store_id,
        MAX(CASE WHEN bd.name = 'parent_id' THEN bd.value END) parent_id,
        MAX(CASE WHEN bd.name = 'end_of_chain' THEN bd.value END) end_of_chain,
        MAX(CASE WHEN bd.name = 'current' THEN bd.value END) current
FROM    backups b
LEFT    JOIN backup_details bd ON b.id = bd.backup_id
LEFT    JOIN backup_offering bo ON b.backup_offering_id = bo.id
WHERE   bo.provider='knib'
GROUP BY b.id;
