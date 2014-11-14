angular.module(PKG.name+'.features').controller('HardwareFormCtrl',
function ($scope, $state, myApi, CrudFormBase, caskFocusManager) {
  CrudFormBase.apply($scope);

  $scope.allProviders = myApi.Provider.query();
  $scope.textFields = [{
    name: 'flavor',
    placeholder: 'Flavor'
  }];
  if($scope.editing) {
    $scope.model = myApi.HardwareType.get($state.params);
    $scope.model.$promise['catch'](function () { $state.go('404'); });
  }
  else { // creating
    $scope.model = new myApi.HardwareType();
    $scope.model.initialize();
    caskFocusManager.focus('inputHardwareName');
  }
});
