angular.module(PKG.name+'.services').factory('myApi_providers', 
function ($resource, myApiPrefix) {

  return {

    Provider: $resource(myApiPrefix + 'providers/:name',
      { name: '@name' },
      { 
        update: {
          method: 'PUT'
        },
        save: {
          method: 'POST',
          url: myApiPrefix + '/providers'
        }
      }
    ),

    ProviderType: $resource(myApiPrefix + 'plugins/providertypes/:type')

  };

});

