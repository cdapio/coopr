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

      if(scope.html5) {

        // the almost-max value for the max slider in html5 mode
        scope.maxThreshold = parseInt(attrs.maxThreshold, 10) || 100;

        // the real max value
        function maxoutValue() {
          var max = parseInt(scope.inputMax, 10);
          return isNaN(max) ? INTEGER_MAX : max;
        } 

        // the checkbox to go beyond the threshold
        scope.maxout = (maxoutValue() == scope.model.max) || null; 

        scope.$watch('maxout', function (newVal) {
          if(newVal === null) {
            return; 
          }
          if(newVal) { // checkbox checked
            scope.model.max = maxoutValue();
          }
          else { // checkbox unchecked
            scope.model.max = Math.max(
              Math.ceil(scope.maxThreshold/2), 
              scope.model.min+1
            );
          }
        });

        scope.$watch('model.max', function (newVal, oldVal) {
          if(newVal < oldVal) { 
            if(scope.maxout) {
              scope.maxout = false;
            }
          }
          else if(attrs.maxThreshold) {
            if(newVal >= scope.maxThreshold) {
              scope.maxout = true;
            }
          }
        });

      }


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
            if(!scope.html5 || !attrs.maxThreshold || scope.maxout) {
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
