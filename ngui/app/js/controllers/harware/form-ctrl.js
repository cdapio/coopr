angular.module(PKG.name+'.controllers').controller('HardwareFormCtrl',
function ($scope, $state, $alert, $q, myApi, CrudFormBase) {
  CrudFormBase.apply($scope);

  $scope.allProviders = myApi.Provider.query();
  if($state.includes('*.create')) {
    $scope.editing = false;
    $scope.model = new myApi.HardwareType();
    $scope.allProviders.$promise
      .then(function() {
        $scope.providermapShadow = [];
        $scope.providermapShadow.push({
          flavor: "",
          name: $scope.allProviders[0].name
        });
        debugger;
      });
    $scope.addProviderHandlerOnCreate = function(event) {
      $scope.providermapShadow.push({
        flavor: "",
        name: $scope.allProviders[0].name
      });
      event.preventDefault();
    };
  }
  else {
    $scope.editing = true;
    $scope.model = myApi.HardwareType.get($state.params);
    $scope.model.$promise
      .then(function () {
        window.a = $scope.model;
        $scope.providermapShadow = Object.keys($scope.model.providermap || {})
        .map(function(key) {
          return {
            name: key,
            flavor: $scope.model.providermap[key].flavor,
            disable: "true"
          };
        });
      })
      ['catch'](function() {
        $state.go("404");
      });
      $scope.addProviderHandlerOnEdit = function(event) {
        $scope.providermapShadowNew = $scope.providermapShadowNew || [];
        $scope.providermapShadowNew.push({
          flavor: "",
          name: $scope.allProviders[0].name,
          disable: "false"
        });
        event.preventDefault();
      };
  }


  $scope.doSubmit = function(model) {
    var promise,
        providerArr = $scope.providermapShadow;
    $scope.model.providermap = {};
    angular.forEach(providerArr, function(item) {
      $scope.model.providermap[item.name] = {
        flavor: item.flavor
      };
    });

    if (!$scope.editing) {
      promise = model.$save();
    } else {
      angular.forEach($scope.providermapShadowNew, function(item) {
        $scope.model.providermap[item.name] = {
          flavor: item.flavor
        };
      });
      promise = model.$update();
    }

    promise
      .then(function () {
        $scope.fetchSubnavList();
        $state.go('^.list');
      })
      .finally(function () {
        $scope.submitting = false;
      });
  }
});