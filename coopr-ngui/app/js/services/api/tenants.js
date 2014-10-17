angular.module(PKG.name+'.services').factory('myApi_tenants',
function ($resource, myApiPrefix) {

  var Tenant = $resource(myApiPrefix + 'tenants/:name',
    { name: '@name' },
    { 
      save: {
        method: 'POST',
        url: myApiPrefix + 'tenants',
        params: {name: null},
        transformRequest: function (data) {
          return angular.toJson({tenant: data});
        }
      },
      update: {
        method: 'PUT',
        transformRequest: function (data) {
          return angular.toJson({tenant: data});
        }
      }
    }
  );


  Tenant.prototype.initialize = function() {
    angular.extend(this, {
      maxNodes: 0,
      maxClusters: 0,
      workers: 1
    });
  };

  return {

    Tenant: Tenant,

    Metric: $resource(myApiPrefix + 'metrics/:type',
      { },
      {
        getTaskQueue: {
          method: 'GET',
          params: {type: 'queues'},
          transformResponse: function (data) {
            data = angular.fromJson(data);
            var tasks = { queued: 0, inProgress: 0 };
            angular.forEach(data, function (val) {
              tasks.queued += (val.queued || 0);
              tasks.inProgress += (val.inProgress || 0);
            });
            return tasks;
          }
        }

      }
    )

  };

});

