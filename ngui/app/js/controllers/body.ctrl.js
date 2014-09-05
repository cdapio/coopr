var module = angular.module(PKG.name+'.controllers');


module.controller('BodyCtrl', function ($scope) {


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
    $scope.bodyClass = classes.join(' ');
  });



  console.timeEnd(PKG.name);
});