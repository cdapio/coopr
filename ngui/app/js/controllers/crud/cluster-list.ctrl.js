var module = angular.module(PKG.name+'.controllers');


module.controller('ClusterListCtrl', function ($scope, $filter, $timeout, moment, myApi, CrudListBase) {
  CrudListBase.apply($scope);

  var timeoutPromise,
      filterFilter = $filter('filter'),
      oneHourAgo = moment().hours(-1),
      notTerminatedPredicate = function (item) { 
        return item.status!=='terminated' || moment(item.createTime)<oneHourAgo;
      };

  $scope.clusterFilter = null;

  $scope.$watchCollection('list', function (list) {
    if (!list.$promise || list.$resolved) {

      if (list.length) {
        setPredicate();
        updatePending();
      }
    }
  });

  function setPredicate () {
    var notTerminatedCount = filterFilter($scope.list, notTerminatedPredicate).length,
        terminatedCount = $scope.list.length - notTerminatedCount;
    if(terminatedCount && notTerminatedCount) {
      $scope.clusterPredicate = notTerminatedPredicate; // enables the filtering
    }
    else {
      $scope.clusterPredicate = null; // hide the button and do not filter
    }
  }

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
          setPredicate();
          updatePending();
        });

      },
      1000);
    }
  }

  $scope.$on('$destroy', function () {
    $timeout.cancel(timeoutPromise);
  });

});



