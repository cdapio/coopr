/**
 * myThingPicker
 */

angular.module(PKG.name+'.commons').directive('myThingPicker',
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
      freetextMode: '@'
    },

    controller: function ($scope, myApi, $modal, caskFocusManager) {

      var modalInstance,
          modalScope;

      if ($scope.freetextMode) {
        modalScope = $scope.$new();
        modalScope.newThing = {
          name: ''
        };

        modalInstance = $modal({
          scope: modalScope,
          title: 'Add '+ $scope.thingName,
          template: 'thingpicker/freetext-modal.html',
          placement: 'center',
          show: false
        });

        $scope.showModal = modalInstance.show;

        modalScope.$watch('newThing.name', function() {
          modalScope.modalError = false;
        });

        modalScope.$on('modal.show', function() {
          caskFocusManager.focus('inputNewThingName_'+$scope.thingName);
        });

        modalScope.$on('modal.hide', function() {
          modalScope.newThing.name = '';
        });

        modalScope.validateAndAddToModel = function(name) {
          if ($scope.model.indexOf(name) === -1) {
            $scope.addThing(name);
            modalInstance.hide();
          } else {
            modalScope.modalError = true;
          }
        };

        $scope.$on('$destroy', function() {
          modalInstance.destroy();
        });
      }

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
