/**
 * [TODO] Add some description.
 */

var module = angular.module(PKG.name+'.services');

module.factory('myApi_clusters', function($resource, MYAPI_PREFIX){

  return {

    Cluster: $resource(MYAPI_PREFIX + 'clusters/:id',
      { id: '@id' },
      { 
        getStatus: {
          method: 'GET',
          url: MYAPI_PREFIX + 'clusters/:id/status'
        },
        startAllServices: {
          method: 'POST',
          url: MYAPI_PREFIX + 'clusters/:id/services/start'
        },
        stopAllServices: {
          method: 'POST',
          url: MYAPI_PREFIX + 'clusters/:id/services/stop'
        },
        restartAllServices: {
          method: 'POST',
          url: MYAPI_PREFIX + 'clusters/:id/services/restart'
        },
        syncTemplate: {
          method: 'POST',
          url: MYAPI_PREFIX + 'clusters/:id/clustertemplate/sync'
        }
      }
    ),

    ClusterConfig: $resource(MYAPI_PREFIX + 'clusters/:clusterId/config',
      {},
      {
        update: {
          method: 'PUT'
        }
      }
    ),

    ClusterActionPlan: $resource(MYAPI_PREFIX + 'clusters/:clusterId/plans/:id',
      { id: '@id' }
    ),

    ClusterService: $resource(MYAPI_PREFIX + 'clusters/:clusterId/services/:name',
      { name: '@name' },
      { 
        start: {
          method: 'POST',
          url: MYAPI_PREFIX + 'clusters/:clusterId/services/:name/start'
        },
        stop: {
          method: 'POST',
          url: MYAPI_PREFIX + 'clusters/:clusterId/services/:name/stop'
        },
        restart: {
          method: 'POST',
          url: MYAPI_PREFIX + 'clusters/:clusterId/services/:name/restart'
        }
      }
    )

  };

});

