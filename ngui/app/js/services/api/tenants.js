var module = angular.module(PKG.name+'.services');

module.factory('myApi_tenants', function($resource, myApiPrefix){

  return {

    Tenant: $resource(myApiPrefix + 'tenants/:name',
      { name: '@name' },
      { 
        update: {
          method: 'PUT'
        }
      }
    )

  };

});

