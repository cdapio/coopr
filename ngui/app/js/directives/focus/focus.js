var module = angular.module(PKG.name+'.directives');

module.directive('myFocus', function myFocusDirective ($timeout, myFocusManager) {
  return {

    restrict: 'A',

    link: function (scope, element, attrs) {

      var cleanup = myFocusManager.is.$watch(attrs.myFocus, function (o) {
        if(o && (o.focus || o.select)) {
          $timeout(function() {
            element[0][o.focus?'focus':'select']();
          });
        }
      });

      scope.$on('$destroy', cleanup);
    }
  };
});

