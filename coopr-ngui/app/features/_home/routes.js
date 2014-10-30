angular.module(PKG.name)
  .config(function ($stateProvider, $urlRouterProvider) {


    /**
     * Redirects and Otherwise
     */
    $urlRouterProvider
      .otherwise(function($injector, $location){
        $injector.get('$state').go($location.path() ? '404' : 'home');
      });


    /**
     * State Configurations
     */
    $stateProvider

      .state('home', {
        url: '/',
        templateUrl: '/assets/features/_home/home.html',
        controller: 'HomeCtrl'
      })

      .state('404', {
        templateUrl: '/assets/features/_home/404.html'
      })

      ;


  });
