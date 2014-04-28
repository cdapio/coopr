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
 * Service pages: create, edit and delete services.
 */
var ServiceCtrl = {};

ServiceCtrl = angular.module('CreateServiceApp', ['ngRoute'], ['$interpolateProvider',
  function ($interpolateProvider) {
  $interpolateProvider.startSymbol('[[');
  $interpolateProvider.endSymbol(']]');
}]);

ServiceCtrl.value('fetchUrl', '/pipeApiCall?path=');

ServiceCtrl.factory('dataFactory', ['$http', '$q', 'fetchUrl',
  function ($http, $q, fetchUrl) {
  var serviceId = $("#inputName").val();
  return {
    getServiceId: function () {
      return serviceId;
    },
    getCurrentService: function (currentService, callback) {
      $http.get(fetchUrl + '/services/' + currentService).success(callback);
    },
    getAvailableServices: function (callback) {
      $http.get(fetchUrl + '/services').success(callback);
    },
    getAutomators: function (callback) {
      $http.get(fetchUrl + '/automators').success(callback);
    }
  }
}]);


ServiceCtrl.controller('ServiceCtrl', ['$scope', '$interval', 'dataFactory',
  function ($scope, $interval, dataFactory) {

  $scope.name;
  $scope.description;
  $scope.automatorData = {};
  $scope.availableServices = [];
  $scope.selectedServices = [];
  $scope.currService;

  $scope.actions = [];
  $scope.categoryOptions = [
    'install',
    'remove',
    'initialize',
    'configure',
    'start',
    'stop'
  ];

  $scope.serviceId = dataFactory.getServiceId();
  $scope.serviceExists = false;

  // If serviceId exists, we are updating existing service, load service into UI.
  if ($scope.serviceId) {
    $scope.serviceExists = true;
    dataFactory.getCurrentService($scope.serviceId, function (service) {
      if (Object.keys(service).length) {
        $scope.name = service.name;
        $scope.description = service.description;
        $scope.selectedServices = service.dependencies.runtime.requires;
        $scope.actions = ServiceCtrl.getActionsFromServiceTempl(service.provisioner.actions);
      }
    });
  }

  dataFactory.getAutomators(function (automators) {
    automators.map(function (item) {
      $scope.automatorData[item.name] = item;
    });
  });

  dataFactory.getAvailableServices(function (services) {
    $scope.availableServices = services.map(function (item) {
      return item.name;
    });
  });

  /**
   * Adds service dependency to existing services.
   */
  $scope.addService = function () {
    Helpers.checkAndAdd($scope.currService, $scope.selectedServices);
  };

  /**
   * Removes a service dependecy from existing services.
   * @param  {String} service.
   */
  $scope.removeService = function (service) {
    Helpers.checkAndRemove(service, $scope.selectedServices);
  };

  /**
   * Creates a blank configurable action.
   */
  $scope.addEmptyAction = function () {
    $scope.actions.push({
      category: '',
      fields: {}
    });
  };

  /**
   * Removes action based on index.
   * @param  {Number} actionIndex index of action in actions array.
   */
  $scope.removeAction = function (actionIndex) {
    $scope.actions.splice(actionIndex, 1);
  };

  /**
   * Handles deletion of service, shows deletion modal.
   * @param  {Object} $event form submit event.
   */
  $scope.handleDeletion = function ($event) {
    $event.preventDefault();
    Helpers.handleConfirmDeletion($event, '/services');
  };

  /**
   * Submit provider information.
   * @param  {Object} $event Form submit event.
   */
  $scope.handleFormSubmit = function ($event) {
    $event.preventDefault();
    var self = this;
    var postJson = {
      name: $scope.name,
      description: $scope.description,
      dependencies: {
        provides: [],
        conflicts: [],
        install: {
          requires: [],
          uses: []
        },
        runtime: {
          requires: [],
          uses: []
        }
      },
      provisioner: {
        actions: {}
      }
    };

    postJson.dependencies.runtime.requires = $scope.selectedServices;
    var allInputValid = true;
    for (var i = 0, len = $scope.actions.length; i < len; i++) {
      if (!Helpers.isInputValid($scope.actions[i].fields,
       $scope.automatorData[$scope.actions[i].type].parameters.admin.required)) {
        allInputValid = false;
        $("#notification").text('Required fields missing for action automators.');
        $("html, body").animate({ scrollTop: 0 }, "slow");
        return;
      }
      postJson.provisioner.actions[$scope.actions[i].category] = {
        type: $scope.actions[i].type,
        fields: $scope.actions[i].fields
      };
    }
    if (allInputValid) {
      Helpers.submitPost($event, postJson, '/services');
    }
  };

}]);

/**
 * Modifies actions json received from api call to the format used by the UI. It changes actions
 * from:
 * actions: {
 *   categoryType: {
 *     fields: {}
 *     type: ""
 *   }
 * }
 * to:
 * [ {category: categoryType, type: "", fields: fields },...]
 * @param  {Object} templActions Template actions object from API call.
 * @return {Array} List of template actions in UI friendly format.
 */
ServiceCtrl.getActionsFromServiceTempl = function (templActions) {
  var actionsArr = [];
  for (var category in templActions) {
    if (templActions.hasOwnProperty(category)) {
      var action = templActions[category];
      actionsArr.push({
        category: category,
        type: action.type,
        fields: action.fields
      });
    }
  }
  return actionsArr;
};

