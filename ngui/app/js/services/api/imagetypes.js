var module = angular.module(PKG.name+'.services');

module.factory('myApi_imagetypes', function($resource, myApiPrefix){

  return {

    ImageType: $resource(myApiPrefix + 'imagetypes/:name',
      { },
      { 
        update: {
          method: 'PUT'
        }
      }
    )

  };

});

