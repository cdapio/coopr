/**
 * Provider form controller for both edit and create views.
 */

angular.module(PKG.name+'.features').controller('ProviderFormCtrl',
function ($scope, $state, $q, myApi, CrudFormBase, caskFocusManager) {
  CrudFormBase.apply($scope);

  /* type {Array} */
  $scope.providerTypes = myApi.ProviderType.query();

  /* type {Object} */
  $scope.selectedProvider = {};

  if($scope.editing) {

    $scope.model = myApi.Provider.get($state.params);
    $scope.model.$promise['catch'](function () { $state.go('404'); });

  } else {

    $scope.model = new myApi.Provider();
    $scope.model.initialize();

    // No need to watch if not editing becuase this field is disabled.
    $scope.$watch('model.providertype', function (newVal) {
      $scope.selectedProvider = findProvider(newVal);
    });

    caskFocusManager.focus('inputProviderName');
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
