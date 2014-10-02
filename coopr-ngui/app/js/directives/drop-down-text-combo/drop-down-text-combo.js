angular.module(PKG.name+".directives")
  .directive("dropDownTextCombo", function dropDownTextComboDirective() {
    return {
      restrict: "E",
      scope: {
        model: "=ngModel",
        dropDownList: "=",
        textFields: "="
      },
      templateUrl: "drop-down-text-combo/drop-down-text-combo.html",
      link: function ($scope) {
        $scope.dropDownValues = [];
        function buildDropDown () {
          //dropDownList doesn't always needs to be a $resource object with a promise.
          if(
              (
                $scope.dropDownList.$promise &&
                  !$scope.dropDownList.$resolved
              )||
              !$scope.model) {
            return;
          }
          $scope.dropDownValues = $scope.dropDownList
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

        //dropDownList doesn't always needs to be a $resource object with a promise.
        if ($scope.dropDownList.$promise) {
          $scope.dropDownList.$promise.then(buildDropDown);
        }

        $scope.$watch("model", buildDropDown);

        $scope.rmProvider = function (pName) {
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
