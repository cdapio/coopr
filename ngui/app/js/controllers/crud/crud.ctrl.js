var module = angular.module(PKG.name+'.controllers');

/**
 * generic list controller
 */
module.controller('CrudListCtrl', function ($scope, $state, $modal) {
  // we already fetched the list in the parent view
  $scope.$watch('subnavList', function (list) {
    if(list) {
      $scope.list = list;
    }
  });

  // but we want it to be fresh
  if(!$scope.subnavList || $scope.subnavList.$resolved) {
    $scope.fetchSubnavList();
  }

  $scope.doDelete = function (model) {
    model.$delete(function () {
      $scope.fetchSubnavList();
    });
  };
});



/**
 * a constructor implementing $scope by the controllers that follow
 */
module.factory('CrudFormBase', function CrudFormBaseFactory ($injector) {
  return function CrudFormBase () {
    var $state = $injector.get('$state'),
        myApi = $injector.get('myApi'),
        scope = this;

    function doThenList(model, method) {
      scope.submitting = true;

      if(!angular.isFunction(model[method]) ) {
        // happens using jsonEdit directive directly on model
        model = new myApi[$state.current.data.modelName](model);
      }

      model[method]()
        .then(function () {
          scope.fetchSubnavList();
          $state.go('^.list');
        })
        .finally(function () {
          scope.submitting = false;
        });
    }

    scope.doSubmit = function (model) {
      doThenList(model, $state.includes('*.create') ? '$save' : '$update');
    };

    scope.doDelete = function (model) {
      doThenList(model, '$delete');
    };

  };
});



/**
 * a controller to edit an existing model
 */
module.controller('CrudEditCtrl', function ($scope, $state, myApi, CrudFormBase) {
  CrudFormBase.apply($scope);

  var data = $state.current.data;
  if(data) {
    $scope.model = myApi[data.modelName].get($state.params);
  }
});


/**
 * a controller to create a new model
 */
module.controller('CrudCreateCtrl', function ($scope, $state, myApi, CrudFormBase) {
  CrudFormBase.apply($scope);

  var data = $state.current.data;
  if(data) {
    $scope.model = new myApi[data.modelName]();
  }
});
