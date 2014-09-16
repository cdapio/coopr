var module = angular.module(PKG.name+'.services');

module.factory('myApiPrefix', function ($location, MY_CONFIG) {

  // to work with CORS proxy, we expect that the URI will include a port
  //  ... first we need to remove the protocol from the URI 
  var restPath = MY_CONFIG.COOPR_SERVER_URI.replace(/^(https?:\/\/)?/i, '');
  if(restPath.substr(-1)!=='/') {
    restPath += '/'; // then we ensure it ends with a slash
  }

  /* end result should look something like:

       http :// host.foo.com : 8081 / otherhost.foo.com:55054/v2/
  */
  return $location.protocol() + '://' + $location.host() + 
            ':' + MY_CONFIG.COOPR_CORS_PORT + '/' + restPath;
            
});


module.constant('MYAPI_EVENT', {
  error: 'myapi-error'
});


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

module.config(function ($httpProvider, MYAPI_EVENT) {
  $httpProvider.interceptors.push(function ($q, $timeout, $rootScope, $log, myAuth, myApiPrefix) {
    var isApi = function(url) {
      return url.indexOf(myApiPrefix) === 0;
    };

    return {
     'request': function(config) {
        if(isApi(config.url)) {
          var u = myAuth.currentUser;
          angular.extend(config.headers, {
            'X-Requested-With': angular.version.codeName
          }, u ? {
            'X-Loom-UserID': u.username,
            'X-Loom-TenantID': u.tenant
          } : {});
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
