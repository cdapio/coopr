/**
 * TenantFormCtrl
 * handles both "edit" and "create" views
 */

angular.module(PKG.name+'.features').controller('TenantFormCtrl',
function ($scope, $state, $alert, $q, myApi, CrudFormBase, caskFocusManager) {
  CrudFormBase.apply($scope);

  var promise;
  if($scope.editing) {
    $scope.model = myApi.Tenant.get($state.params);
    promise = $scope.model.$promise;
  }
  else {
    $scope.model = new myApi.Tenant();
    $scope.model.initialize();

    promise = $q.when($scope.model);

    caskFocusManager.focus('inputTenantName');
  }

  $q.all({
    workers: myApi.Provisioner.getWorkerCapacity().$promise,
    model: promise
  })
  .then(function (result){

    $scope.maxWorkers = result.workers.free + (result.model.workers||0);

    if($scope.maxWorkers <= 0) {
      $scope.maxWorkers = 0;
      $scope.model.workers = 0;
      $alert({
        title: 'No workers available!',
        content: 'You will need to free some workers for this tenant.',
        type: 'warning'
      });
    }

  })
  ['catch'](function () { $state.go('404'); });

});
