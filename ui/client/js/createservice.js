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

var ServiceCtrl = {};

ServiceCtrl = angular.module('CreateServiceApp', ['ngRoute'], ['$interpolateProvider',
  function ($interpolateProvider) {
  $interpolateProvider.startSymbol('[[');
  $interpolateProvider.endSymbol(']]');
}]);

// ServiceCtrl.config(['$routeProvider',
//   function ($routeProvider) {
//   $routeProvider.
//     when('/', {
//       templateUrl: '/static/templates/providers/createprovider.html',
//       controller: 'CreateProviderCtrl'
//     }).
//     when('/edit', {
//       templateUrl: '/static/templates/providers/editprovider.html',
//       controller: 'EditProviderCtrl'
//     }).
//     otherwise({
//       redirectTo: '/'
//     });
// }]);

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

  $scope.addService = function () {
    Helpers.checkAndAdd($scope.currService, $scope.selectedServices);
  };

  $scope.removeService = function (service) {
    Helpers.checkAndRemove(service, $scope.selectedServices);
  };

  $scope.addEmptyAction = function () {
    $scope.actions.push({
      category: ''
    });
  };

  /**
   * Submit provider information.
   * @param  {Object} $event Form submit event.
   */
  $scope.submitProvider = function ($event) {
    $event.preventDefault();
    if (!$scope.providerInputs) {
      $("#notification").text('You must select a provider.');
      $("html, body").animate({ scrollTop: 0 }, "slow");
    }
    var postJson = {
      name: $scope.inputName,
      description: $scope.inputDescription,
      providertype: $scope.providerType.name,
      provisioner: {}
    };
    for (var item in $scope.providerInputs.parameters.admin.fields) {
      postJson.provisioner[item] = $scope.providerInputs.parameters.admin.fields[item]['userinput'];
    }
    if (Helpers.isProviderInputValid(
      postJson, $scope.providerInputs.parameters.admin.required)) {
      Helpers.submitPost($event, postJson, '/providers');  
    } else {
      $("#notification").text('Required fields missing.');
      $("html, body").animate({ scrollTop: 0 }, "slow");
    }
    
  };

}]);


define([], function () {
  var Page = {

    init: function () {
      var self = this;
      $("#add-action").click(function () {
        var providerEntry = $(".action-entry").clone()[0];
        $(".action-entries").append(providerEntry);
      });

      $("#add-service").click(function (e) {
        self.handleAddServiceEntry(e);
      });

      $("#create-service-form").submit(function (e) {
        e.preventDefault();
        self.getFormDataAndSubmit(e);
      });

      $(".service-delete").click(function (e) {
        $(this).parent().remove();
      });

      $(".service-delete-form").submit(function (e) {
        e.preventDefault();
        Helpers.handleConfirmDeletion(e, '/services');
      });

    },

    handleAddServiceEntry: function (e) {
      var self = this;
      var context = $(e.currentTarget).attr('data-context');
      var valToAdd = $(e.currentTarget).parent().parent().find('select').val();
      if (!valToAdd) {
        return;
      }
      var serviceName = $('<span class="service-name"></span>').text(valToAdd);
      var serviceDelete = $(
        '<span class="service-delete pointer-cursor right-float glyphicon glyphicon-minus"></span>'
      );
      var div = $('<div class="form-control"></div>').append(serviceName).append(serviceDelete);
      $('.service-entries').append(div);
      Helpers.bindDeletion('service-delete');
    },

    getFormDataAndSubmit: function (e) {
      var self = this;
      var postJson = {
        name: $("#inputName").val(),
        description: $("#inputDescription").val(),
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
      var actionEntries = $(".action-entry");
      for (var i = 0; i < actionEntries.length; i++) {
        var configurables = { 
          type: $(actionEntries[i]).find("[name=inputType]").val(),
          script: $(actionEntries[i]).find("[name=inputScript]").val(),
        };
        if ($(actionEntries[i]).find("[name=inputData]").val()) {
          configurables['data'] = $(actionEntries[i]).find("[name=inputData]").val();
        }
        postJson.provisioner.actions[$(actionEntries[i]).find("[name=inputCategory]").val()] = 
          configurables;
          
      }
      var serviceEntries = $(".service-entries .service-name").each(function (index, item) {
        postJson.dependencies.runtime.requires.push($(item).text());
      });
      Helpers.submitPost(e, postJson, '/services');
    }

  };

  return Page.init();

});
