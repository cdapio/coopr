/**
 * jsontoformfields directive.
 *
 * This takes json of this format and converts them into form fields bound to a provided object.
 * !! This directive modifies bindobject passed to it from the parent.
 *
 * Expected json:
 * fields: {
 *   <fieldname>: {
 *     label: string,
 *     override: boolean,
 *     tip: string,
 *     type: string
 *   }
 * },
 * required: []
 *   [<fieldname>, <fieldname>]
 * }
 *
 * <div my-jsontoformfields fieldsconfig="json" bindobject="model.fields"/>
 */

angular.module(PKG.name+'.directives').directive('myJsontoformfields', 
function myJsontoformfieldsDirective () {
  return {
    restrict: 'AE',
    replace: true,
    scope: {
      fieldsconfig: '=',
      bindobject: '='
    },
    templateUrl: 'jsontoformfields/jsontoformfields.html',
    link: function (scope, element, attrs) {

      scope.bindProvided = false;

      scope.$watch('fieldsconfig', function (newVal, oldVal) {

        if (!angular.equals(newVal, oldVal)) {
          if (scope.fieldsconfig && typeof scope.fieldsconfig !== 'object') {
            scope.fieldsconfig = angular.fromJson(scope.fieldsconfig);
          }

          if (!scope.fieldsconfig.hasOwnProperty('fields')) {
            throw Error('fields not declared in json for fieldsconfig');
          }

          setDefaults();
        }

      });

      scope.$watch('bindobject', function (newVal, oldVal) {
        if (!scope.bindProvided && !angular.equals(newVal, oldVal) && newVal) {  
          scope.bindProvided = true;  
        }
        setDefaults();
      });

      scope.$watchCollection(['bindobject', 'fieldsconfig'], function (newValues, oldValues) {
        if (!angular.equals(newValues, oldValues)) {
          setRequired(newValues[0]);
        }
      });

      function setDefaults () {        
        if (!scope.fieldsconfig || !scope.bindobject) {
          return;
        }
        angular.forEach(scope.fieldsconfig.fields, function (field, key) {
          if (field.hasOwnProperty('default') && !scope.bindobject[key]) {
            scope.bindobject[key] = field.default;
          }
        });
      }

      function setRequired (newVal) {
        var out = {};

        angular.forEach(newVal, function (val, key) {
          if(val && !out[key]) {
            angular.forEach(scope.fieldsconfig.required, function (set) {
              if(set.indexOf(key)>=0) {
                angular.forEach(set, function (want) {
                  out[want] = true;
                });
              }
            });
          }
        });

        scope.required = out;
      }

    }
  };

});
