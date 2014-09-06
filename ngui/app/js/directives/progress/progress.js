var module = angular.module(PKG.name+'.directives');

module.directive('myProgress', function myProgressDirective () {
  return {
    restrict: 'E',
    templateUrl: function(element, attrs) {
      return 'progress/'+ attrs.type.split(' ')[0] +'.tpl';
    },
    replace: true,
    scope: {
      value: '=',
      max: '=' 
    },
    link: function(scope, element, attrs) {
      var additionalClasses = attrs.type.split(' ').slice(1).map(function(cls) {
            return 'progress-bar-'+cls;
          });

      scope.$watch('value', function(newVal) {
        var max = parseInt(scope.max, 10) || 100;

        scope.percent = Math.floor((newVal / max) * 100);

        var cls = {
          'active progress-bar-striped': (newVal < max),
          'progress-bar': true
        };

        angular.forEach(additionalClasses, function(add) {
          cls[add] = true;
        });

        scope.cls = cls;
      });
    }

  };
});
