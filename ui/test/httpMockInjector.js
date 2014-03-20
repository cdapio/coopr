/**
 * Copyright 2012-2014, Continuuity, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Http mock injector for simulating responses from API.
 */

var providers = {};
providers.joyent = require('../../examples/providers/joyent.json');
providers.openstack = require('../../examples/providers/openstack.json');
providers.rackspace = require('../../examples/providers/rackspace.json');

var hardwaretypes = {};
hardwaretypes.small = require('../../examples/hardwaretypes/small.json');
hardwaretypes.medium = require('../../examples/hardwaretypes/medium.json');
hardwaretypes.large = require('../../examples/hardwaretypes/large.json');

var imagetypes = {};
imagetypes.centos6 = require('../../examples/imagetypes/centos6.json');
imagetypes.ubuntu12 = require('../../examples/imagetypes/ubuntu12.json');

var services = {};
services['apache-httpd'] = require('../../examples/services/apache-httpd.json');
services['fail2ban'] = require('../../examples/services/fail2ban.json');
services['firewall'] = require('../../examples/services/firewall.json');
services['hadoop-hdfs-datanode'] = require('../../examples/services/hadoop-hdfs-datanode.json');
services['hadoop-hdfs-journalnode'] = require('../../examples/services/hadoop-hdfs-journalnode.json');
services['hadoop-hdfs-namenode'] = require('../../examples/services/hadoop-hdfs-namenode.json');
services['hadoop-yarn-nodemanager'] = require('../../examples/services/hadoop-yarn-nodemanager.json');
services['hadoop-yarn-resourcemanager'] = require('../../examples/services/hadoop-yarn-resourcemanager.json');
services['haproxy'] = require('../../examples/services/haproxy.json');
services['hbase-master'] = require('../../examples/services/hbase-master.json');
services['hbase-regionserver'] = require('../../examples/services/hbase-regionserver.json');
services['hive-metastore'] = require('../../examples/services/hive-metastore.json');
services['hosts'] = require('../../examples/services/hosts.json');
services['mysql-server'] = require('../../examples/services/mysql-server.json');
services['nodejs'] = require('../../examples/services/nodejs.json');
services['oozie'] = require('../../examples/services/oozie.json');
services['php'] = require('../../examples/services/php.json');
services['reactor'] = require('../../examples/services/reactor.json');
services['zookeeper-server'] = require('../../examples/services/zookeeper-server.json');

var clustertemplates = {};
clustertemplates['hadoop-distributed'] = require('../../examples/clustertemplates/hadoop-distributed.json');
clustertemplates['hadoop-hbase-distributed'] = require('../../examples/clustertemplates/hadoop-hbase-distributed.json');
clustertemplates['hadoop-singlenode'] = require('../../examples/clustertemplates/hadoop-singlenode.json');
clustertemplates['lamp'] = require('../../examples/clustertemplates/lamp.json');

var clusters = require('./clusters/clusters.json');
var clusterDefinitions = require('./clusters/clusterdefinitions.json');
var clusterStatuses = require('./clusters/clusterstatuses.json');
var createCluster = require('./clusters/createcluster.json');

