/**
 * ClusterListCtrl
 */

angular.module(PKG.name+'.feature.clusters').controller('ClusterListCtrl',
function ($stateParams, $scope, $filter, $timeout, moment, myApi, CrudListBase) {

  CrudListBase.apply($scope);

  var timeoutPromise,
      filterFilter = $filter('filter');

  if($stateParams.status === 'terminated') {
    myApi.Cluster.query($stateParams, function (list) {
      $scope.list = list;
    });
  }
  else { // we can re-use the list that comes from SubnavCtrl
    $scope.$watchCollection('list', updatePending);
  }

  $scope.$on('$destroy', function () {
    $timeout.cancel(timeoutPromise);
  });

  /* ----------------------------------------------------------------------- */

  function updatePending () {
    if(filterFilter($scope.list, {status:'pending'}).length) {
      timeoutPromise = $timeout(function () {

        myApi.Cluster.query(function (list) {
          // $scope.list = list works, but then we lose the animation of progress bars
          // instead we only modify the properties that interest us
          angular.forEach($scope.list, function (cluster) {
            if(cluster.status === 'pending') {
              var update = filterFilter(list, {id:cluster.id});
              if(update && update.length) {
                cluster.status = update[0].status;
                cluster.progress = update[0].progress;
              }
            }
          });

          updatePending();
        });

      },
      10000);
    }
  }

});