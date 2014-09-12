var module = angular.module(PKG.name+'.directives');

module.directive('myProviderFields', function myProviderFieldsDirective () {
  return {
    restrict: 'E',
    templateUrl: 'providerfields/providerfields.tpl',
    scope: {
      model: '=',
      provider: '='
    },
    controller: function ($scope, myApi) {

      var allTypes, typeReqs = [];

      myApi.ProviderType.query(function (result) {
        allTypes = result;
        $scope.$watch('provider', setFields);
        $scope.$watchCollection('model', setRequired);
      });

      function setFields () {
        var fields = {},
            model = {},
            providerType = allTypes.filter(function (type) {
              return type.name === $scope.provider.providertype;
            })[0];

        typeReqs = [];

        angular.forEach(['user', 'admin'], function (role) {
          var p = providerType && providerType.parameters[role];
          if(p) {
            if(p.required) {
              typeReqs = typeReqs.concat(p.required);
            }
            angular.forEach(p.fields, function (val, key) {
              if(role==='user' || val.override) {
                fields[key] = val;
              }
            });
          }
        });

        $scope.fields = fields;

        angular.forEach(fields, function (field, key) {
          if(field.default) {
            model[key] = field.default;
          }
        });

        $scope.model = model;

      }


      function setRequired (newVal) {
        var out = {};

        angular.forEach(newVal, function (val, key) {
          if(val && !out[key]) {
            angular.forEach(typeReqs, function (set) {
              if(set.indexOf(key)>=0) {
                angular.forEach(set, function (want) {
                  out[want] = true;
                });
              }
            });
          }
        });

        $scope.required = out;
      }

    }
  };
});
