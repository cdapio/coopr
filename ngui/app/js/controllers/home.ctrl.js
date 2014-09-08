var module = angular.module(PKG.name+'.controllers');


module.controller('HomeCtrl', function ($scope, $filter, $modal, $alert, myAuth, myApi, myFileReader) {

  if(myAuth.isAuthenticated()) {
    refresh();
  }

  $scope.refresh = refresh;

  $scope.doImport = function() {
    myFileReader.get()
      .then(
        function (reader) {
          $modal({title:'TODO', content:reader.result, show:true});
        },
        function (err) {
          $alert({title:'import error!', content:err, type:'danger', duration:3 });
        }
      );
  };

  $scope.doExport = function () {
    $modal({title: 'TODO', content: 'Not implemented yet', show: true});
  };



  var filterFilter = $filter('filter');

  function refresh () {
    myApi.Cluster.query(function (list) {

      var active = filterFilter(list, {status:'active'});

      $scope.liveClusters = active.length;
      $scope.pendingClusters = filterFilter(list, {status:'pending'}).length;

      $scope.liveNodes = countNodes(active);
      $scope.totalNodes = countNodes(list);

      $scope.timestamp = new Date();
    });
  }

  function countNodes (list) {
    return list.reduce(function (memo, cluster) {
      return memo + cluster.numNodes;
    }, 0);
  }


});



