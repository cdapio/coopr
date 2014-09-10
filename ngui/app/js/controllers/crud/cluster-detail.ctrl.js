var module = angular.module(PKG.name+'.controllers');


module.controller('ClusterDetailCtrl', function ($scope, CrudFormBase, $state, myApi, $timeout) {
  CrudFormBase.apply($scope);

  if($state.params.id) {
    $scope.model = myApi.Cluster.get( {id:$state.params.id});
    $scope.model.$promise.catch(failure);
  }
  else {
    failure();
  }

  function failure () {
    $state.go('404');
  };

  function update () {
    $scope.model.$get();
  };

  $scope.$watchCollection('model', function (data) {
    if(!data.$resolved) { return; }

    $scope.availableServices = data.clusterTemplate.compatibility.services.filter(function(svc) {
      return data.services.indexOf(svc)===-1; // filter out services that are already installed
    });

    $scope.additionalServices = [];

    if(data.status === 'pending') {
      $timeout(update, 1000);
    }
  });

  $scope.doSubmitServices = function (arrSvcs) {
    myApi.ClusterService.save( {clusterId: $scope.model.id}, { services: arrSvcs }, update);
  };

});


