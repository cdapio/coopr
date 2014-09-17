var module = angular.module(PKG.name+'.services');

module.factory('myApi_provisioners', function($resource, myApiPrefix){

  return {

    Provisioner: $resource(myApiPrefix + 'provisioners/:id',
      { id: '@id' },
      {}
    )

  };

});

