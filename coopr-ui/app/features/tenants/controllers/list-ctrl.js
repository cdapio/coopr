/**
 * TenantListCtrl
 */

angular.module(PKG.name+'.features').controller('TenantListCtrl',
function ($scope, $interval, $q, myApi, CrudListBase) {
  CrudListBase.apply($scope);

  var promise = $interval(updateTicker, 5000);
  updateTicker();

  $scope.$on('$destroy', function () {
    $interval.cancel(promise);
  });

  /* ----------------------------------------------------------------------- */

  function updateTicker() {
    return $q.all({
      tasks: myApi.Metric.getTaskQueue().$promise,
      workers: myApi.Provisioner.getWorkerCapacity().$promise
    })
    .then(function (result) {

      $scope.tasksQueued = result.tasks.queued;
      $scope.tasksInProgress = result.tasks.inProgress;

      $scope.workersTotal = result.workers.total;
      $scope.workersAvailable = result.workers.free;

    });
  }

});