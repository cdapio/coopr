/**
 * myThingPicker
 */

angular.module(PKG.name+'.directives').directive('myThingPicker',
function myThingPickerDirective () {
  return {
    restrict: 'E',
    templateUrl: 'thingpicker/thingpicker.html',
    replace: true,
    scope: {
      model: '=', // an array of names
      available: '=', // an array of strings, or of objects with name & description keys
      allowRm: '=', // allow removal boolean
      listInline: '@',
      thingName: '@',
      popupMode: '='
    },

    controller: function ($scope, myApi, $modal) {
      var modalInstance;
      $scope.rmThing = function (thing) {
        $scope.model = $scope.model.filter(function (one) {
          return one !== thing;
        });
      };

      $scope.addThing = function (thing) {

        if ($scope.popupMode) {
          if (validateThing(thing)) {
            $scope.onError = false;
            modalInstance.destroy();
          } else {
            $scope.popupError = $scope.thingName + ' "' + thing+ '" ' + ' already exists!';
            $scope.onError = true;
            return;
          }
        }
        if(!angular.isArray($scope.model)) {
          $scope.model = [];
        }
        $scope.model.push(thing);
      };

      $scope.$watchCollection('model', function(newVal) {
        remapAddables($scope.available, newVal);
        remapActionables(newVal);
      });

      $scope.$watchCollection('available', function(newVal) {
        remapAddables(newVal, $scope.model);
      });

      function validateThing(thing) {
        var isAlreadyExists = $scope.model.filter(function(existingThing) {
          return existingThing === thing;
        });
        return isAlreadyExists.length === 0;
      }

      function remapAddables (available, avoidable) {
        $scope.addDropdown = (available||[]).reduce(function (out, thing) {
          var name = thing.name || thing; // in case available is an array of strings
          if((avoidable||[]).indexOf(name)===-1) {
            out.push({
              text: name,
              click: 'addThing("'+name+'")'
            });
          }
          return out;
        }, []);
      }

      function remapActionables (visible) {
        $scope.actionDropdowns = (visible||[]).reduce(function (out, name) {

          var dd = [];

          if($scope.allowRm) {
            dd.push({
              text: '<span class="fa fa-fw fa-remove"></span> Remove',
              click: 'rmThing("'+name+'")'
            });
          }

          out[name] = dd;

          return out;
        }, {});
      }

      $scope.onPopupShowHandler = function() {
        modalInstance = $modal({
          scope: $scope,
          title: 'Add '+ $scope.thingName,
          contentTemplate: 'thingpicker/addThingPopup.html',
          placement: 'center',
          show: true
        });
      };

      $scope.closePopup = function() {
        modalInstance.destroy();
      };
    }
  };
});
