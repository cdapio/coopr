angular.module(PKG.name+'.services').factory('myApi_services', 
function ($resource, myApiPrefix) {


  var Service = $resource(myApiPrefix + 'services/:name',
    { name: '@name' },
    { 
      update: {
        method: 'PUT'
      },
      save: {
        method: 'POST',
        url: myApiPrefix + 'services',
        params: {name: null}
      }      
    }
  );

  Service.prototype.initialize = function() {
    angular.extend(this, {
      dependencies: {
        provides: [],
        conflicts: [],
        install: {
          requires: [],
          uses: []
        },
        runtime: {
          requires: [],
          uses: []
        }
      },
      provisioner: {
        actions: {}
      }
    });
  };

  return {
    Service: Service,

    AutomatorType: $resource(myApiPrefix + 'plugins/automatortypes/:name',
      { name: '@name' }
    )
  };

});

