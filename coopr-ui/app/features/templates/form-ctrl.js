/**
 * TemplateFormCtrl
 * handles both "edit" and "create" views
 */

angular.module(PKG.name+'.features').controller('TemplateFormCtrl',
function ($scope, $state, $window, myApi, $q, myHelpers, CrudFormBase, caskFocusManager) {
  CrudFormBase.apply($scope);

  var promise;

  if($scope.editing) {
    $scope.model = myApi.Template.get($state.params);
    promise = $scope.model.$promise;
    promise['catch'](function () { $state.go('404'); });
  }
  else {
    $scope.model = new myApi.Template();
    $scope.model.initialize();

    promise = $q.when($scope.model);
  }


  /*
    collapsible side panel
   */
  $scope.debugJson = {
    visible: !$scope.editing
  };



  /*
    tabs
   */
  $scope.tabs = ['General','Compatibility','Defaults','Constraints'].map(function (t){
    return {title:t, partial:'/assets/features/templates/form-tabs/'+t.toLowerCase()+'.html'};
  });


  $scope.onTabLoaded = function (tabIndex) {
    if(tabIndex === 0 && !$scope.editing) {
      caskFocusManager.focus('inputTemplateName');
    }
  };

  $scope.nextTab = function () {
    $scope.tabs.activeTab++;
    $window.scrollTo(0,0);
  };

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
    angular.forEach(['initial', 'max', 'step'], function (one) {
      $scope.leaseDuration[one] = myHelpers.parseMilliseconds( model.administration.leaseduration[one] || 0 );

      $scope.$watchCollection('leaseDuration.'+one, function (newVal) {
        model.administration.leaseduration[one] = myHelpers.concatMilliseconds(newVal);
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





  /*
    constraints section
   */

  $scope.rmServiceConstraint = function (key) {
    delete $scope.model.constraints.services[key];
  };

  $scope.showServiceConstraintTable = function () {
    return !!getConstrainedServices().length;
  };

  $scope.doAddServiceConstraint = function (key) {
    $scope.model.constraints.services[key] = {
      quantities: {},
      imagetypes: [],
      hardwaretypes: [],
    };
  };


  $scope.addServiceConstraintDd = [];

  $scope.$watchCollection('model.constraints.services', rebuildServiceConstraintDd);
  $scope.$watchCollection('model.compatibility.services', rebuildServiceConstraintDd);




  function getConstrainedServices() {
    try {
      return Object.keys($scope.model.constraints.services);
    } catch(e) {
      return [];
    }
  }


  function rebuildServiceConstraintDd() {
    if(!$scope.model.compatibility) { return; }
    var constrained = getConstrainedServices();
    $scope.addSvcConstraintDd = $scope.model.compatibility.services
      .filter(function(svc) {
        return constrained.indexOf(svc)===-1;
      })
      .map(function (svc) {
        return {
          text: svc,
          click: 'doAddServiceConstraint("'+svc+'")'
        };
      });
  }

});
