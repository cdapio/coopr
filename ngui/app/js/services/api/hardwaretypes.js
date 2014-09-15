/**
 * [TODO] Add some description.
 */

var module = angular.module(PKG.name+'.services');

module.factory('myApi_hardwaretypes', function($resource, MYAPI_PREFIX){

  return {

    HardwareType: $resource(MYAPI_PREFIX + 'hardwaretypes/:name',
      { name: '@name' },
      { 
        update: {
          method: 'PUT'
        }
      }
    )

  };

});

