var module = angular.module(PKG.name+'.directives');

module.directive('myServicePicker', function myServicePickerDirective () {
  return {
    restrict: 'E',
    templateUrl: 'servicepicker/servicepicker.tpl',

    scope: {
      model: '=',
      available: '='
    },

    link: function(scope, element, attrs) {
      scope.allowRm = !!attrs.allowRm && attrs.allowRm!=='false'; // can we delete?
      scope.allowMngmt = !!attrs.allowMngmt && attrs.allowMngmt!=='false'; // can we manage?
    },

    controller: function ($scope) {

      $scope.rmService = function (name) {
        $scope.model = $scope.model.filter(function (svc) {
          return svc !== name;
        });
      };

      $scope.addService = function (name) {
        $scope.model.push(name);
      };

      $scope.manageService = function (name) {
        alert('Not yet implemented');
      };

      function remapAddables (available, avoidable) {
        $scope.addsvcDropdown = (available||[]).reduce(function (out, svc) {
          if((avoidable||[]).indexOf(svc)===-1) {
            out.push({
              text: svc,
              click: 'addService("'+svc+'")'
            });
          }
          return out;
        }, []);
      }

      function remapActionables (visible) {
        $scope.actionDropdowns = (visible||[]).reduce(function (out, svc) {
          var dd = [];

          if($scope.allowMngmt) {
            dd.push({
              text: '<span class="fa fa-fw fa-play"></span> Start',
              click: 'manageService("start", "'+svc+'")'
            });
            dd.push({
              text: '<span class="fa fa-fw fa-stop"></span> Stop',
              click: 'manageService("stop", "'+svc+'")'
            });
            dd.push({
              text: '<span class="fa fa-fw fa-undo"></span> Restart',
              click: 'manageService("restart", "'+svc+'")'
            });
            if($scope.allowRm) {
              dd.push({divider:true});
            }
          }

          if($scope.allowRm) {
            dd.push({
              text: '<span class="fa fa-fw fa-remove"></span>&nbsp;&nbsp;Remove',
              click: 'rmService("'+svc+'")'
            });
          }

          out[svc] = dd;
          return out;
        }, {});
      }

      $scope.$watchCollection('model', function(newVal) {
        remapAddables($scope.available, newVal);
        remapActionables(newVal);
      });

      $scope.$watchCollection('available', function(newVal) {
        remapAddables(newVal, $scope.model);
      });

    }
  };
});
