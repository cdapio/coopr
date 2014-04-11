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
 * Cluster template view module. Depends on AngularJS.
 */
var TemplateView = {};

TemplateView.app = angular.module('templateview', [], ['$interpolateProvider',
  function ($interpolateProvider) {
  $interpolateProvider.startSymbol('[[');
  $interpolateProvider.endSymbol(']]');
}]);

TemplateView.app.value('fetchUrl', '/pipeApiCall?path=');
TemplateView.app.value('templateEndpoint', '/clustertemplates/');

TemplateView.app.factory('dataFactory', ['$http', '$q', 'fetchUrl', 'templateEndpoint',
  function ($http, $q, fetchUrl, templateEndpoint) {
  var templateId = $("#inputName").val();
  return {
    getTemplateId: function () {
      return templateId;
    },
    getTemplate: function (callback) {    
      $http.get(fetchUrl + templateEndpoint + templateId).success(callback);
    },
    getProviders: function (callback) {
      $http.get(fetchUrl + '/providers').success(callback);
    },
    getHardwaretypes: function (callback) {
      $http.get(fetchUrl + '/hardwaretypes').success(callback);
    },
    getImagetypes: function (callback) {
      $http.get(fetchUrl + '/imagetypes').success(callback);
    },
    getServices: function (callback) {
      $http.get(fetchUrl + '/services').success(callback);
    }
  }
}]);

