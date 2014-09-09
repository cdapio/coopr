var module = angular.module(PKG.name+'.controllers');


module.controller('ClusterListCtrl', function ($scope, $filter, $timeout, myApi, CrudListBase) {
  CrudListBase.apply($scope);

  var filterFilter = $filter('filter');

  $scope.clusterFilter = null;

  $scope.$watchCollection('list', function (list) {
    if (!list.$promise || list.$resolved) {

      if (list.length) {

        var terminatedCount = filterFilter(list, { status: 'terminated' }).length;
        if(terminatedCount && terminatedCount!==list.length) {
          // we have both active inactive clusters, so filter and show button
          $scope.clusterFilter = { status: '!terminated' };
        }

        updatePending();
      }
    }
  });

  function updatePending() {
    if(filterFilter($scope.list, {status:'pending'}).length) {
      $timeout(function () {

        myApi.Cluster.query(function (list) {
          // $scope.list = list works, but then we lose the animation of progress bars
          // instead we only modify the properties that interest us
          angular.forEach($scope.list, function (cluster) {
            if(cluster.status === 'pending') {
              var update = filterFilter(list, {id:cluster.id});
              if(update && update.length) {
                cluster.status = update[0].status;
                cluster.progress = update[0].progress;
                console.log('[updatePending]', cluster.progress);
              }
            }
          });
          updatePending();
        });

      },
      1000);
    }
  }

});



