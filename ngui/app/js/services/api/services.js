angular.module(PKG.name+'.services').factory('myApi_services', 
function ($resource, myApiPrefix) {

  return {

    Service: $resource(myApiPrefix + 'services/:name',
      { name: '@name' },
      { 
        update: {
          method: 'PUT'
        }
      }
    )

  };

});