TemplateView.app.controller('TemplateCtrl', ['$scope', '$interval', 'dataFactory',
  'templateEndpoint', function ($scope, $interval, dataFactory, templateEndpoint) {

  $scope.configDiv = $('#inputConfig')[0];
  $scope.jsonValidEl = $('#is-json-valid')[0];

  /**
   * Cluster template model description.
   */
  $scope.clustertemplate = {
    name: '',
    description: '',
    defaults: {
      services: [],
      provider: '',
      hardwaretype: '',
      imagetype: '',
      dnsSuffix: '',
      config: ''
    },
    compatibility: {
      hardwaretypes: [],
      imagetypes: [],
      services: []
    },
    constraints: {
      layout: {
        mustcoexist: [],
        cantcoexist: []
      },
      services: {}
    },
    administration: {
      leaseduration: {}
    }
  };

  /**
   * Current constraint model description. Used for defining coexisting and layout service groups.
   */
  $scope.currentConstraints = {
    mustcoexistGroup: [],
    cantcoexistGroup: [],
    serviceGroup: {
      name: '',
      hardwaretypes: [],
      imagetypes: [],
      min: '',
      max: ''
    }
  };

  $scope.leaseDuration = {
    initial: {},
    max: {},
    step: {}
  };

  /**
   * Set total tab length to enable Next button.
   */
  $scope.totalTabs = $("ul#sectionTab li").length;

  /**
   * Id of this cluster template.
   */
  $scope.templateId = dataFactory.getTemplateId();

  /**
   * Load data for cluster template options.
   */
  dataFactory.getProviders(function (providers) {
    $scope.providers = providers.map(function (provider) {
      return provider.name;
    });
  });
  dataFactory.getHardwaretypes(function (hardwaretypes) {
    $scope.hardwaretypes = hardwaretypes.map(function (hardwaretype) {
      return hardwaretype.name;
    });
  });
  dataFactory.getImagetypes(function (imagetypes) {
    $scope.imagetypes = imagetypes.map(function (imagetype) {
      return imagetype.name;
    });
  });
  dataFactory.getServices(function (services) {
    $scope.services = services.map(function (service) {
      return service.name;
    }).sort();
  });
  
  if ($scope.templateId) {

    // Get cluster template and map to model.
    dataFactory.getTemplate(function (clustertemplate) {
      if ('config' in clustertemplate.defaults) {
        $scope.defaultConfig = JSON.stringify(clustertemplate.defaults.config, undefined, 4);
      }
      if ('services' in clustertemplate.constraints) {
        for (var item in clustertemplate.constraints.services) {
          if (!('hardwaretypes' in clustertemplate.constraints.services[item])) {
            clustertemplate.constraints.services[item]['hardwaretypes'] = [];
          }
          if (!('imagetypes' in clustertemplate.constraints.services[item])) {
            clustertemplate.constraints.services[item]['imagetypes'] = [];
          }
        }
      }

      if ('administration' in clustertemplate) {
        $scope.leaseDuration.initial = Helpers.parseMilliseconds(
          clustertemplate.administration.leaseduration.initial);
        $scope.leaseDuration.max = Helpers.parseMilliseconds(
          clustertemplate.administration.leaseduration.max);
        $scope.leaseDuration.step = Helpers.parseMilliseconds(
          clustertemplate.administration.leaseduration.step);
      }

      // Prettify the json config manually since PP doesn't recognize angular change event.
      PP.prettify($scope.configDiv, $scope.jsonValidEl, 1000, true);

      $scope.clustertemplate = $.extend($scope.clustertemplate, clustertemplate);
      $scope.templateJSON = JSON.stringify($scope.clustertemplate);
      Helpers.enableTableSorting($interval);
    });
  }

  $scope.addEntry = function (name, arr) {
    Helpers.checkAndAdd(name, arr);
  };

  $scope.removeEntry = function (name, arr) {
    Helpers.checkAndRemove(name, arr);
  };

  $scope.clearServiceConstraintForm = function () {
    $scope.currentConstraints = {
      mustcoexistGroup: [],
      cantcoexistGroup: [],
      serviceGroup: {
        name: '',
        hardwaretypes: [],
        imagetypes: [],
        min: '',
        max: ''
      }
    };
  };

  /**
   * Adds a service constraint to existing service constraints.
   */
  $scope.addServiceConstraint = function () {
    var constraint = {
      quantities: {}
    };

    if (parseInt($scope.currentConstraints.serviceGroup.min)) {
      constraint['quantities']['min'] = $scope.currentConstraints.serviceGroup.min;
    }

    if (parseInt($scope.currentConstraints.serviceGroup.max)) {
      constraint['quantities']['max'] = $scope.currentConstraints.serviceGroup.max;
    }

    if ($scope.currentConstraints.serviceGroup.hardwaretypes.length) {
      constraint['hardwaretypes'] = $scope.currentConstraints.serviceGroup.hardwaretypes;
    }
    if ($scope.currentConstraints.serviceGroup.hardwaretypes.length) {
      constraint['imagetypes'] = $scope.currentConstraints.serviceGroup.imagetypes;
    }
    $scope.clustertemplate.constraints.services[$scope.currentConstraints.serviceGroup.name] = 
      constraint;
  };

  /**
   * Removes service constraint from existing service constraints if it exists.\
   */
  $scope.removeServiceConstraint = function (constraint) {
    if (constraint in $scope.clustertemplate.constraints.services) {
      delete $scope.clustertemplate.constraints.services[constraint];
    }
  };

  /**
   * Submits data when user click on the submit button of form.
   * @param  {Object} $event event fired off by form submit.
   */
  $scope.submitData = function ($event) {
    $event.preventDefault();
    $scope.notification = '';
    if ($scope.defaultConfig) {
      if (!Helpers.isValidJSON($scope.defaultConfig)) {
        $scope.notification = Helpers.JSON_ERR;
        return;
      } else {
        $scope.clustertemplate.defaults.config = $.extend({}, JSON.parse($scope.defaultConfig));
      }
    } else {
      delete $scope.clustertemplate.defaults.config;
    }
    if ('administration' in $scope.clustertemplate) {
      $scope.clustertemplate.administration.leaseduration.initial = Helpers.concatMilliseconds(
        $scope.leaseDuration.initial);
      $scope.clustertemplate.administration.leaseduration.max = Helpers.concatMilliseconds(
        $scope.leaseDuration.max);
      $scope.clustertemplate.administration.leaseduration.step = Helpers.concatMilliseconds(
        $scope.leaseDuration.step);
    }
    Helpers.submitPost($event, $scope.clustertemplate, '/clustertemplates');
  };

  /**
   * Deletes a cluster from view after confirming with user.
   * @param  {Object} $event contains form submit event information.
   */
  $scope.deleteTemplate = function ($event) {
    $event.preventDefault();
    Helpers.handleConfirmDeletion($event, '/clustertemplates');
  };

  /**
   * Moves to next tab upon clicking next button.
   */
  $scope.nextTab = function () {
    var currentTab = $("ul#sectionTab li.active").index() + 1;
    if (currentTab < $scope.totalTabs) {
      $('#sectionTab li:eq(' + currentTab + ') a').tab('show');
    } else {
      $('#sectionTab li:eq(0) a').tab('show');
    }
  };

}]);
