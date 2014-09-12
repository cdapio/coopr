var module = angular.module(PKG.name+'.services');

module.factory('myApi_tenants', function($resource, MYAPI_PREFIX){

  return {

    Tenant: $resource(MYAPI_PREFIX + 'tenants/:name',
      { name: '@name' },
      { 
        update: {
          method: 'PUT'
        }
      }
    )

  };

});

