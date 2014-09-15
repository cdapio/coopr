var module = angular.module(PKG.name+'.directives');

module.directive('myFocus', function myFocusDirective ($timeout, myFocusManager) {
  return {

    restrict: 'A',

    link: function (scope, element, attrs) {

      attrs.$observe('myFocus', function (newVal) {
        // console.log('[myFocus] watching', newVal);

        var cleanup = myFocusManager.is.$watch(newVal, function (o) {
          if(o && (o.focus || o.select)) {
            $timeout(function() {
              element[0][o.focus?'focus':'select']();
            });
          }
        });

        scope.$on('$destroy', function() {
          // console.log('[myFocus] cleanup', newVal);
          cleanup();
        });
      });

    }
  };
});

