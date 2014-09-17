var module = angular.module(PKG.name+'.controllers');


module.controller('ClusterListCtrl', function ($scope, $filter, $timeout, moment, myApi, CrudListBase) {
  CrudListBase.apply($scope);

  var timeoutPromise,
      filterFilter = $filter('filter'),
      tenMinutesAgo = moment().minutes(-10);

  $scope.isActive = function (item) { 
    // any cluster created recently is considered "active" for display purposes
    return (moment(item.createTime)>tenMinutesAgo) || ['terminated','incomplete'].indexOf(item.status)===-1;
  };

  $scope.$watchCollection('list', function (list) {
    if (list.length) {

      var activeCount = filterFilter(list, $scope.isActive).length,
          filteredCount = list.length - activeCount;

      // show the toggle only if there are both visible and filterable items.
      $scope.togglerVisible = (activeCount && filteredCount);

      if(!activeCount) { // if there are no active items, don't filter.
        $scope.filterIsOff = true;
      }

      updatePending();
    }
  });


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
      1000);
    }
  }

  $scope.$on('$destroy', function () {
    $timeout.cancel(timeoutPromise);
  });

});



