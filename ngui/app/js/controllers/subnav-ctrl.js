/**
 * SubnavCtrl
 */

angular.module(PKG.name+'.controllers').controller('SubnavCtrl', 
function ($scope, $state, myApi) {

  var path = $state.current.name.split('.')[0],
      modelName = $state.current.data.modelName;

  function fetchSubnavList () {
    $scope.subnavList = myApi[modelName].query(function (list) {
      $scope.dropdown = list
        .filter(function (item) {
          switch (modelName) {
            case 'Cluster':
              return item.status!=='terminated';
            default:
              return true;
          }
        })
        .map(function (item) {
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
