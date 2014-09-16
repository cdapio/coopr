var module = angular.module(PKG.name+'.directives');

module.directive('myPassword', function myPasswordDirective (myFocusManager) {
  return {
    restrict: 'E',
    templateUrl: 'password/click2show.tpl',
    replace: true,
    scope: {
      value: '='
    },
    link: function(scope, element, attrs) {

      scope.uid = ['myPassword', Date.now(), Math.random().toString().substr(2)].join('_');

      scope.doToggle = function() {
        var show = !scope.show;
        scope.show = show;
        if (show) {
          myFocusManager.select(scope.uid);          
        }
      };

    }

  };
});
