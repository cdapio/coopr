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
    $scope.model.compatibility = {
      services: ['base'],
      imagetypes: [],
      hardwaretypes: []
    };
    $scope.model.defaults = {
      config: {},
      services: ['base']
    };


    promise = $q.when($scope.model);
  }



  /*
    tabs
   */
  $scope.tabs = [
    {title: 'General',        partial: 'form-tabs/general.html'},
    {title: 'Compatibility',  partial: 'form-tabs/compatibility.html'},
    {title: 'Defaults',       partial: 'form-tabs/defaults.html'},
    {title: 'Constraints',    partial: 'form-tabs/constraints.html'},
  ];

  $scope.nextTab = function () {
    $scope.tabs.activeTab++;
  }

  $scope.$watch('tabs.activeTab', function (newVal) {
    $state.go($state.includes('**.tab') ? $state.current : '.tab', {tab:newVal});
  });

  $scope.$on('$stateChangeSuccess', function (event, state) {
    var tab = parseInt($state.params.tab, 10) || 0;
    if((tab<0 || tab>=$scope.tabs.length)) {
      tab = 0;
    }
    $scope.tabs.activeTab = tab;
  });




  /*
    leaseDuration
   */
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




  /*
    available resources for the compatibility section
   */
  $scope.availableServices = myApi.Service.query();
  $scope.availableHardware = myApi.HardwareType.query();
  $scope.availableImages = myApi.ImageType.query();

  /*
    and the defaults section
   */
  $scope.availableProviders = myApi.Provider.query();

  /*
    when compatibility changes, ensure defaults are not still set to incompatible options
   */
  angular.forEach(['hardwaretype', 'imagetype', 'service'], function (one) {
    $scope.$watchCollection('model.compatibility.'+one+'s', function (newVal) {
      var d = $scope.model.defaults;
      if(d) {
        if(one === 'service') {
          d.services = d.services.filter(function (svc) {
            return newVal.indexOf(svc)!==-1;
          });
        }
        else {
          if(newVal.indexOf(d[one])<0) {
            d[one] = null;
          }
        }
      }

    });
  });


});