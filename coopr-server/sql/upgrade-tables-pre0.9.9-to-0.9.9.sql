#
# Copyright © 2012-2014 Cask Data, Inc.
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
# mysql -u $user -p -h $hostname $dbname < upgrade-tables-pre0.9.9-to-0.9.9.sql

# Resource versioning (COOPR-617)
ALTER TABLE `providerTypes` ADD version BIGINT NOT NULL DEFAULT 1 AFTER name;
ALTER TABLE `automatorTypes` ADD version BIGINT NOT NULL DEFAULT 1 AFTER name;
ALTER TABLE `providers` ADD version BIGINT NOT NULL DEFAULT 1 AFTER name;
ALTER TABLE `hardwareTypes` ADD version BIGINT NOT NULL DEFAULT 1 AFTER name;
ALTER TABLE `imageTypes` ADD version BIGINT NOT NULL DEFAULT 1 AFTER name;
ALTER TABLE `services` ADD version BIGINT NOT NULL DEFAULT 1 AFTER name;
ALTER TABLE `clusterTemplates` ADD version BIGINT NOT NULL DEFAULT 1 AFTER name;

ALTER TABLE `providerTypes` DROP PRIMARY KEY;
ALTER TABLE `providerTypes` ADD PRIMARY KEY (name, version, tenant_id);
ALTER TABLE `automatorTypes` DROP PRIMARY KEY;
ALTER TABLE `automatorTypes` ADD PRIMARY KEY (name, version, tenant_id);
ALTER TABLE `providers` DROP PRIMARY KEY;
ALTER TABLE `providers` ADD PRIMARY KEY (name, version, tenant_id);
ALTER TABLE `hardwareTypes` DROP PRIMARY KEY;
ALTER TABLE `hardwareTypes` ADD PRIMARY KEY (name, version, tenant_id);
ALTER TABLE `imageTypes` DROP PRIMARY KEY;
ALTER TABLE `imageTypes` ADD PRIMARY KEY (name, version, tenant_id);
ALTER TABLE `services` DROP PRIMARY KEY;
ALTER TABLE `services` ADD PRIMARY KEY (name, version, tenant_id);
ALTER TABLE `clusterTemplates` DROP PRIMARY KEY;
ALTER TABLE `clusterTemplates` ADD PRIMARY KEY (name, version, tenant_id);

# Node hours API (COOPR-581)
ALTER TABLE `tasks`
    ADD type VARCHAR(64) AFTER status,
    ADD cluster_template_name VARCHAR(255) AFTER type,
    ADD user_id VARCHAR(255) after cluster_template_name,
    ADD tenant_id VARCHAR(64) after user_id;

# Partial Templates (COOPR-653)
CREATE TABLE IF NOT EXISTS partialTemplates (
    name VARCHAR(255),
    version BIGINT,
    tenant_id VARCHAR(64),
    partialTemplate MEDIUMBLOB,
    PRIMARY KEY (name, version, tenant_id)
) ENGINE = InnoDB;
