/**
 * myDoublerange
 *
 */

angular.module(PKG.name+'.directives').directive('myDoublerange', 
function myDoublerangeDirective () {
  return {
    restrict: 'E',
    templateUrl: 'doublerange/doublerange.html',
    replace: true,
    scope: {
      model: '=',
      inputMax: '=max',
      inputMin: '=min'
    },
    link: function(scope, element, attrs) {

      if(angular.isUndefined(scope.model.max)) {
        scope.model.max = scope.inputMax;
      }

      if(angular.isUndefined(scope.model.min)) {
        scope.model.min = scope.inputMin;
      }

      scope.range = {
        min: scope.model.min,
        max: scope.model.max
      };

      scope.$watchCollection('range', function (newVal) {
        scope.model.min = parseInt(newVal.min, 10);
        scope.model.max = parseInt(newVal.max, 10);
      });

      scope.getConstraint = function (constraint, range) {
        var min = parseInt(scope.inputMin, 10),
            max = parseInt(scope.inputMax, 10);

        if(range === 'min') {

          if(constraint === 'min') {
            return min;
          }
          else { // constraint === 'max'
            return Math.min(max, scope.model.max);
          }

        }
        else {  // range === 'max'

          if(constraint === 'min') {
            return Math.max(min, scope.model.min);
          }
          else { // constraint === 'max'
            return max;
          }

        }
      };

    }

  };
});
