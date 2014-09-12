var module = angular.module(PKG.name+'.directives');

module.directive('myConfirmable', function myConfirmableDirective ($window, $modal) {
  return {
    restrict: 'A',
    link: function (scope, element, attrs) {

      function confirmed() {
        scope.$eval(attrs.myConfirmable);
      }

      scope.myConfirm = function () {

        // TODO: replace with a nice modal

        if($window.confirm('Are you sure?')) {
          confirmed();
        }

      };

    }
  };

});
