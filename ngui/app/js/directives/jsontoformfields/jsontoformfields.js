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
 * <div my-jsontoformfields fieldsjson="json" bindobject="model.fields"/>
 */

angular.module(PKG.name+'.directives').directive('myJsontoformfields', 
function myJsontoformfieldsDirective () {
  return {
    restrict: 'AE',
    replace: true,
    scope: {
      fieldsjson: '=',
      bindobject: '='
    },
    templateUrl: 'jsontoformfields/jsontoformfields.html',
    link: function (scope, element, attrs) {

      scope.bindProvided = false;

      scope.$watch('fieldsjson', function (newVal, oldVal) {

        if (!angular.equals(newVal, oldVal)) {
          if (scope.fieldsjson && typeof scope.fieldsjson !== 'object') {
            scope.fieldsjson = angular.fromJson(scope.fieldsjson);
          }

          if (!scope.fieldsjson.hasOwnProperty('fields')) {
            throw Error('fields not declared in json for fieldsjson');
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

      scope.$watchCollection(['bindobject', 'fieldsjson'], function (newValues, oldValues) {
        if (!angular.equals(newValues, oldValues)) {
          setRequired(newValues[0]);
        }
      });

      function setDefaults () {        
        if (!scope.fieldsjson || !scope.bindobject) {
          return;
        }
        angular.forEach(scope.fieldsjson.fields, function (field, key) {
          if (field.hasOwnProperty('default') && !scope.bindobject[key]) {
            scope.bindobject[key] = field.default;
          }
        });
      }

      function setRequired (newVal) {
        var out = {};

        angular.forEach(newVal, function (val, key) {
          if(val && !out[key]) {
            angular.forEach(scope.fieldsjson.required, function (set) {
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
