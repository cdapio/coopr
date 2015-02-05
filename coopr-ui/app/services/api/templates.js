angular.module(PKG.name+'.services').factory('myApi_templates',
function ($resource, myApiPrefix) {

  var Template = $resource(myApiPrefix + 'clustertemplates/:name',
    { name: '@name' },
    { 
      update: {
        method: 'PUT'
      },
      save: {
        method: 'POST',
        url: myApiPrefix + 'clustertemplates',
        params: {name: null}
      }
    }
  );

  Template.prototype.initialize = function() {
    angular.extend(this, {
      compatibility: {
        services: [],
        imagetypes: [],
        hardwaretypes: []
      },
      defaults: {
        config: {},
        services: []
      },
      constraints: {
        layout: {
          mustcoexist: [],
          cantcoexist: []
        },
        services: {},
        size: {
          min: 1,
          max: 2147483647
        }
      },
      administration: {
        leaseduration: {}
      }
    });
  };

  return {
    Template: Template
  };

});

