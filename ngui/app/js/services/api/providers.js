angular.module(PKG.name+'.services').factory('myApi_providers', 
function ($resource, myApiPrefix) {

  return {

    Provider: $resource(myApiPrefix + 'providers/:name',
      { name: '@name' },
      { 
        update: {
          method: 'PUT'
        }
      }
    )

  };

});

