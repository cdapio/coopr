#
# Copyright 2012-2014, Continuuity, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# to use, run:
# mysql -u $user -p -h $hostname $dbname < loom-upgrade-tables-mysql.sql

ALTER TABLE clusters
    ADD COLUMN tenant_id VARCHAR(255),
    DROP INDEX cluster_user_index,
    ADD INDEX cluster_account_index (tenant_id, owner_id, id);

ALTER TABLE providerTypes
    ADD COLUMN tenant_id VARCHAR(255),
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (tenant_id, name);

ALTER TABLE automatorTypes
    ADD COLUMN tenant_id VARCHAR(255),
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (tenant_id, name);

ALTER TABLE providers
    ADD COLUMN tenant_id VARCHAR(255),
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (tenant_id, name);

ALTER TABLE hardwareTypes
    ADD COLUMN tenant_id VARCHAR(255),
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (tenant_id, name);

ALTER TABLE imageTypes
    ADD COLUMN tenant_id VARCHAR(255),
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (tenant_id, name);

ALTER TABLE services
    ADD COLUMN tenant_id VARCHAR(255),
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (tenant_id, name);

ALTER TABLE clusterTemplates
    ADD COLUMN tenant_id VARCHAR(255),
    DROP PRIMARY KEY,
    ADD PRIMARY KEY (tenant_id, name);
