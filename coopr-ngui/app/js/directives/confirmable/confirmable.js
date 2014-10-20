/**
 * myConfirmable
 *
 * adds a "myConfirm" method on the scope. call that, and
 *  the expression in "my-confirmable" attribute will be evaluated 
 *  after the user accepts the confirmation dialog. Eg:
 *
 * <a ng-click="myConfirm()" my-confirmable="doDelete(model)">delete</a>
 */

angular.module(PKG.name+'.directives').directive('myConfirmable', 
function myConfirmableDirective ($modal) {
  return {
    restrict: 'A',
    link: function (scope, element, attrs) {

      scope.myConfirm = function () {

        var modal, modalScope;

        modalScope = scope.$new(true);

        modalScope.doConfirm = function() {
          modal.hide();
          scope.$eval(attrs.myConfirmable);
        };

        modal = $modal({
          scope: modalScope,
          template: 'confirmable/confirm-modal.html',
          title: attrs.myConfirmableTitle || 'Confirmation',
          content: attrs.myConfirmableContent || 'Are you sure?',
          placement: 'center', 
          show: true
        });

      };

    }
  };

});
