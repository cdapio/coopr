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
# mysql -u $user -p -h $hostname $dbname < create-tables-mysql.sql

CREATE TABLE IF NOT EXISTS clusters (
    id BIGINT,
    owner_id VARCHAR(255),
    tenant_id VARCHAR(64),
    name VARCHAR(255),
    create_time TIMESTAMP DEFAULT '0000-00-00 00:00:00',
    expire_time TIMESTAMP NULL,
    status VARCHAR(32),
    latest_job_num BIGINT,
    cluster MEDIUMBLOB,
    PRIMARY KEY (id),
    INDEX cluster_account_index (tenant_id, owner_id, id),
    INDEX ctime_index (create_time)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS jobs ( 
    job_num BIGINT,
    cluster_id BIGINT,
    status VARCHAR(32),
    create_time TIMESTAMP DEFAULT '0000-00-00 00:00:00',
    job MEDIUMBLOB,
    PRIMARY KEY (cluster_id, job_num),
    FOREIGN KEY (cluster_id) REFERENCES clusters(id),
    INDEX ctime_index (create_time)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS tasks (
    task_num BIGINT,
    job_num BIGINT,
    cluster_id BIGINT,
    submit_time TIMESTAMP DEFAULT '0000-00-00 00:00:00',
    status_time TIMESTAMP DEFAULT '0000-00-00 00:00:00',
    status VARCHAR(32),
    type VARCHAR(64),
    cluster_template_name VARCHAR(255),
    user_id VARCHAR(255),
    tenant_id VARCHAR(64),
    task MEDIUMBLOB,
    PRIMARY KEY (cluster_id, job_num, task_num),
    FOREIGN KEY (cluster_id, job_num) REFERENCES jobs(cluster_id, job_num),
    INDEX status_time_index (status_time),
    INDEX submit_time_index (submit_time)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS nodes (
    id VARCHAR(64),
    cluster_id BIGINT,
    node MEDIUMBLOB,
    PRIMARY KEY (id),
    INDEX cluster_node_index (cluster_id, id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS providerTypes (
    name VARCHAR(255),
    version BIGINT,
    tenant_id VARCHAR(64),
    providerType MEDIUMBLOB,
    PRIMARY KEY (tenant_id, name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS automatorTypes (
    name VARCHAR(255),
    version BIGINT,
    tenant_id VARCHAR(64),
    automatorType MEDIUMBLOB,
    PRIMARY KEY (tenant_id, name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS providers (
    name VARCHAR(255),
    version BIGINT,
    tenant_id VARCHAR(64),
    provider MEDIUMBLOB,
    PRIMARY KEY (tenant_id, name, version)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS hardwareTypes (
    name VARCHAR(255),
    version BIGINT,
    tenant_id VARCHAR(64),
    hardwareType MEDIUMBLOB,
    PRIMARY KEY (tenant_id, name, version)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS imageTypes (
    name VARCHAR(255),
    version BIGINT,
    tenant_id VARCHAR(64),
    imageType MEDIUMBLOB,
    PRIMARY KEY (tenant_id, name, version)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS services (
    name VARCHAR(255),
    version BIGINT,
    tenant_id VARCHAR(64),
    service MEDIUMBLOB,
    PRIMARY KEY (tenant_id, name, version)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS clusterTemplates (
    name VARCHAR(255),
    version BIGINT,
    tenant_id VARCHAR(64),
    clusterTemplate MEDIUMBLOB,
    PRIMARY KEY (tenant_id, name, version)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS partialTemplates (
    name VARCHAR(255),
    version BIGINT,
    tenant_id VARCHAR(64),
    partialTemplate MEDIUMBLOB,
    PRIMARY KEY (tenant_id, name, version)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS tenants (
    id VARCHAR(64),
    name VARCHAR(255),
    workers INT,
    deleted BOOLEAN,
    create_time TIMESTAMP DEFAULT '0000-00-00 00:00:00',
    delete_time TIMESTAMP NULL,
    tenant MEDIUMBLOB,
    PRIMARY KEY (id),
    INDEX name_index (name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS provisioners (
    id VARCHAR(255),
    last_heartbeat TIMESTAMP NULL,
    capacity_total INTEGER,
    capacity_free INTEGER,
    provisioner MEDIUMBLOB,
    PRIMARY KEY (id),
    INDEX heartbeat_index (last_heartbeat),
    INDEX capacity_index (capacity_free)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS provisionerWorkers (
    provisioner_id VARCHAR(64),
    tenant_id VARCHAR(64),
    num_assigned INTEGER,
    num_live INTEGER,
    PRIMARY KEY (provisioner_id, tenant_id),
    INDEX tenant_index (tenant_id),
    INDEX assigned_index (num_assigned)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS pluginMeta (
    tenant_id VARCHAR(64),
    plugin_type VARCHAR(16),
    plugin_name VARCHAR(255),
    resource_type VARCHAR(255),
    name VARCHAR(255),
    version INTEGER,
    live BOOLEAN,
    slated BOOLEAN,
    deleted BOOLEAN,
    create_time TIMESTAMP DEFAULT '0000-00-00 00:00:00',
    delete_time TIMESTAMP NULL,
    PRIMARY KEY (tenant_id, plugin_type, plugin_name, resource_type, name, version)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(255),
    tenant_id VARCHAR(64),
    profile MEDIUMBLOB,
    PRIMARY KEY (tenant_id, user_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS sensitiveFields (
    tenant_id VARCHAR(64),
    cluster_id VARCHAR(255),
    fields MEDIUMBLOB,
    PRIMARY KEY (tenant_id, cluster_id)
) ENGINE = InnoDB;
