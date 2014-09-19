angular.module(PKG.name + '.directives')
  .directive('dropDownTextCombo', function dropDownTextCombo() {
    return {
      restrict: 'E',
      scope: {
        dropDownDisable: "=",
        dropDownValue: "=",
        textValue: "=",
        dropDownOptions: "="
      },
      replace: true,
      templateUrl: 'drop-down-text-combo/drop-down-text-combo.html',
      link: function($scope, $element) {
        $scope.$watch("dropDownDisable", function(newValue, oldValue, $scope) {
          if (newValue !== true) {
            $element.find("select").removeAttr("disabled");
          }
        });
      }

    }
  })