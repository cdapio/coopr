var module = angular.module(PKG.name+'.services');

module.factory('myApi_clusters', function($resource, myApiPrefix){

  return {

    Cluster: $resource(myApiPrefix + 'clusters/:id',
      { id: '@id' },
      { 
        getStatus: {
          method: 'GET',
          url: myApiPrefix + 'clusters/:id/status'
        },
        startAllServices: {
          method: 'POST',
          url: myApiPrefix + 'clusters/:id/services/start'
        },
        stopAllServices: {
          method: 'POST',
          url: myApiPrefix + 'clusters/:id/services/stop'
        },
        restartAllServices: {
          method: 'POST',
          url: myApiPrefix + 'clusters/:id/services/restart'
        },
        syncTemplate: {
          method: 'POST',
          url: myApiPrefix + 'clusters/:id/clustertemplate/sync'
        }
      }
    ),

    ClusterConfig: $resource(myApiPrefix + 'clusters/:clusterId/config',
      {},
      {
        update: {
          method: 'PUT'
        }
      }
    ),

    ClusterActionPlan: $resource(myApiPrefix + 'clusters/:clusterId/plans/:id',
      { id: '@id' }
    ),

    ClusterService: $resource(myApiPrefix + 'clusters/:clusterId/services/:name',
      { name: '@name' },
      { 
        start: {
          method: 'POST',
          url: myApiPrefix + 'clusters/:clusterId/services/:name/start'
        },
        stop: {
          method: 'POST',
          url: myApiPrefix + 'clusters/:clusterId/services/:name/stop'
        },
        restart: {
          method: 'POST',
          url: myApiPrefix + 'clusters/:clusterId/services/:name/restart'
        }
      }
    )

  };

});

