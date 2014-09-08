var module = angular.module(PKG.name+'.controllers');


module.controller('HomeCtrl', function ($scope, $filter, myAuth, myApi) {

  var filterFilter = $filter('filter'),
      countNodes = function (list) {
        return list.reduce(function (memo, cluster) {
          return memo + cluster.numNodes;
        }, 0);
      };

  if(myAuth.isAuthenticated()) {

    myApi.Cluster.query(function (list) {

      var active = filterFilter(list, {status:'active'});

      $scope.liveClusters = active.length;
      $scope.pendingClusters = filterFilter(list, {status:'pending'}).length;

      $scope.liveNodes = countNodes(active);
      $scope.totalNodes = countNodes(list);

    });

  }

});



