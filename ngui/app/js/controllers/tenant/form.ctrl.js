angular.module(PKG.name+'.controllers').controller('TenantFormCtrl', 
function ($scope, $state, $alert, $q, myApi, CrudFormBase) {
  CrudFormBase.apply($scope);

  var promise;

  if($state.includes('*.create')) {
    $scope.model = new myApi.Tenant();
    promise = $q.when($scope.model);
    $scope.model.maxClusters = 0;
    $scope.model.maxNodes = 0;
  }
  else {
    $scope.editing = true;
    $scope.model = myApi.Tenant.get($state.params);
    promise = $scope.model.$promise;
  }

  promise['catch'](function () { $state.go('404'); });

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
        type: 'warning', 
        duration: 3
      });
    }

  });

});