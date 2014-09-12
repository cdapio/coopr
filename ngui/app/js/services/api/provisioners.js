var module = angular.module(PKG.name+'.services');

module.factory('myApi_provisioners', function($resource, MYAPI_PREFIX){

  return {

    Provisioner: $resource(MYAPI_PREFIX + 'provisioners/:id',
      { id: '@id' },
      {}
    )

  };

});

