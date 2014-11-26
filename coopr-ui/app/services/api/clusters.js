angular.module(PKG.name+'.services').factory('myApi_clusters',
function ($resource, myApiPrefix) {

  return {

    Cluster: $resource(myApiPrefix + 'clusters/:id',
      { id: '@id' },
      {
        query: {
          method: 'GET',
          isArray: true,
          params: { status: 'pending,active,incomplete,inconsistent' }
        },
        getStatus: {
          method: 'GET',
          url: myApiPrefix + 'clusters/:id/status'
        },
        pauseDeploy: {
          method: 'POST',
          url: myApiPrefix + 'clusters/:id/pause'
        },
        resumeDeploy: {
          method: 'POST',
          url: myApiPrefix + 'clusters/:id/resume'
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

