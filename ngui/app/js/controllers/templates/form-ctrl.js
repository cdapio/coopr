/**
 * TemplateFormCtrl
 * handles both "edit" and "create" views
 */

angular.module(PKG.name+'.controllers').controller('TemplateFormCtrl', 
function ($scope, $state, myApi, $q, myHelpers, CrudFormBase) {
  CrudFormBase.apply($scope);

  var promise;

  if($scope.editing) {
    $scope.model = myApi.Template.get($state.params);
    promise = $scope.model.$promise;
    promise['catch'](function () { $state.go('404'); });
  }
  else {
    $scope.model = new myApi.Template();
    promise = $q.when($scope.model);
  }

  $scope.tabs = [
    {title: 'General',        partial: 'form-tabs/general.html'},
    {title: 'Compatibility',  partial: 'form-tabs/compatibility.html'},
    {title: 'Defaults',       partial: 'form-tabs/defaults.html'},
    {title: 'Constraints',    partial: 'form-tabs/constraints.html'},
  ];
  $scope.tabs.activeTab = 0;

  $scope.nextTab = function () {
    $scope.tabs.activeTab = Math.min($scope.tabs.activeTab+1, $scope.tabs.length);
  }

  $scope.leaseDuration = {};

  promise.then(function (model) {
    model.administration = model.administration || {leaseduration:{}};

    angular.forEach(['initial', 'max', 'step'], function (type) {
      $scope.leaseDuration[type] = myHelpers.parseMilliseconds( model.administration.leaseduration[type] || 0 );

      $scope.$watchCollection('leaseDuration.'+type, function (newVal) {
        model.administration.leaseduration[type] = myHelpers.concatMilliseconds(newVal);
      });
    });


  });


});