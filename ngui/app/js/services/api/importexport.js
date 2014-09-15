/**
 * [TODO] Add some description.
 */

var module = angular.module(PKG.name+'.services');

module.factory('myApi_importexport', function($resource, MYAPI_PREFIX){

  return {

    Import: $resource(MYAPI_PREFIX + 'import'),

    Export: $resource(MYAPI_PREFIX + 'export', 
      { }, 
      {
        query: {
          method:'GET', 
          isArray: false
        },
      }
    )

  };

});

