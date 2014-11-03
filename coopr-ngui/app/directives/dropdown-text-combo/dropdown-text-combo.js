angular.module(PKG.name + ".commons")
  .directive("myDropdownTextCombo", function myDropdownTextComboDirective() {
    return {
      restrict: "E",
      scope: {
        model: "=",
        dropdownList: "=",
        textFields: "=",
        assetLabel: "@"
      },
      templateUrl: "dropdown-text-combo/dropdown-text-combo.html",
      link: function ($scope) {
        $scope.dropdownValues = [];

        function buildDropdown () {
          //dropdownList doesn't always needs to be a $resource object with a promise.
          if(
              (
                $scope.dropdownList.$promise &&
                  !$scope.dropdownList.$resolved
              )||
              !$scope.model) {
            return;
          }
          $scope.dropdownValues = $scope.dropdownList
            .filter(function (item) {
              var isValid = Object.keys($scope.model)
                                  .indexOf(item.name) === -1;
              return isValid;
            })
            .map(function (item) {
              return {
                text: item.name,
                click: "addAsset(\""+item.name+"\")"
              };
            });
        }

        //dropdownList doesn't always needs to be a $resource object with a promise.
        if ($scope.dropdownList.$promise) {
          $scope.dropdownList.$promise.then(buildDropdown);
        }

        $scope.$watchCollection("model", buildDropdown);

        $scope.rmAsset = function (pName) {
          delete $scope.model[pName];
        };

        $scope.addAsset = function (pName) {
          if(!$scope.model) { return; }

          $scope.model[pName] = {
            name: pName
          };

        };
      }
    };
  });
