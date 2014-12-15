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

IF NOT EXISTS(SELECT * FROM sys.columns WHERE Name = N'version' AND OBJECT_ID = OBJECT_ID(N'providerTypes'))
BEGIN
  ALTER TABLE providerTypes ADD version BIGINT AFTER name;
  UPDATE providerTypes SET version = 1;
END

IF NOT EXISTS(SELECT * FROM sys.columns WHERE Name = N'version' AND OBJECT_ID = OBJECT_ID(N'automatorTypes'))
BEGIN
  ALTER TABLE automatorTypes ADD version BIGINT AFTER name;
  UPDATE automatorTypes SET version = 1;
END

IF NOT EXISTS(SELECT * FROM sys.columns WHERE Name = N'version' AND OBJECT_ID = OBJECT_ID(N'providers'))
BEGIN
  ALTER TABLE providers ADD version BIGINT AFTER name;
  UPDATE providers SET version = 1;
END

IF NOT EXISTS(SELECT * FROM sys.columns WHERE Name = N'version' AND OBJECT_ID = OBJECT_ID(N'hardwareTypes'))
BEGIN
  ALTER TABLE hardwareTypes ADD version BIGINT AFTER name;
  UPDATE hardwareTypes SET version = 1;
END

IF NOT EXISTS(SELECT * FROM sys.columns WHERE Name = N'version' AND OBJECT_ID = OBJECT_ID(N'imageTypes'))
BEGIN
  ALTER TABLE imageTypes ADD version BIGINT AFTER name;
  UPDATE imageTypes SET version = 1;
END

IF NOT EXISTS(SELECT * FROM sys.columns WHERE Name = N'version' AND OBJECT_ID = OBJECT_ID(N'services'))
BEGIN
  ALTER TABLE services ADD version BIGINT AFTER name;
  UPDATE services SET version = 1;
END

IF NOT EXISTS(SELECT * FROM sys.columns WHERE Name = N'version' AND OBJECT_ID = OBJECT_ID(N'clusterTemplates'))
BEGIN
  ALTER TABLE clusterTemplates ADD version BIGINT AFTER name;
  UPDATE clusterTemplates SET version = 1;
END
