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
 * CreateCluster module, sets up namespace, depends on AngularJS.
 * @type {Object}
 */
var CreateCluster = {};

CreateCluster.app = angular.module('createcluster', ['ngSanitize'], ['$interpolateProvider',
  function ($interpolateProvider) {
  $interpolateProvider.startSymbol('[[');
  $interpolateProvider.endSymbol(']]');
}]);

CreateCluster.app.value('fetchUrl', '/pipeApiCall?path=');

CreateCluster.app.factory('dataFactory', ['$http', '$q', 'fetchUrl',
  function ($http, $q, fetchUrl) {
    return {
      getClusterTemplate: function (templateId, callback) {
        $http.get(fetchUrl + '/clustertemplates/' + templateId).success(callback);
      },
      getProviders: function (callback) {
        $http.get(fetchUrl + '/providers').success(callback);
      }
    };
}]);
 
CreateCluster.app.controller('CreateClusterCtrl', ['$scope', '$interval', 'dataFactory',
  function ($scope, $interval, dataFactory) {

  $scope.showAdvanced = false;
  $scope.allowedProviders = [];
  $scope.allowedHardwareTypes = [];
  $scope.allowedImageTypes = [];
  $scope.allowedServices = [];

  $scope.leaseDuration = {
    initial: {},
    max: {},
    step: {}
  };
  $scope.notification = '';


  dataFactory.getProviders(function (providers) {
    $scope.allowedProviders = providers.map(function (provider) {
      return provider.name;
    });
  });
  
  $scope.$watch('clusterTemplateId', function () {
    if ($scope.clusterTemplateId) {
      dataFactory.getClusterTemplate($scope.clusterTemplateId, function (template) {
        $scope.template = template;
        $scope.allowedHardwareTypes = template.compatibility.hardwaretypes;
        $scope.allowedImageTypes = template.compatibility.imagetypes;
        $scope.allowedServices = template.compatibility.services;
        $scope.selectedServices = template.defaults.services;
        $scope.defaultProvider = template.defaults.provider;
        $scope.defaultHardwareType = template.defaults.hardwaretype;
        $scope.defaultImageType = template.defaults.imagetype;
        $scope.defaultConfig = JSON.stringify(template.defaults.config);
        if ('administration' in template) {
          $scope.leaseDuration.initial = Helpers.parseMilliseconds(
            template.administration.leaseduration.initial);
        }
      });
    }
  });

  $scope.toggleAdvanced = function () {
    $scope.showAdvanced = !$scope.showAdvanced;
  };

  $scope.addEntry = function (name, arr) {
    Helpers.checkAndAdd(name, arr);
  };

  $scope.removeEntry = function (name, arr) {
    Helpers.checkAndRemove(name, arr);
  };

  $scope.submitData = function ($event) {
    $event.preventDefault();
    $scope.notification = '';
    if (!$scope.template) {
      $scope.notification = 'Template is empty.';
      return;
    }
    var postJson = {
      name: $scope.clusterName,
      clusterTemplate: $scope.clusterTemplateId,
      numMachines: $scope.clusterNumMachines,
      provider: $scope.defaultProvider,
      hardwaretype: $scope.defaultHardwareType,
      imagetype: $scope.defaultImageType,
      services: $scope.selectedServices,
      administration: {
        leaseduration: {
          initial: null
        }
      }
    };
    if ($scope.defaultConfig) {
      if (!Helpers.isValidJSON($scope.defaultConfig)) {
        $scope.notification = Helpers.JSON_ERR;
        return;
      }
      postJson.config = $.extend({}, JSON.parse($scope.defaultConfig));
    }
    postJson.administration.leaseduration.initial = Helpers.concatMilliseconds(
      $scope.leaseDuration.initial);
    if ($scope.template.administration.leaseduration.initial !== 0 &&
      $scope.template.administration.leaseduration.initial 
      < postJson.administration.leaseduration.initial) {
      $("#notification").text('You cannot initially request a longer lease.');
      $("html, body").animate({ scrollTop: 0 }, "slow");
      return;
    }
    Helpers.submitPost($event, postJson, '/user/clusters');
  };
}]);


