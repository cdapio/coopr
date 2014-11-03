/**
 * myConfirmable
 *
 * adds a "myConfirm" method on the scope. call that, and
 *  the expression in "my-confirmable" attribute will be evaluated 
 *  after the user accepts the confirmation dialog. Eg:
 *
 * <a ng-click="myConfirm()" 
 *       my-confirmable="doDelete(model)"
 *       data-confirmable-title="Hold on..."
 *       data-confirmable-content="Are you absolutely sure?"
 * >delete</a>
 */

angular.module(PKG.name+'.commons').directive('myConfirmable', 
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
          title: attrs.confirmableTitle || 'Confirmation',
          content: attrs.confirmableContent || 'Are you sure?',
          placement: 'center', 
          show: true
        });

      };

    }
  };

});
