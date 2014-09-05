var module = angular.module(PKG.name+'.directives');

module.directive('myServicePicker', function myServicePickerDirective () {
  return {
    restrict: 'E',
    templateUrl: 'servicepicker/servicepicker.tpl',

    scope: {
      model: '=',
      available: '=',
      readonly: '@'
    },

    controller: function ($scope) {

      $scope.rmService = function (name) {
        $scope.model = $scope.model.filter(function (svc) {
          return svc !== name;
        });
      };

      $scope.pushService = function (name) {
        $scope.model.push(name);
        remapDropdown($scope.available, $scope.model);
      };

      function remapDropdown (available, avoidable) {
        $scope.dropdown = (available||[]).reduce(function (out, svc) {
          if((avoidable||[]).indexOf(svc.name)===-1) {
            out.push({
              text: svc.name,
              click: 'pushService("'+svc.name+'")'
            });
          }
          return out;
        }, []);
      }

      $scope.$watch('model', function(newVal) {
        remapDropdown($scope.available, newVal);
      });

      $scope.$watch('available', function(newVal) {
        remapDropdown(newVal, $scope.model);
      });

    }
  };
});
