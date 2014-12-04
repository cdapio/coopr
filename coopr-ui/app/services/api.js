var module = angular.module(PKG.name+'.services');

module.value('myApiVersion', 2);

module.factory('myApiPrefix', function myApiPrefixFactory ($location, myApiVersion) {
  return $location.protocol() + '://' + $location.host() +
            ':' + $location.port() + '/proxy/v' + myApiVersion + '/';
});


module.constant('MYAPI_EVENT', {
  error: 'myapi-error'
});


/**
 * myApi
 * wraps all calls to the REST API
 * @return {Object} with one property xxxx per myApi_xxxx file
 */
module.factory('myApi', function(
    myApi_clusters,
    myApi_hardwaretypes,
    myApi_imagetypes,
    myApi_providers,
    myApi_provisioners,
    myApi_services,
    myApi_templates,
    myApi_tenants,
    myApi_importexport
  ){

  return angular.extend({},
    myApi_clusters,
    myApi_hardwaretypes,
    myApi_imagetypes,
    myApi_providers,
    myApi_provisioners,
    myApi_services,
    myApi_templates,
    myApi_tenants,
    myApi_importexport
  );

});

module.config(function ($httpProvider, MYAPI_EVENT, MY_CONFIG) {
  $httpProvider.interceptors.push(function ($q, $timeout, $rootScope, $log, myAuth, myApiPrefix) {
    var isApi = function(url) {
      return url.indexOf(myApiPrefix) === 0;
    };

    return {
     'request': function(config) {
        if(isApi(config.url)) {

          angular.extend(config.headers, {
            'X-Requested-With': angular.version.codeName
          });

          if(myAuth.currentUser) {
            angular.extend(config.headers, {
              'Coopr-UserID': myAuth.currentUser.username,
              'Coopr-TenantID': myAuth.currentUser.tenant
            });
          }

          if(MY_CONFIG.authorization) {
            angular.extend(config.headers, {
              'Authorization': MY_CONFIG.authorization
            });
          }

          $log.log('[myApi]', config.method, config.url.substr(myApiPrefix.length));
        }
        return config;
      },

     'responseError': function(rejection) {
        if(isApi(rejection.config.url)) {
          $rootScope.$broadcast(MYAPI_EVENT.error, rejection);
        }
        return $q.reject(rejection);
      }

    };
  });
});
