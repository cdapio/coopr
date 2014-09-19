/**
 * Provider form controller for both edit and create views.
 */

angular.module(PKG.name+'.controllers').controller('ProviderFormCtrl', 
function ($scope, $state, $alert, $q, myApi, CrudFormBase) {
  CrudFormBase.apply($scope);
  $scope.editing = !$state.includes('*.create');

  /* type {Array} */
  $scope.providerTypes = myApi.ProviderType.query();

  /* type {Object} */
  $scope.selectedProvider = {};

  if($scope.editing) {
    $scope.model = myApi.Provider.get($state.params);
    $scope.model.$promise['catch'](function () { $state.go('404'); });

  } else {
    $scope.model = new myApi.Provider();
    $scope.model.provisioner = {};

    // No need to watch if not editing becuase this field is disabled.
    $scope.$watch('model.providertype', function (newVal, oldVal) {
      $scope.providerTypes.forEach(function (item) {
        if (item.name === $scope.model.providertype) {
          $scope.selectedProvider = item;
        }
      });
    });
  }


  // Wait for both to complete before setting selected provider and showing fields.
  $q.all([
    $scope.providerTypes.$promise,
    $scope.model.$promise
  ])
  .then(function (result) {
    var selectedProvider = {};
    result[0].forEach(function (item) {
      if (item.name === $scope.model.providertype) {
        $scope.selectedProvider = item;
      }
    });
  });



});