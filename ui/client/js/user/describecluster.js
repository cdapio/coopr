/**
 * Copyright 2012-2014, Continuuity, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * ClusterView module, sets up namespace, depends on AngularJS.
 * @type {Object}
 */
var ClusterView = {};

/**
 * Gets service sets from cluster data. Grouping algorithm to go through all similar services in a
 * cluster and create groups of nodes belonging to those service sets.
 * @param  {Object} clusterData returned from server.
 * @return {Object} data grouped by service sets.
 */
ClusterView.getServiceSets = function (clusterData) {
  var serviceSets = {};
  for (item in clusterData.nodes) {
    var node = clusterData.nodes[item];
    var services = node.services.map(function(service) {
      return service.name;
    });
    if (services.toString() in serviceSets) {
      serviceSets[services.toString()].push(node);
    } else {
      serviceSets[services.toString()] = [node];
    }
  }
  return serviceSets;
};

/**
 * Gets services that are available in the templates but haven't been added to cluster yet.
 * @param  {Object} clusterData cluster info.
 * @return {Array} list of remaining services.
 */
ClusterView.getRemainingServices = function (clusterData) {
  var remainingServices = [];
  var services = clusterData.clusterTemplate.compatibility.services;
  for (var i = 0, len = services.length; i < len; i++) {
    if (clusterData.services.indexOf(services[i]) == -1) {
      remainingServices.push(services[i]);
    }
  }
  return remainingServices;
};

/**
 * Gets status of cluster and updates progress.
 * @param  {Object} $scope Angular Js scope.
 * @param  {Object} dataFactory Service.
 * @return {Function} Function to get status of cluster.
 */
ClusterView.getStatusFn = function ($scope, dataFactory, Globals) {
  return function () {
    dataFactory.getClusterStatus(function (data) {
      $scope.status.data = data;
      $scope.status.progressPercent = 0;
      var progressPercent = data.stepscompleted * 100 / data.stepstotal;
      if (!isNaN(progressPercent)) {
        $scope.status.progressPercent = progressPercent.toFixed(0);
      }
      if (data.actionstatus in Helpers.FRIENDLY_STATUS) {
        $scope.status.statusText = Helpers.FRIENDLY_STATUS[data.actionstatus];
        $scope.status.class= Globals.STATUS_CLASSES[data.actionstatus];
        $scope.status.action = Globals.READABLE_ACTIONS[data.action];
      }
    });
  }
};

/**
 * Reset adding service controls.
 * @param  {Object} $scope.
 */
ClusterView.resetServiceAddControls = function ($scope) {
  $scope.remainingServices = [];
  $scope.servicesToAdd = [];
  $scope.curRemainingService = '';
};

/**
 * Load cluster information.
 * @param  {Object} scope ClusterCtrl scope.
 * @param  {Object} dataFactory.
 * @param  {Object} interval.
 */
ClusterView.loadCluster = function (scope, dataFactory, interval) {
  dataFactory.getCluster(function (cluster) {
    scope.cluster = cluster;
    scope.leaseDuration.step = Helpers.parseMilliseconds(
      cluster.clusterTemplate.administration.leaseduration.step);
    scope.maxLeaseStr = Helpers.timeToStr(scope.leaseDuration.step);
    for (item in cluster.nodes) {
      scope.nodeDisplayMapping[cluster.nodes[item]] = false;
    }
    scope.cluster.creationTime = new Date(cluster.createTime).toUTCString();
    scope.cluster.expireTimeStr = new Date(cluster.expireTime).toUTCString();
    scope.serviceSets = ClusterView.getServiceSets(cluster);
    for (item in scope.serviceSets) {
      scope.serviceDisplayMapping[scope.serviceSets[item]] = false; 
    }
    scope.remainingServices = ClusterView.getRemainingServices(cluster);
    Helpers.enableTableSorting(interval);
  });
};

ClusterView.app = angular.module('clusterview', ['ngSanitize'], ['$interpolateProvider',
  function ($interpolateProvider) {
  $interpolateProvider.startSymbol('[[');
  $interpolateProvider.endSymbol(']]');
}]);

ClusterView.app.value('fetchUrl', '/pipeApiCall?path=');
ClusterView.app.value('clusterEndpoint', '/clusters/');

/**
 * Breaks new lines in html printable line breaks.
 * @return {String} HTML string containing \n new lines.
 */
ClusterView.app.filter('linebreaksbr', function () {
  return function (input) {
    if (input) {
      return Helpers.escapeHtml(input).replace(/\n/g, "<br />");  
    }
    return input;
  };
});

/**
 * Prettifies a unix timestamp.
 * @return {String} Timestamp.
 */
ClusterView.app.filter('prettifyTimestamp', function () {
  return function (timestamp) {
    return Helpers.prettifyTimestamp(timestamp);
  };
});

/**
 * Gets minutes for a given time in milliseconds.
 * @return {Number} Number of minutes.
 */
ClusterView.app.filter('stringifyTime', function () {
  return function (milliseconds) {
    if (milliseconds > 0) {
      return Helpers.stringifyTime(milliseconds);  
    }
    return '';
  }
});

ClusterView.app.factory('Globals', [function () {
  return {
    STATUS_CLASSES : {
      COMPLETE: "text-success",
      CREATING: "",
      FAILED: "text-danger",
      NOT_SUBMITTED: "",
      SOLVING_LAYOUT: ""
    },

    READABLE_ACTIONS: Helpers.READABLE_ACTIONS,

    CALL_INTERVAL: Helpers.CALL_INTERVAL
  }
}]);

