var module = angular.module(PKG.name+'.controllers');


module.controller('ClusterDetailCtrl', function ($scope, CrudFormBase, $state, myApi, $interval) {
  CrudFormBase.apply($scope);

  var failure = function () { $state.go('404'); };

  if($state.params.id) {
    $scope.model = myApi.Cluster.get( {id:$state.params.id}, function (data) {
      console.log('editing cluster', data);
    });

    $scope.model.$promise.catch(failure);
  }
  else {
    failure();
  }

});


