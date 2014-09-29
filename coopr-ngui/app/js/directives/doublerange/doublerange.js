/**
 * myDoublerange
 *
 */

angular.module(PKG.name+'.directives').directive('myDoublerange', 
function myDoublerangeDirective ($log) {
  return {
    restrict: 'E',
    templateUrl: 'doublerange/doublerange.html',
    replace: true,
    scope: {
      model: '=',
      inputMin: '=min',
      inputMax: '=max'
    },
    link: function(scope, element, attrs) {

      scope.html5 = attrs.html5 && attrs.html5!=='false';

      // hack around input[type=range] getting string values
      scope.$watchCollection('model', function (newVal) {
        scope.range = angular.copy(newVal);
      });

      // in the model we need numbers, with defaults
      scope.$watchCollection('range', function (newVal) {
        if(scope.model) {
          var min = parseInt(newVal.min, 10),
              max = parseInt(newVal.max, 10);
          if(!isNaN(min)) {
            scope.model.min = min;
          }
          if(!isNaN(max)) {
            scope.model.max = max;
          }
        }
      });

      scope.getConstraint = function (constraint, range) {
        var min = parseInt(scope.inputMin, 10),
            max = parseInt(scope.inputMax, 10);

        if(isNaN(min)) {
          min = 0;
        }
        if(isNaN(max)) {
          max = 2147483647;
        }

        if(range === 'min') {
          if(constraint === 'min') {
            return min;
          }
          else { // constraint === 'max'
            return Math.min(max, scope.model ? scope.model.max : max);
          }
        }
        else {  // range === 'max'
          if(constraint === 'min') {
            return Math.max(min, scope.model ? scope.model.min : min);
          }
          else { // constraint === 'max'
            return max;
          }
        }
      };

    }

  };
});
