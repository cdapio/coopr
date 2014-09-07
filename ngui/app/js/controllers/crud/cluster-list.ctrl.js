var module = angular.module(PKG.name+'.controllers');


module.controller('ClusterListCtrl', function ($scope, $filter, $timeout, myApi, CrudListBase) {
  CrudListBase.apply($scope);

  var filterFilter = $filter('filter'),
      notTerminated = {status:'!terminated'};

  $scope.clusterFilter = notTerminated;

  $scope.$watchCollection('list', function (list) {
    if (!list.$promise || list.$resolved) {
      if(filterFilter(list, notTerminated).length == 0) {
        // there are no active clusters, so we dont need to filter nor show button
        $scope.clusterFilter = null;
      }
      else updatePending();
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
              if(update) {
                console.log('updatePending', cluster.progress);
                cluster.status = update[0].status;
                cluster.progress = update[0].progress;
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



