/**
 * [TODO] Add some description.
 */

var module = angular.module(PKG.name+'.directives');

module.directive('myStatusLabel', function myStatusLabelDirective () {
  return {
    restrict: 'E',
    templateUrl: 'statuslabel/statuslabel.html',
    replace: true,
    scope: {
      status: '=value',
      display: '='
    }
  };
});
