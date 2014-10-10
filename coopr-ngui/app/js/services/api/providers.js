angular.module(PKG.name+'.services').factory('myApi_providers', 
function ($resource, myApiPrefix) {


  var Provider = $resource(myApiPrefix + 'providers/:name',
    { name: '@name' },
    { 
      update: {
        method: 'PUT'
      }
    }
  );

  Provider.prototype.initialize = function() {
    angular.extend(this, {
      provisioner: {}
    });
  };


  return {

    Provider: Provider,

    ProviderType: $resource(myApiPrefix + 'plugins/providertypes/:type')

  };

});

