(function() {

var app = angular.module('coopr', [], ['$interpolateProvider', function ($interpolateProvider) {
  $interpolateProvider.startSymbol('[[');
  $interpolateProvider.endSymbol(']]');
}]);



app.factory('dataFactory', ['$http', function ($http) {
  var fetchUrl = '/pipeApiCall?path=';

  return {
    getTenants: function (callback) {
      $http.get(fetchUrl + '/tenants').success(callback);
    },
    getQueues: function (callback) {
      $http.get(fetchUrl + '/metrics/queues ').success(callback);
    },
    getProvisioners: function (callback) {
      $http.get(fetchUrl + '/provisioners ').success(callback);
    }
  };
}]);




app.controller('TenantCreateCtrl', ['$scope', function ($scope) {

  $scope.tenant = {
    workers: 3
  };

  $scope.submitForm = function (event) {
    event.preventDefault();

    var postJson = {tenant: $scope.tenant};
    if($scope.bootstrap) {
      postJson.bootstrap = true;
    }

    Helpers.submitPost(event, postJson, '/tenants');
  };

}]);




app.controller('TenantListCtrl', ['$scope','$interval','dataFactory', function ($scope, $interval, dataFactory) {

  dataFactory.getQueues(function(result) {

    var tasksQueued = 0,
        tasksInProgress = 0;

    angular.forEach(result, function (tenant, tenantId) {
      tasksQueued += tenant.queued;
      tasksInProgress += tenant.inProgress;
    });

    $scope.tasksQueued = tasksQueued;
    $scope.tasksInProgress = tasksInProgress;

  });


  dataFactory.getProvisioners(function(result) {

    $scope.totalWorkers = result.reduce(function(memo, provisioner) {
      return memo + provisioner.capacityTotal;
    }, 0);

    $scope.availableWorkers = result.reduce(function(memo, provisioner) {
      return memo + provisioner.capacityFree;
    }, 0);

  });

}]);


})();
