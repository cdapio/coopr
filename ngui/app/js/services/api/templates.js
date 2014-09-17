var module = angular.module(PKG.name+'.services');

module.factory('myApi_templates', function($resource, myApiPrefix){

  return {
    Template: $resource(myApiPrefix + 'clustertemplates/:name',
      { name: '@name' },
      { 
        update: {
          method: 'PUT'
        }
      }
    )
  };

});

