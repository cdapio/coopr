/**
 * SubnavCtrl
 */

angular.module(PKG.name+'.feature.crud').controller('SubnavCtrl',
function ($scope, $state, myApi) {

  var path = $state.current.name.split('.')[0],
      modelName = $state.current.data.modelName;

  $scope.fetchSubnavList = fetchSubnavList;

  fetchSubnavList();

  /* ----------------------------------------------------------------------- */

  function fetchSubnavList () {

    $scope.subnavList = myApi[modelName].query(function (list) {
      $scope.dropdown = list.map(function (item) {
        return {
          text: item.name,
          href: $state.href($state.get(path+'.detail') || $state.get(path+'.edit'), item)
        };
      });
    });
  }

});
