/**
 * myDoublerange
 *
 */
var INTEGER_MAX = 2147483647;

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
      scope.model = scope.model || {};

      scope.html5 = attrs.html5 && attrs.html5!=='false';

      // the max value for the max slider in html5 mode
      scope.maxThreshold = parseInt(attrs.maxThreshold, 10) || 100;

      scope.maxout; // bool or null
      scope.$watch('maxout', function (newVal) {
        if(newVal) {
          var max = parseInt(scope.inputMax, 10);
          scope.model.max = isNaN(max) ? INTEGER_MAX : max;
        }
        else if(!angular.isUndefined(newVal) && scope.html5) {
          scope.model.max = Math.max(
            Math.ceil(scope.maxThreshold/2), 
            scope.model.min+1
          );
        }
      });


      // changes coming in
      scope.$watchCollection('model', function (newVal, oldVal) {
        scope.range = angular.copy(newVal);

        if(newVal && newVal.max) {
          // watch out for max out!
          if(newVal.max < oldVal.max) { 
            if(scope.maxout) {
              scope.maxout = false;
            }
          }
          else if(newVal.max >= scope.maxThreshold) {
            scope.maxout = true;
          }
        }

      });


      // changes going out
      scope.$watchCollection('range', function (newVal) {
        if(scope.model) {
          // hack around input[type=range] getting string values
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
          max = INTEGER_MAX;
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
            if(scope.maxout || !scope.html5) {
              return max;
            }
            else {
              return scope.maxThreshold;
            }
          }
        }
      };

    }

  };
});
