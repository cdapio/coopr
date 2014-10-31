var module = angular.module(PKG.name+'.feature.crud');

/**
 * CrudListCtrl
 * generic list controller
 */
module.controller('CrudListCtrl', function ($scope, CrudListBase) {
  CrudListBase.apply($scope);
});


/**
 * CrudEditCtrl
 * a controller to edit an existing model
 */
module.controller('CrudEditCtrl', function ($scope, $state, myApi, CrudFormBase) {
  CrudFormBase.apply($scope);

  var data = $state.current.data,
      failure = function () { $state.go('404'); };

  if(data) {
    $scope.model = myApi[data.modelName].get($state.params);
    $scope.model.$promise['catch'](failure);
  }
  else {
    failure();
  }
});


/**
 * CrudCreateCtrl
 * a controller to create a new model
 */
module.controller('CrudCreateCtrl', function ($scope, $state, myApi, CrudFormBase) {
  CrudFormBase.apply($scope);

  var data = $state.current.data;
  if(data) {
    $scope.model = new myApi[data.modelName]();
  }
});
