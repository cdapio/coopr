/*
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

var NodeView = {};

NodeView.app = angular.module("nodeview", ["ui.bootstrap", "ngTable", "ngSanitize"], ['$interpolateProvider', function($interpolateProvider) {
  $interpolateProvider.startSymbol('[{');
  $interpolateProvider.endSymbol('}]');
}]);

NodeView.app.value("fetchUrl", "/pipeApiCall?path=");

NodeView.app.factory("dataFactory", ["$http", "$q", "fetchUrl", function($http, $q, fetchUrl) {
  return {
    getNodes: function(callback) {
      $http.get(fetchUrl + "/nodes").success(callback);
    },
    getDefaultColumns: function(callback) {
      $http.get("/nodes/columns").success(callback);
    }
  };
}]);

function createColumns(rows, filterVisible, defaultColumns) {
  var i = 0;
  var columns = [];
  for (var property in rows[0]) {
    if (rows[0].hasOwnProperty(property)) {
      var columnVisibility = false;
      var columnIndex;
      angular.forEach(defaultColumns, function(defaultColumn) {
        if (defaultColumn.header === property) {
          columnVisibility = true;
          columnIndex = defaultColumn.index;
        }
      });
      var column = {
        id: i++,
        title: property,
        field: property,
        visible: columnVisibility,
        filterVisible: filterVisible
      };

      var filter = {};
      filter[property] = "text";

      column.filter = filter;
      if (columnIndex) {
        columns.splice(columnIndex, 0, column);
      } else {
        columns.push(column);
      }
    }
  }
  return columns;
}

function flattenNodeProperties(nodes) {
  angular.forEach(nodes, function(node) {
    for (var property in node.properties) {
      if (node.properties.hasOwnProperty(property)) {
        if (typeof node.properties[property] === "object") {
          for (var propertyObject in node.properties[property]) {
            if (node.properties[property].hasOwnProperty(propertyObject)) {
              node[property + " " + propertyObject] = node.properties[property][propertyObject];
            }
          }
        } else {
          node[property] = node.properties[property];
        }
      }
    }

    for (property in node.metadata) {
      if (node.metadata.hasOwnProperty(property)) {
        node[property] = node.metadata[property];
      }
    }

    delete node.properties;
    delete node.metadata;
  });

  return nodes;
}

function createTopLevelFilter($scope, topLevelFilterName) {
  $scope.topLevelFilters = [];
  angular.forEach($scope.rows, function(row) {
    if (row.hasOwnProperty(topLevelFilterName)) {
      if ($scope.topLevelFilters.indexOf(row[topLevelFilterName]) == -1 && row[topLevelFilterName] !== "" && row[topLevelFilterName] !== undefined) {
        $scope.topLevelFilters.push(row[topLevelFilterName]);
      }
    }
  });

  $scope.topLevelFilterName = topLevelFilterName;
  $scope.topLevelFilter = null;

  $scope.$watch("selectedTopLevelFilter", function(changedSelectedTopLevelFilter) {
    if (changedSelectedTopLevelFilter === null || changedSelectedTopLevelFilter === undefined) {
      $scope.topLevelFilter = null;
    } else {
      $scope.topLevelFilter = {};
      $scope.topLevelFilter[$scope.topLevelFilterName] = changedSelectedTopLevelFilter;
    }
    if (angular.isDefined($scope.tableParams)) {
      $scope.tableParams.reload();
      $scope.tableParams.page(1);
    }
  });
}

NodeView.app.controller("NodeViewCtrl", ["$scope", "$filter", "ngTableParams", "dataFactory", "$modal", "$log", function($scope, $filter, ngTableParams, dataFactory, $modal, $log) {

  dataFactory.getNodes(function(nodes) {
    if (nodes.length === 0) {
      // TODO: Maybe put error here
      return;
    }
    dataFactory.getDefaultColumns(function(defaultColumns) {
      createTable($scope, nodes, ngTableParams, $filter, defaultColumns);
      createToggles($scope);
      createCheckboxes($scope);
      createTopLevelFilter($scope, "team");
    });
  });

  $scope.openModal = function(index, size) {
    $modal.open({
      templateUrl: '/static/templates/user/nodes/nodesMetadataModal.html',
      controller: ModalInstanceCtrl,
      size: size,
      resolve: {
        item: function() {
          return $scope.rows[index];
        }
      }
    });
  };
}]);

function createToggles($scope) {
  $scope.globalFilterVisibleState = false;
  $scope.inputGlobalSearch = "";

  $scope.$watch("inputGlobalSearch", function() {
    $scope.globalSearchFilter = createGlobalSearchFilter($scope.inputGlobalSearch);

    if (angular.isDefined($scope.tableParams)) {
      $scope.tableParams.reload();
      $scope.tableParams.page(1);
    }
  });

  $scope.toggleColumn = function(column) {
    column.visible = !column.visible;
    column.filterVisible = shouldFilterBeVisibleWhenToggleColumn(column, $scope.globalFilterVisibleState);
  };
}

function createCheckboxes($scope) {
  $scope.checkboxes = { "checked": false, items: {} };
  // watch for check all checkbox
  $scope.$watch("checkboxes.checked", function(value) {
    angular.forEach($scope.rows, function(item) {
      if (angular.isDefined(item.id)) {
        $scope.checkboxes.items[item.id] = value;
      }
    });
  });

  // watch for data checkboxes
  $scope.$watch("checkboxes.items", function() {
    if (!$scope.rows) {
      return;
    }
    var checked = 0, unchecked = 0, total = $scope.rows.length;
    angular.forEach($scope.rows, function(item) {
      checked += ($scope.checkboxes.items[item.id]) || 0;
      unchecked += (!$scope.checkboxes.items[item.id]) || 0;
    });
    if ((unchecked === 0) || (checked === 0)) {
      $scope.checkboxes.checked = (checked === total);
    }
    // grayed checkbox
    angular.element(document.getElementById("select_all")).prop("indeterminate", (checked != 0 && unchecked != 0));
  }, true);
}

NodeView.app.filter('capitalize', function() {
  return function(input) {
    return (!!input) ? input.replace(/([^\W_]+[^\s-]*) */g, function(txt) {
      return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
    }) : '';
  };
});