module.exports = function (nock, argv, clientAddr) {

  /**
   * Set up nock environment. Disable net connection.
   */
  nock.disableNetConnect();

  /**
   * Clusters call mocks.
   */
  var clustersResponse = [];
  for (var item in clusters) {
    nock(clientAddr)
      .persist()
      .get('/v1/loom/clusters/' + item)
      .reply(200, clusterDefinitions[item]);

    nock(clientAddr)      
      .persist()
      .get('/v1/loom/clusters/' + item + '/status')
      .reply(200, clusterStatuses[item]);

    nock(clientAddr)
      .persist()
      .delete('/v1/loom/clusters/' + item)
      .reply(200);

    clustersResponse.push(clusters[item]);
  }

  nock(clientAddr)
    .persist()
    .post('/v1/loom/clusters', createCluster)
    .reply(200);

  nock(clientAddr)
    .persist()
    .get('/v1/loom/clusters')
    .reply(200, clustersResponse);


  /**
   * Mocks for all providers calls.
   */
  var providersResponse = [];
  for (var item in providers) {
    nock(clientAddr)
      .persist()
      .get('/v1/loom/providers/' + item)
      .reply(200, providers[item]);

    nock(clientAddr)
      .persist()
      .post('/v1/loom/providers', providers[item])
      .reply(200);

    nock(clientAddr)
      .persist()
      .put('/v1/loom/providers/' + item, providers[item])
      .reply(200);

    nock(clientAddr)
      .persist()
      .delete('/v1/loom/providers/' + item)
      .reply(200);

    providersResponse.push(providers[item]);
  }
  nock(clientAddr)
    .persist()
    .get('/v1/loom/providers')
    .reply(200, providersResponse);

  /**
   * Mocks for all hardwaretypes calls.
   */
  var hardwaretypesResponse = [];
  for (var item in hardwaretypes) {
    nock(clientAddr)
      .persist()
      .get('/v1/loom/hardwaretypes/' + item)
      .reply(200, hardwaretypes[item]);

    nock(clientAddr)
      .persist()
      .post('/v1/loom/hardwaretypes', hardwaretypes[item])
      .reply(200);

    nock(clientAddr)
      .persist()
      .put('/v1/loom/hardwaretypes/' + item, hardwaretypes[item])
      .reply(200);

    nock(clientAddr)
      .persist()
      .delete('/v1/loom/hardwaretypes/' + item)
      .reply(200);

    hardwaretypesResponse.push(hardwaretypes[item]);
  }
  nock(clientAddr)
    .persist()
    .get('/v1/loom/hardwaretypes')
    .reply(200, hardwaretypesResponse);

  /**
   * Mocks for all imagetypes calls.
   */
  var imagetypesResponse = [];
  for (var item in imagetypes) {
    nock(clientAddr)
      .persist()
      .get('/v1/loom/imagetypes/' + item)
      .reply(200, imagetypes[item]);

    nock(clientAddr)
      .persist()
      .post('/v1/loom/imagetypes', imagetypes[item])
      .reply(200);

    nock(clientAddr)
      .persist()
      .put('/v1/loom/imagetypes/' + item, imagetypes[item])
      .reply(200);

    nock(clientAddr)
      .persist()
      .delete('/v1/loom/imagetypes/' + item)
      .reply(200);

    imagetypesResponse.push(imagetypes[item]);
  }
  nock(clientAddr)
    .persist()
    .get('/v1/loom/imagetypes')
    .reply(200, imagetypesResponse);


  /**
   * Mocks for all services calls.
   */
  var servicesResponse = [];
  for (var item in services) {
    nock(clientAddr)
      .persist()
      .get('/v1/loom/services/' + item)
      .reply(200, services[item]);

    nock(clientAddr)
      .persist()
      .post('/v1/loom/services', services[item])
      .reply(200);

    nock(clientAddr)
      .persist()
      .put('/v1/loom/services/' + item, services[item])
      .reply(200);

    nock(clientAddr)
      .persist()
      .delete('/v1/loom/services/' + item)
      .reply(200);

    servicesResponse.push(services[item]);
  }
  nock(clientAddr)
    .persist()
    .get('/v1/loom/services')
    .reply(200, servicesResponse);

  /**
   * Mocks for all cluster templates calls.
   */
  var clustertemplatesResponse = [];
  for (var item in clustertemplates) {
    nock(clientAddr)
      .persist()
      .get('/v1/loom/clustertemplates/' + item)
      .reply(200, clustertemplates[item]);

    nock(clientAddr)
      .persist()
      .post('/v1/loom/clustertemplates', clustertemplates[item])
      .reply(200);

    nock(clientAddr)
      .persist()
      .put('/v1/loom/clustertemplates/' + item, clustertemplates[item])
      .reply(200);

    nock(clientAddr)      
      .persist()
      .delete('/v1/loom/clustertemplates/' + item)
      .reply(200);

    clustertemplatesResponse.push(clustertemplates[item]);
  }
  nock(clientAddr)
    .persist()
    .get('/v1/loom/clustertemplates')
    .reply(200, clustertemplatesResponse);

  /**
   * Blanket for unhandled mocks.
   */
  nock(clientAddr)
    .persist()
    .filteringPath(function(path){
        return '/';
    })
    .get('/')
    .reply(500, 'Route handler not available.');

  nock(clientAddr)
    .persist()
    .filteringPath(function(path){
        return '/';
    })
    .post('/')
    .reply(500, 'Route handler not available.');

  nock(clientAddr)
    .persist()
    .filteringPath(function(path){
        return '/';
    })
    .put('/')
    .reply(500, 'Route handler not available.');

};
