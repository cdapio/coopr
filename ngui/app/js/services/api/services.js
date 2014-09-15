/**
 * [TODO] Add some description.
 */

var module = angular.module(PKG.name+'.services');

module.factory('myApi_services', function($resource, MYAPI_PREFIX){

  return {

    Service: $resource(MYAPI_PREFIX + 'services/:name',
      { name: '@name' },
      { 
        update: {
          method: 'PUT'
        }
      }
    )

  };

});

