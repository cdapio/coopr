var module = angular.module(PKG.name+'.services');

module.factory('myApi_tenants', function($resource, MYAPI_PREFIX){

  return {

    Tenant: $resource(MYAPI_PREFIX + 'tenants/:id',
      { id: '@id' },
      { 
        update: {
          method: 'PUT'
        }
      }
    )

  };

});

