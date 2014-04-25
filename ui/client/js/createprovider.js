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
 * Create provider page.
 */

var CreateProviderApp = {};

CreateProviderApp = angular.module('CreateProviderApp', ['ngRoute'], ['$interpolateProvider',
  function ($interpolateProvider) {
  $interpolateProvider.startSymbol('[[');
  $interpolateProvider.endSymbol(']]');
}]);

CreateProviderApp.config(['$routeProvider',
  function ($routeProvider) {
    $routeProvider.
      when('/', {
        templateUrl: '/static/templates/providers/createprovider.html',
        controller: 'CreateProviderCtrl'
      }).
      when('/edit', {
        templateUrl: '/static/templates/providers/editprovider.html',
        controller: 'EditProviderCtrl'
      }).
      otherwise({
        redirectTo: '/'
      });
  }]);

CreateProviderApp.value('fetchUrl', '/pipeApiCall?path=');

CreateProviderApp.factory('dataFactory', ['$http', '$q', 'fetchUrl',
  function ($http, $q, fetchUrl) {
  var providerId = $("#inputName").val();
  return {
    getProviderId: function () {
      return providerId;
    },
    getCurrentProvider: function (currentProvider, callback) {
      $http.get(fetchUrl + '/providers/' + currentProvider).success(callback);
    },
    getProviders: function (callback) {
      $http.get(fetchUrl + '/providertypes').success(callback);
    }
  }
}]);

CreateProviderApp.controller('CreateProviderCtrl', ['$scope', '$interval', 'dataFactory',
  function ($scope, $interval, dataFactory) {


  $scope.providerType = '';
  $scope.providerData = {};
  $scope.providerTypes = [];
  $scope.providerInputs = {};


  dataFactory.getProviders(function (providertypes) {
    providertypes.map(function (item) {
      $scope.providerData[item.name] = item;
    });
  });

  $scope.$watch('providerType', function () {
    if ($scope.providerType) {
      $scope.providerInputs = $scope.providerData[$scope.providerType.name];  
    }
  });

  $scope.submitProvider = function ($event) {
    $event.preventDefault();
    if (!$scope.providerInputs) {
      $("#notification").text('You must select a provider.');
      $("html, body").animate({ scrollTop: 0 }, "slow");
    }
    var postJson = {
      name: $scope.inputName,
      description: $scope.inputDescription,
      providertype: $scope.providerType,
      provisioner: {}
    };
    for (var item in $scope.providerInputs.parameters.admin.fields) {
      postJson.provisioner[item] = $scope.providerInputs.parameters.admin.fields[item];
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

CreateProviderApp.controller('EditProviderCtrl', ['$scope', '$interval', 'dataFactory',
  function ($scope, $interval, dataFactory) {
    $scope.providerId = dataFactory.getProviderId();
    $scope.currProvider;
    $scope.providerInputs;
    
    $scope.providerData = {};
    dataFactory.getProviders(function (providertypes) {
      providertypes.map(function (item) {
        $scope.providerData[item.name] = item;
      });
    });

    dataFactory.getCurrentProvider($scope.providerId, function (provider) {
      $scope.currProvider = provider;
    });

  $scope.$watch('currProvider.providertype', function () {
    if ($scope.currProvider && 'providertype' in $scope.currProvider) {
      $scope.providerInputs = $scope.providerData[$scope.currProvider.providertype];
      AppHelpers.addInputSchema($scope.currProvider, $scope.providerInputs);
    }
  });

    $scope.$watchCollection('[currProvider,providerData]', function (newValues, oldValues) {
      if (!$.isEmptyObject($scope.currProvider) && !$.isEmptyObject($scope.providerData)) {
        $scope.currProvider = AppHelpers.addInputSchema(
          $scope.currProvider, $scope.providerData[$scope.providerId]);
      }
    }, true);

    $scope.submitProvider = function ($event) {
      $event.preventDefault();
      if (!$scope.currProvider.providertype) {
        $("#notification").text('You must select a provider.');
        $("html, body").animate({ scrollTop: 0 }, "slow");
      }
      var postJson = {
        name: $scope.currProvider.name,
        description: $scope.currProvider.description,
        providertype: $scope.currProvider.providertype,
        provisioner: {}
      };
      for (var item in $scope.currProvider.provisioner) {
        postJson.provisioner[item] = $scope.currProvider.provisioner[item]['userinput'];
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

/**
 * Helper methods.
 */
var AppHelpers = {};

AppHelpers.addInputSchema = function (currProvider, providerInputs) {
  if (providerInputs) {
    for (var item in currProvider.provisioner) {
      for (var entry in providerInputs.parameters.admin.fields) {
        if (entry === item) {
          var userinput = currProvider.provisioner[entry];

          if(typeof currProvider.provisioner[entry] !== 'object') {
            currProvider.provisioner[entry] = {
              userinput: userinput
            };
          }

          for (var field in providerInputs.parameters.admin.fields[entry]) {
            currProvider.provisioner[entry][field] = (
              providerInputs.parameters.admin.fields[entry][field]);
          }
        }
      }
    }
  }
  return currProvider;
};