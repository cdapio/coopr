angular.module(PKG.name+'.controllers').controller('TenantListCtrl', 
function ($scope, $timeout, $q, myApi, CrudListBase) {
  CrudListBase.apply($scope);

  var timeoutPromise;

  $scope.$on('$destroy', function () {
    $timeout.cancel(timeoutPromise);
  });

  updateTicker();

  function updateTicker() {
    $q.all({
      tasks: myApi.Metric.getTaskQueue().$promise,
      workers: myApi.Provisioner.getWorkerCapacity().$promise
    })
    .then(function (result) {

      $scope.tasksQueued = result.tasks.queued;
      $scope.tasksInProgress = result.tasks.inProgress;

      $scope.workersTotal = result.workers.total;
      $scope.workersAvailable = result.workers.free;

      timeoutPromise = $timeout(updateTicker, 5000);
    });
  }

});