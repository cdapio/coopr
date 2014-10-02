angular.module(PKG.name+'.controllers').controller('HardwareFormCtrl',
function ($scope, $state, $alert, $q, myApi, CrudFormBase) {
  CrudFormBase.apply($scope);

  $scope.allProviders = myApi.Provider.query();
  $scope.providersDropDown = [];


  if($scope.editing) {
    $scope.model = myApi.HardwareType.get($state.params);
    $scope.model.$promise['catch'](function () { $state.go('404'); });
  }
  else { // creating
    $scope.model = new myApi.HardwareType();
    angular.extend($scope.model, {
      providermap: {}
    });
  }

  function buildProviderDropDown () {
    if(!$scope.allProviders.$resolved || !$scope.model.providermap) {
      return;
    }
    $scope.providersDropDown = $scope.allProviders
      .filter(function (provider) {
        return Object.keys($scope.model.providermap).indexOf(provider.name)===-1;
      })
      .map(function (provider) {
        return {
          text: provider.name,
          click: 'addProvider("'+provider.name+'")'
        };
      });
  }

  $scope.allProviders.$promise.then(buildProviderDropDown);

  $scope.$watchCollection('model.providermap', buildProviderDropDown);

  $scope.rmProvider = function(pName) {
    delete $scope.model.providermap[pName];
  };

  $scope.addProvider = function (pName) {
    if(!$scope.model) { return; }

    $scope.model.providermap[pName] = {
      name: pName
    };

  };


});
