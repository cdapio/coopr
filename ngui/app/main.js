console.time(PKG.name);

angular
  .module(PKG.name, [

    angular.module(PKG.name+'.services', [
      'ngResource',
      'ngStorage'
    ]).name,

    angular.module(PKG.name+'.filters', [
      PKG.name+'.services'
    ]).name,

    angular.module(PKG.name+'.controllers', [
      PKG.name+'.services',
      PKG.name+'.filters',
      'mgcrea.ngStrap.alert',
      'angular-loading-bar',
      'ui.router'
    ]).name,

    angular.module(PKG.name+'.directives', [
      PKG.name+'.services',
      PKG.name+'.filters',
      'mgcrea.ngStrap.alert',
      'mgcrea.ngStrap.tooltip',
      'mgcrea.ngStrap.popover',
      'mgcrea.ngStrap.dropdown',
      'mgcrea.ngStrap.modal'
    ]).name,

    'angular-loading-bar',
    'angularMoment',
    'ngAnimate',
    'ngSanitize',
    'ui.router'
  ])

  .run(function ($rootScope, $state, $stateParams) {
    // It's very handy to add references to $state and $stateParams to the $rootScope
    // so that you can access them from any scope within your applications.For example,
    // <li ng-class="{ active: $state.includes('contacts.list') }"> will set the <li>
    // to active whenever 'contacts.list' or one of its decendents is active.
    $rootScope.$state = $state;
    $rootScope.$stateParams = $stateParams;
  })




  .config(function ($httpProvider) {
    var delay = window.location.search.match(/delay=(\d+)/);
    if(delay) {
      delay = parseInt(delay[1], 10);
      console.log('HTTP interceptor will delay responses for', delay, 'ms');
      $httpProvider.interceptors.push(function ($q, $timeout) {
        return {
          response: function(data) {
            var defer = $q.defer();
            $timeout(function () {
              defer.resolve(data);
            }, delay);
            return defer.promise;
          }
        };
      });
    }

  })


  .config(function ($alertProvider) {
    angular.extend($alertProvider.defaults, {
      animation: 'am-fade-and-scale',
      container: '#alerts > .container'
    });
  })

  .config(function ($compileProvider) {
    $compileProvider.aHrefSanitizationWhitelist(
      /^\s*(https?|ftp|mailto|tel|file|blob):/
    );
  })

  .config(function (cfpLoadingBarProvider) {
    cfpLoadingBarProvider.includeSpinner = false;
  })

  .run(function ($rootScope, $alert, MYAPI_EVENT){
    $rootScope.$on(MYAPI_EVENT.error, function (event, rejection) {
      $alert({title:'API error '+rejection.status, content:rejection.data, type:'danger', duration:3});
    });
  });
