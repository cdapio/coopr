angular.module(PKG.name+'.services').factory('myApi_services', 
function ($resource, myApiPrefix) {


  var Service = $resource(myApiPrefix + 'services/:name',
    { name: '@name' },
    { 
      update: {
        method: 'PUT'
      }
    }
  );

  Service.prototype.initialize = function() {
    angular.extend(this, {
      provisioner: {},
      dependencies: {}
    });
  };

  return {
    Service: Service
  };

});

