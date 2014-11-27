angular.module(PKG.name+'.features').controller('ImageFormCtrl',
function ($scope, $state, myApi, CrudFormBase, caskFocusManager) {
  CrudFormBase.apply($scope);

  $scope.allProviders = myApi.Provider.query();
  $scope.textFields = [{
    name: 'image',
    placeholder: 'Image'
  }, {
    name: 'sshuser',
    placeholder: 'SSH user'
  }];
  if($scope.editing) {
    $scope.model = myApi.ImageType.get($state.params);
    $scope.model.$promise['catch'](function () { $state.go('404'); });
  }
  else { // creating
    $scope.model = new myApi.ImageType();
    $scope.model.initialize();
    caskFocusManager.focus('inputImageName');
  }
});