ClusterView.app.factory('dataFactory', ['$http', '$q', 'fetchUrl', 'clusterEndpoint',
  function ($http, $q, fetchUrl, clusterEndpoint) {
  var clusterId = $(".cluster-id").attr('id');
  return {
    /**
     * Gets cluster id. Safe to use synchronously i.e.
     * var clusterId = dataFactory.getClusterId();
     * @return {String} Cluster id.
     */
    getClusterId: function () {
      return clusterId;
    },
    /**
     * Fetches cluster definition.
     */
    getCluster: function (callback) {    
      $http.get(fetchUrl + clusterEndpoint + clusterId).success(callback);
    },
    /**
     * Fetches cluster status.
     * @param  {Function} callback [description]
     * @return {[type]}            [description]
     */
    getClusterStatus: function (callback) {
      $http.get(fetchUrl + clusterEndpoint + clusterId + '/status').success(callback);
    }
  }
}]);
 
ClusterView.app.controller('ClusterCtrl', 
  ['$scope', '$interval', '$http', 'dataFactory', 'clusterEndpoint',
  function ($scope, $interval, $http, dataFactory, clusterEndpoint) {
  $scope.nodeDisplayMapping = {};
  $scope.serviceDisplayMapping = {};
  
  /**
   * Lease extension controls.
   */
  $scope.leaseDuration = {
    step: {
      days: null,
      hours: null,
      minutes: null
    }
  };
  $scope.maxLeaseStr = '';
  $scope.extendLeaseInvalid = false;

  /**
   * Reset services controls.
   */
  ClusterView.resetServiceAddControls($scope);

  ClusterView.loadCluster($scope, dataFactory, $interval);

  /**
   * Checks if lease extension is valid.
   */
  $scope.$watch('leaseDuration.step', function () {
    if ($scope.cluster) {
      if (Helpers.concatMilliseconds($scope.leaseDuration.step) >
        $scope.cluster.clusterTemplate.administration.leaseduration.step) {
        $scope.extendLeaseInvalid = true;
      } else {
        $scope.extendLeaseInvalid = false;
      }      
    }
  }, true);

  /**
   * Submits services to be added to cluster.
   * @param {Array} servicesToAdd (implicit).
   */
  $scope.submitServicesToAdd = function () {
    Helpers.submitPost('/user/clusters/cluster/' + $scope.cluster.id + '/services',
      { services: $scope.servicesToAdd}, '/user/clusters/cluster/' + $scope.cluster.id,
      function (resp) {
      if (resp === 'OK') {
        ClusterView.resetServiceAddControls($scope);
        ClusterView.loadCluster($scope, dataFactory, $interval);
      } else {
        var errorMessage = '';
        if (resp) {
          errorMessage = resp;
        } else {
          errorMessage = 'Request unsuccessful';
        }
        $("#notification").text(errorMessage);
        $("html, body").animate({ scrollTop: 0 }, "slow");
      }
    });
  };

  /**
   * Adds services to remaining set.
   * @param {String} curRemainingService (implicit).
   * @param {Array} servicesToAdd (implicit).
   */
  $scope.addServiceToRemaining = function () {
    $scope.servicesToAdd = Helpers.checkAndAdd(
      $scope.curRemainingService, $scope.servicesToAdd);
  };

  /**
   * Removes entry.
   * @param {String} item string to remove.
   * @param {Array} arr to remove from.
   */
  $scope.removeEntry = function (item, arr) {
    $scope.servicesToAdd = Helpers.checkAndRemove(item, arr);
  };

  /**
   * Submits extension for cluster lease.
   */
  $scope.submitExtension = function  () {
    var extensionMillis = $scope.cluster.expireTime + Helpers.concatMilliseconds(
      $scope.leaseDuration.step);
    postJson = {
      expireTime: extensionMillis
    };
    Helpers.submitPost('/user/clusters/cluster/' + $scope.cluster.id,
      postJson, document.location.href);
  };

  /**
   * Toggles logs inside node table.
   * @param  {String} nodeId id of node containing logs.
   */
  $scope.toggleLogs = function (nodeId) {
    $scope.nodeDisplayMapping[nodeId] = !$scope.nodeDisplayMapping[nodeId];
  };

  /**
   * Toggles nodes inside service set.
   * @param  {String} serviceId service set id => "hbase-master,hive-metastore,zookeeper-server".
   */
  $scope.toggleNodes = function(serviceId) {
    $scope.serviceDisplayMapping[serviceId] = !$scope.serviceDisplayMapping[serviceId];
  };

  /**
   * Deletes a cluster from view after confirming with user.
   * @param  {Object} $event contains form submit event information.
   */
  $scope.deleteCluster = function ($event) {
    $event.preventDefault();
    Helpers.handleConfirmDeletion($event, '/user/clusters');
  };

  /**
   * Abort current job in progress.
   */
  $scope.abort = function () {
    Helpers.submitPost('/user/clusters/abort/' + $scope.cluster.id,
      '/user/clusters/cluster/' + $scope.cluster.id);
  };

  /**
   * Checks if an object is empty.
   */
  $scope.checkIfEmpty = function(obj) {
    for (item in obj) {
      if (obj.hasOwnProperty(item)) {
        return true;
      }
    }
    return false;
  };

}]);

ClusterView.app.controller('ClusterProgressCtrl', ['$scope', '$interval', 'dataFactory', 'Globals',
  function ($scope, $interval, dataFactory, Globals) {
  $scope.status = {
    progressPercent: '',
    statusText: '',
    class: '',
    action: ''
  };

  // Get status first before going into interval.
  ClusterView.getStatusFn($scope, dataFactory, Globals)();

  $interval(ClusterView.getStatusFn($scope, dataFactory, Globals), Globals.CALL_INTERVAL);
}]);
