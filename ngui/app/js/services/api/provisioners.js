angular.module(PKG.name+'.services').factory('myApi_provisioners', 
function ($resource, myApiPrefix) {

  return {

    Provisioner: $resource(myApiPrefix + 'provisioners/:id',
      { id: '@id' },
      {
        getWorkerCapacity: {
          method: 'GET',
          url: myApiPrefix + 'provisioners',
          transformResponse: function (data) {
            data = angular.fromJson(data);
            return {
              total: data.reduce(function(memo, provisioner) {
                       return memo + provisioner.capacityTotal;
                     }, 0),

              free: data.reduce(function(memo, provisioner) {
                      return memo + provisioner.capacityFree;
                    }, 0)
            };
          }
        }
      }
    )

  };

});




