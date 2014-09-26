angular.module(PKG.name+'.services').factory('myApi_importexport', 
function ($resource, myApiPrefix) {

  return {

    Import: $resource(myApiPrefix + 'import'),

    Export: $resource(myApiPrefix + 'export', 
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

