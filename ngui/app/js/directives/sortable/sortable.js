var module = angular.module(PKG.name+'.directives');

module.directive('mySortable', function mySortableDirective ($log) {

  function getPredicate(node) {
    return node.attr('data-predicate') || node.text();
  }

  return {
    restrict: 'A',
    link: function (scope, element, attrs) {

      var headers = element.find('th'),
          defaultPredicate,
          defaultReverse;

      angular.forEach(headers, function(th) {
        th = angular.element(th);
        var a = th.attr('data-predicate-default');
        if(angular.isDefined(a)) {
          defaultPredicate = th;
          defaultReverse = (a==='reverse');
        }
      });

      if(!defaultPredicate) {
        defaultPredicate = headers.eq(0);
      }

      scope.sortable = {
        predicate: getPredicate(defaultPredicate.addClass('predicate')),
        reverse: defaultReverse
      };

      headers.append('<i class="fa fa-toggle-down"></i>');

      headers.on('click', function(event) {
        var th = angular.element(this),
            predicate = getPredicate(th);

        scope.$apply(function() {
          if(scope.sortable.predicate === predicate){
            scope.sortable.reverse = !scope.sortable.reverse;
            th.find('i').toggleClass('fa-flip-vertical');
          }
          else {
            headers.removeClass('predicate');
            headers.find('i').removeClass('fa-flip-vertical');
            scope.sortable = {
              predicate: predicate,
              reverse: false
            };
            th.addClass('predicate');
          }
        });
      });

    }
  };
});
