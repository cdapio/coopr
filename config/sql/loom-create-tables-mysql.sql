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
# mysql -u $user -p -h $hostname $dbname < loom-create-tables-mysql.sql

CREATE TABLE IF NOT EXISTS clusters (
    id BIGINT,
    owner_id VARCHAR(255),
    name VARCHAR(255),
    create_time TIMESTAMP DEFAULT '0000-00-00 00:00:00',
    expire_time TIMESTAMP NULL,
    status VARCHAR(32),
    cluster MEDIUMBLOB,
    PRIMARY KEY (id),
    INDEX cluster_user_index (owner_id, id),
    INDEX ctime_index (create_time),
    INDEX status_index (status)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS jobs ( 
    job_num BIGINT,
    cluster_id BIGINT,
    status VARCHAR(32),
    create_time TIMESTAMP DEFAULT '0000-00-00 00:00:00',
    job MEDIUMBLOB,
    PRIMARY KEY (job_num, cluster_id),
    FOREIGN KEY (cluster_id) REFERENCES clusters(id),
    INDEX ctime_index (create_time),
    INDEX status_index (status)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS tasks (
    task_num BIGINT,
    job_num BIGINT,
    cluster_id BIGINT,
    submit_time TIMESTAMP DEFAULT '0000-00-00 00:00:00',
    status_time TIMESTAMP DEFAULT '0000-00-00 00:00:00',
    status VARCHAR(32),
    task MEDIUMBLOB,
    PRIMARY KEY (task_num, job_num, cluster_id),
    FOREIGN KEY (job_num, cluster_id) REFERENCES jobs(job_num, cluster_id),
    INDEX status_time_index (status_time),
    INDEX submit_time_index (submit_time),
    INDEX status_index (status)
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
    providerType MEDIUMBLOB,
    PRIMARY KEY (name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS automatorTypes (
    name VARCHAR(255),
    automatorType MEDIUMBLOB,
    PRIMARY KEY (name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS providers (
    name VARCHAR(255),
    provider MEDIUMBLOB,
    PRIMARY KEY (name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS hardwareTypes (
    name VARCHAR(255),
    hardwareType MEDIUMBLOB,
    PRIMARY KEY (name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS imageTypes (
    name VARCHAR(255),
    imageType MEDIUMBLOB,
    PRIMARY KEY (name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS services (
    name VARCHAR(255),
    service MEDIUMBLOB,
    PRIMARY KEY (name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS clusterTemplates (
    name VARCHAR(255),
    clusterTemplate MEDIUMBLOB,
    PRIMARY KEY (name)
) ENGINE = InnoDB;
