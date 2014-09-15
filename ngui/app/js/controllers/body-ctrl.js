/**
 * Controller for body. [TODO]
 */
var module = angular.module(PKG.name+'.controllers');

module.controller('BodyCtrl', function ($scope, myTheme, MYTHEME_EVENT) {

  var activeThemeClass = myTheme.getClassName();

  $scope.$on(MYTHEME_EVENT.changed, function (event, newClassName) {
    if(!event.defaultPrevented) {
      $scope.bodyClass = $scope.bodyClass.replace(activeThemeClass, newClassName);
      activeThemeClass = newClassName;
    }
  });


  $scope.$on('$stateChangeSuccess', function (event, state) {
    var classes = [];
    if(state.data && state.data.bodyClass) {
      classes = [state.data.bodyClass];
    }
    else {
      var parts = state.name.split('.'),
          count = parts.length + 1;
      while (1<count--) {
        classes.push('state-' + parts.slice(0,count).join('-'));
      }
    }

    classes.push(activeThemeClass);

    $scope.bodyClass = classes.join(' ');
  });

  console.timeEnd(PKG.name);
});