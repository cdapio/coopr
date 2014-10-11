angular.module(PKG.name+'.controllers').controller('ServiceFormCtrl',
function ($scope, $state, $q, myApi, CrudFormBase) {
  CrudFormBase.apply($scope);


  if($scope.editing) {
    $scope.model = myApi.Service.get($state.params);
    $scope.model.$promise['catch'](function () { $state.go('404'); });
  }
  else { // creating
    $scope.model = new myApi.Service();
    $scope.model.initialize();
  }


  /*
    collapsible side panel
   */
  $scope.debugJson = {
    visible: !$scope.editing
  };


  $q.all({
    model: $scope.model.$promise || $scope.model,
    services: myApi.Service.query().$promise,
    automators: myApi.AutomatorType.query().$promise
  })
  .then(function (r) {
    $scope.availableServices = r.services.filter(function(svc) {
      return svc.name !== r.model.name;
    });
  });


  $scope.$watchCollection('model.provisioner.actions', function (newVal) {
    if(newVal) {
      $scope.actionDropdown = [
        'install', 'remove', 'initialize', 'configure', 'start', 'stop'
      ].filter(function (category) {
          return !$scope.model.provisioner.actions[category];
        })
        .map(function (category) {
          return {
            text: category,
            click: 'addAction("'+category+'")'
          }
        });
    }
  });

  $scope.addAction = function (category) {
    $scope.model.provisioner.actions[category] = {fixme: true};
  };

  $scope.rmAction = function (category) {
    delete $scope.model.provisioner.actions[category];
  };

});
