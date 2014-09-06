var module = angular.module(PKG.name+'.directives');

module.directive('myProgress', function myProgressDirective () {
  return {
    restrict: 'E',
    templateUrl: function(element, attrs) {
      // there could be other representations of progress than a "bar"...
      var type = (attrs.type ? attrs.type.split(' ')[0] : 'bar');
      return 'progress/'+ type +'.tpl';
    },
    replace: true,
    scope: {
      value: '=',
      max: '=' 
    },
    link: function(scope, element, attrs) {
      var additionalClasses = attrs.type.split(' ').slice(1).map(function(cls) {
            return cls && 'progress-bar-'+cls;
          });

      scope.$watch('value', function(newVal) {
        var max = parseInt(scope.max, 10) || 100;

        scope.percent = Math.floor((newVal / max) * 100);

        var cls = {
          'active progress-bar-striped': (newVal < max),
          'progress-bar': true
        };

        angular.forEach(additionalClasses, function(add) {
          if(add) {
            cls[add] = true;
          }
        });

        scope.cls = cls;
      });
    }

  };
});
