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
providers.joyent = require('./providers/joyent.json');
providers.openstack = require('./providers/openstack.json');
providers.rackspace = require('./providers/rackspace.json');

var hardwaretypes = {};
hardwaretypes.small = require('./hardwaretypes/small.json');
hardwaretypes.medium = require('./hardwaretypes/medium.json');
hardwaretypes.large = require('./hardwaretypes/large.json');

var imagetypes = {};
imagetypes.centos6 = require('./imagetypes/centos6.json');
imagetypes.ubuntu12 = require('./imagetypes/ubuntu12.json');

var services = {};
services['apache-httpd'] = require('./services/apache-httpd.json');
services['base'] = require('./services/base.json');
services['hadoop-hdfs-datanode'] = require('./services/hadoop-hdfs-datanode.json');
services['hadoop-hdfs-namenode'] = require('./services/hadoop-hdfs-namenode.json');
services['hadoop-yarn-nodemanager'] = require('./services/hadoop-yarn-nodemanager.json');
services['hadoop-yarn-resourcemanager'] = require('./services/hadoop-yarn-resourcemanager.json');
services['haproxy'] = require('./services/haproxy.json');
services['hbase-master'] = require('./services/hbase-master.json');
services['hbase-regionserver'] = require('./services/hbase-regionserver.json');
services['mysql-server'] = require('./services/mysql-server.json');
services['nodejs'] = require('./services/nodejs.json');
services['php'] = require('./services/php.json');
services['zookeeper-server'] = require('./services/zookeeper-server.json');
services['reactor'] = require('./services/reactor.json');
services['test-service'] = require('./services/test-service.json');

var clustertemplates = {};
clustertemplates['hadoop-distributed'] = require('./clustertemplates/hadoop-distributed.json');
clustertemplates['hadoop-hbase-distributed'] = require('./clustertemplates/hadoop-hbase-distributed.json');
clustertemplates['hadoop-singlenode'] = require('./clustertemplates/hadoop-singlenode.json');
clustertemplates['lamp'] = require('./clustertemplates/lamp.json');
clustertemplates['reactor-singlenode'] = require('./clustertemplates/reactor-singlenode.json');

var clusters = require('./clusters/clusters.json');
var clusterDefinitions = require('./clusters/clusterdefinitions.json');
var clusterStatuses = require('./clusters/clusterstatuses.json');
var createCluster = require('./clusters/createcluster.json');

var plugins = {};
plugins['rackspace'] = require('./plugins/rackspace.json');
plugins['openstack'] = require('./plugins/openstack.json');
plugins['joyent'] = require('./plugins/joyent.json');

var automators = {};
automators['shell'] = require('./automators/shell.json');
automators['chef'] = require('./automators/chef.json');

module.exports = function (nock, argv, clientAddr) {

  /**
   * Set up nock environment. Disable net connection.
   */
  nock.disableNetConnect();

  /**
   * Automators call mocks.
   */
  var automatorsResponse = [];
  for (var item in automators) {
    nock(clientAddr)
      .persist()
      .get('/v1/loom/automatortypes/' + item)
      .reply(200, automators[item]);
    automatorsResponse.push(automators[item]);
  }

  nock(clientAddr)
    .persist()
    .get('/v1/loom/automatortypes')
    .reply(200, automatorsResponse);

  /**
   * Plugins call mocks.
   */
  var pluginsResponse = [];
  for (var item in plugins) {
    nock(clientAddr)
      .persist()
      .get('/v1/loom/providertypes/' + item)
      .reply(200, plugins[item]);
    pluginsResponse.push(plugins[item]);
  }

  nock(clientAddr)
    .persist()
    .get('/v1/loom/providertypes')
    .reply(200, pluginsResponse);

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
