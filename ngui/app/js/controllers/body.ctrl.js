var module = angular.module(PKG.name+'.controllers');


module.controller('BodyCtrl', function ($scope) {

  $scope.bodyDemoObj = {
    'title': 'Holy bodyAlertObj!',
    'content': 'Lorem ipsum dolor sit amet, consectetur adipisicing elit.',
    'type': 'info'
  };

  console.timeEnd(PKG.name);
});