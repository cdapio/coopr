/**
 * [TODO] Add some description.
 */

var module = angular.module(PKG.name+'.services');

module.factory('myApi_providers', function($resource, MYAPI_PREFIX){

  return {

    Provider: $resource(MYAPI_PREFIX + 'providers/:name',
      { name: '@name' },
      { 
        update: {
          method: 'PUT'
        }
      }
    ),

    ProviderType: $resource(MYAPI_PREFIX + 'plugins/providertypes/:type')

  };

});

