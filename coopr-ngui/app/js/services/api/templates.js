angular.module(PKG.name+'.services').factory('myApi_templates',
function ($resource, myApiPrefix) {

  var Template = $resource(myApiPrefix + 'clustertemplates/:name',
    { name: '@name' },
    { 
      update: {
        method: 'PUT'
      }
    }
  );

  Template.prototype.initialize = function() {
    angular.extend(this, {
      compatibility: {
        services: ['base'],
        imagetypes: [],
        hardwaretypes: []
      },
      defaults: {
        config: {},
        services: ['base']
      },
      constraints: {
        layout: {
          mustcoexist: [],
          cantcoexist: []
        },
        services: {}
      },
      administration: {
        leaseduration:{}
      }
    });
  };

  return {
    Template: Template
  };

});

