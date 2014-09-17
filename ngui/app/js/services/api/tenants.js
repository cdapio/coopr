var module = angular.module(PKG.name+'.services');

module.factory('myApi_tenants', function($resource, myApiPrefix){

  return {

    Tenant: $resource(myApiPrefix + 'tenants/:name',
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
    ),

    Metric: $resource(myApiPrefix + 'metrics/:type')

  };

});

