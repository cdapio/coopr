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
      scope.allowAdd = angular.isArray(scope.available); // can we add?
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
          if((avoidable||[]).indexOf(svc.name)===-1) {
            out.push({
              text: svc.name,
              click: 'addService("'+svc.name+'")'
            });
          }
          return out;
        }, []);
      }

      function remapActionables (visible) {
        $scope.actionDropdowns = (visible||[]).reduce(function (out, name) {
          var dd = [];

          if($scope.allowMngmt) {
            dd.push({
              text: '<span class="fa fa-fw fa-play"></span>&nbsp;&nbsp;Start',
              click: 'manageService("start", "'+name+'")'
            });
            dd.push({
              text: '<span class="fa fa-fw fa-stop"></span>&nbsp;&nbsp;Stop',
              click: 'manageService("stop", "'+name+'")'
            });
            dd.push({
              text: '<span class="fa fa-fw fa-undo"></span>&nbsp;&nbsp;Restart',
              click: 'manageService("restart", "'+name+'")'
            });
            if($scope.allowRm) {
              dd.push({divider:true});
            }
          }

          if($scope.allowRm) {
            dd.push({
              text: '<span class="fa fa-fw fa-remove"></span>&nbsp;&nbsp;Remove',
              click: 'rmService("'+name+'")'
            });
          }

          out[name] = dd;
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
