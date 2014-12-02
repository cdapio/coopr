angular.module(PKG.name+'.services').factory('myApi_imagetypes', 
function ($resource, myApiPrefix) {

  var ImageType = $resource(myApiPrefix + 'imagetypes/:name',
    { name: '@name' },
    { 
      update: {
        method: 'PUT'
      },
      save: {
        method: 'POST',
        url: myApiPrefix + 'imagetypes',
        params: {name: null}
      }
    }
  );

  ImageType.prototype.initialize = function() {
    angular.extend(this, {
      providermap: {}
    });
  };

  return {

    ImageType: ImageType

  };

});

