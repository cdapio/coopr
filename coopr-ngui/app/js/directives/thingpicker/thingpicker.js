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
      thingName: '@'
    },

    controller: function ($scope, myApi) {

      $scope.rmThing = function (thing) {
        $scope.model = $scope.model.filter(function (one) {
          return one !== thing;
        });
      };

      $scope.addThing = function (thing) {
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


    }
  };
});
