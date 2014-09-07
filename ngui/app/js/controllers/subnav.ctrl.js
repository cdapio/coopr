var module = angular.module(PKG.name+'.controllers');


module.controller('SubnavCtrl', function ($scope, $state, myApi) {

  var path = $state.current.name.split('.')[0];

  function fetchSubnavList () {
    $scope.subnavList = myApi[$state.current.data.modelName]
      .query(function (list) {
        console.log('[fetchSubnavList]', list.length, path);
        $scope.dropdown = list.map(function(item) {
          return {
            text: item.name,
            href: $state.href($state.get(path+'.detail') || $state.get(path+'.edit'), item)
          };
        });
      });
  }

  $scope.fetchSubnavList = fetchSubnavList;

  fetchSubnavList();

});