NodeView.app.filter('uncamel', function() {
  return function(input) {
    return input.replace(/([A-Z])/g, function($1) {
      return " " + $1.toLowerCase();
    });
  };
});

NodeView.app.filter('regex', function() {
  return function(input, field, $scope) {
    if (field.$ === null || field.$ === undefined || field.$ === "") {
      return input;
    }

    // Case insensitive regex
    var patt = new RegExp(field.$, "i");
    var out = [];
    for (var i = 0; i < input.length; i++) {
      for (var j = 0; j < $scope.defaultColumns.length; j++) {
        var columnHeader = $scope.defaultColumns[j].header;
        if (input[i].hasOwnProperty(columnHeader)) {
          if (patt.test(input[i][columnHeader])) {
            out.push(input[i]);
            break;
          }
        }
      }
    }
    return out;
  };
});

function createTable($scope, nodes, ngTableParams, $filter, defaultColumns) {
  $scope.columns = [];
  $scope.defaultColumns = defaultColumns;
  $scope.globalFilterVisibleState = false;
  $scope.rows = flattenNodeProperties(nodes);
  $scope.columns = createColumns($scope.rows, false, defaultColumns);
  $scope.tableParams = new ngTableParams({
    page: 1,            // show first page
    count: 25,           // count per page
    sorting: {
      id: "asc"     // initial sorting
    }
  }, {
    total: $scope.rows.length, // length of data
    filterDelay: 0,
    getData: function($defer, params) {
      // use build-in angular filter
      var filteredData = $scope.topLevelFilter ? $filter("filter")($scope.rows, $scope.topLevelFilter) : $scope.rows;
      filteredData = $scope.globalSearchFilter ? $filter("regex")(filteredData, $scope.globalSearchFilter, $scope) : filteredData;
      var orderedData = params.sorting() ? $filter("orderBy")(filteredData, params.orderBy()) : filteredData;

      params.total(orderedData.length); // set total for recalc pagination
      $scope.totalShowingRows = orderedData.length;
      $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
    }
  });
}

function createGlobalSearchFilter(searchText) {
  if (searchText === "") {
    return null;
  }

  var globalSearchFilter = {};
  globalSearchFilter.$ = searchText;
  return globalSearchFilter;
}

var ModalInstanceCtrl = function($scope, $modalInstance, item) {
  $scope.item = item;

  $scope.close = function() {
    $modalInstance.close();
  };
};