var module = angular.module(PKG.name+'.services');

module.factory('myApi_imagetypes', function($resource, MYAPI_PREFIX){

  return {

    ImageType: $resource(MYAPI_PREFIX + 'imagetypes/:name',
      { name: '@name' },
      { 
        update: {
          method: 'PUT'
        }
      }
    )

  };

});

