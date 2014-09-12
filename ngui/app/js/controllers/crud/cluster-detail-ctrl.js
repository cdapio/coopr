/**
 * Cluster detail controller. [TODO]
 */

angular.module(PKG.name+'.controllers').controller('ClusterDetailCtrl', 
  function ($scope, CrudFormBase, $state, myApi, $timeout) {
    /**
     * TODO: why do this?
     */
    CrudFormBase.apply($scope);

    if($state.params.id) {
      $scope.model = myApi.Cluster.get( {id:$state.params.id});
      $scope.model.$promise.catch(failure);
    }
    else {
      failure();
    }

    var timeoutPromise;

    $scope.$watchCollection('model', function (data) {
      if(!data.$resolved) { return; }

      console.log(data);

      $scope.availableServices = data.clusterTemplate.compatibility.services.filter(function(name) {
        return data.services.indexOf(name)===-1; // filter out services that are already installed
      }).map(function(name) { return { name: name }; }); // mimic myApi.Service.query()

      $scope.additionalServices = [];

      if(data.status === 'pending') {
        timeoutPromise = $timeout(update, 1000);
      }


      $scope.serviceSets = data.nodes.reduce(function (memo, node) {
        var services = node.services.map( function (one) {
          return one.name;
        }).sort(),
            svcStr = services.join('_');

        if(!memo[svcStr]) {
          memo[svcStr] = {
            services: services,
            nodes: []
          };
        }
        memo[svcStr].nodes.push(node);

        return memo;
      }, {});
    });

    $scope.$on('$destroy', function () {
      $timeout.cancel(timeoutPromise);
    });

    $scope.doSubmitServices = function (arrSvcs) {
      myApi.ClusterService.save( {clusterId: $scope.model.id}, { services: arrSvcs }, update);
    };

    function failure () {
      $state.go('404');
    }

    function update () {
      $scope.model.$get();
    }

  }
);


