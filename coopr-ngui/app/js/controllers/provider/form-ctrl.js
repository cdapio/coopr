/**
 * Provider form controller for both edit and create views.
 */

angular.module(PKG.name+'.controllers').controller('ProviderFormCtrl', 
function ($scope, $state, $q, myApi, CrudFormBase) {
  CrudFormBase.apply($scope);

  /* type {Array} */
  $scope.providerTypes = myApi.ProviderType.query();

  /* type {Object} */
  $scope.selectedProvider = {};

  if(!$state.includes('*.create')) {

    $scope.editing = true;
    $scope.model = myApi.Provider.get($state.params);
    $scope.model.$promise['catch'](function () { $state.go('404'); });

  } else {

    $scope.model = new myApi.Provider();
    $scope.model.provisioner = {};

    // No need to watch if not editing becuase this field is disabled.
    $scope.$watch('model.providertype', function (newVal) {
      $scope.selectedProvider = findProvider(newVal);
    });

  }

  // Wait for both to complete before setting selected provider and showing fields.
  $q.all([
    $scope.providerTypes.$promise,
    $scope.model.$promise
  ])
  .then(function () {
    $scope.selectedProvider = findProvider($scope.model.providertype);
  });



  function findProvider (pName) {
    var p = $scope.providerTypes.filter(function (item) {
      return item.name === pName;
    });
    return p.length ? p[0] : null;
  }

});