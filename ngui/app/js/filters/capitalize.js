var module = angular.module(PKG.name+'.filters');


module.filter('myCapitalizeFilter', function myCapitalizeFilter () {

  return function(input) {
    input = input ? input.toLowerCase() : '';
    return input.substr(0,1).toUpperCase() + input.substr(1);
  };

});
