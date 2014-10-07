angular.module(PKG.name+'.controllers').controller('ServiceFormCtrl',
function ($scope, $state, myApi, CrudFormBase) {
  CrudFormBase.apply($scope);

  if($scope.editing) {
    $scope.model = myApi.Service.get($state.params);
    $scope.model.$promise['catch'](function () { $state.go('404'); });
  }
  else { // creating
    $scope.model = new myApi.Service();
    angular.extend($scope.model, {
      provisioner: {},
      dependencies: {}
    });
  }



});
