/**
 * [TODO] Add some description.
 */

var module = angular.module(PKG.name+'.services');

module.factory('myApi_templates', function($resource, MYAPI_PREFIX){

  return {
    Template: $resource(MYAPI_PREFIX + 'clustertemplates/:name',
      { name: '@name' },
      { 
        update: {
          method: 'PUT'
        }
      }
    )
  };

});

