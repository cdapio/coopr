angular.module(PKG.name+'.services').factory('myApi_providerTypes', 
function ($resource, myApiPrefix) {

  return {

    ProviderType: $resource(myApiPrefix + 'plugins/providertypes/:name', {})

  };

});

