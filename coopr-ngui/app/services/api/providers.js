angular.module(PKG.name+'.services').factory('myApi_providers', 
function ($resource, myApiPrefix) {

  var Provider = $resource(myApiPrefix + 'providers/:name',
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
  );

  Provider.prototype.initialize = function() {
    angular.extend(this, {
      provisioner: {}
    });
  };


  return {

    Provider: Provider,

    ProviderType: $resource(myApiPrefix + 'plugins/providertypes/:name',
      { name: '@name' }
    )

  };

});

