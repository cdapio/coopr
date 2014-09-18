/**
 * TemplateFormCtrl
 * handles both "edit" and "create" views
 */

angular.module(PKG.name+'.controllers').controller('TemplateFormCtrl', 
function ($scope, $state, $alert, $q, myApi, CrudFormBase) {
  CrudFormBase.apply($scope);

  if($scope.editing) {
    $scope.model = myApi.Template.get($state.params);
    $scope.model.$promise['catch'](function () { $state.go('404'); });
  }
  else {
    $scope.model = new myApi.Template();
  }


  
});