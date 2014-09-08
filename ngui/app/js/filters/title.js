var module = angular.module(PKG.name+'.filters');


module.filter('myTitleFilter', function myTitleFilter () {

  return function(state) {
    var title = state.data && state.data.title;
    return (title ? title + ' | ' : '') + 'Coopr';
  };

});