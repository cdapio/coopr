angular.module(PKG.name+'.features').controller('ServiceFormCtrl',
function ($scope, $state, $q, myApi, CrudFormBase, caskFocusManager) {
  CrudFormBase.apply($scope);


  if($scope.editing) {
    $scope.model = myApi.Service.get($state.params);
    $scope.model.$promise['catch'](function () { $state.go('404'); });
  }
  else { // creating
    $scope.model = new myApi.Service();
    $scope.model.initialize();
    caskFocusManager.focus('inputServiceName');
  }


  /*
    collapsible side panel
   */
  $scope.debugJson = {
    visible: false // !$scope.editing
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

    $scope.availableAutomators = r.automators;
    $scope.automatorConfig = {};
    r.automators.forEach(function (a) {
      $scope.automatorConfig[a.name] = a.parameters.admin;
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
          };
        });
    }
  });


  $scope.addAction = function (category) {
    $scope.model.provisioner.actions[category] = {
      type: $scope.availableAutomators[0].name,
      fields: {}
    };
    caskFocusManager.focus('inputServiceAutomator_'+category);
  };

  $scope.rmAction = function (category) {
    delete $scope.model.provisioner.actions[category];
  };

});
