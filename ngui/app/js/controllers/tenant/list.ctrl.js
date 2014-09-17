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
      queues: myApi.Metric.get({type:'queues'}).$promise,
      provisioners: myApi.Provisioner.query().$promise
    })
    .then(function (result) {
      var tasksQueued = 0,
          tasksInProgress = 0;

      angular.forEach(result.queues, function (val, key) {
        if(key.substr(0,1)!=='$') {
          tasksQueued += val.queued;
          tasksInProgress += val.inProgress;          
        }
      });

      $scope.tasksQueued = tasksQueued;

      $scope.tasksInProgress = tasksInProgress;

      $scope.workersTotal = result.provisioners.reduce(function(memo, provisioner) {
        return memo + provisioner.capacityTotal;
      }, 0);

      $scope.workersAvailable = result.provisioners.reduce(function(memo, provisioner) {
        return memo + provisioner.capacityFree;
      }, 0);

      timeoutPromise = $timeout(updateTicker, 5000);
    });
  }

});