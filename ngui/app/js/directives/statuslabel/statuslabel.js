var module = angular.module(PKG.name+'.directives');

module.directive('myStatusLabel', function myStatusLabelDirective () {
  return {
    restrict: 'E',
    templateUrl: 'statuslabel/statuslabel.tpl',
    replace: true,
    scope: {
      status: '=value'
    }
  };
});
