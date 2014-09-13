/**
 * CRUD controller. [TODO]
 */
var module = angular.module(PKG.name+'.controllers');

/**
 * A constructor implementing a generic list controller.
 */
module.factory('CrudListBase', function CrudListBaseFactory() {
  return function CrudListBase () {
    var scope = this;

    // we already fetched the list in the parent view
    scope.$watch('subnavList', function (list) {
      if(list) {
        scope.list = list;
      }
    });

    // but we want it to be fresh
    if(!scope.subnavList || scope.subnavList.$resolved) {
      scope.fetchSubnavList();
    }

    scope.doDelete = function (model) {
      model.$delete(function () {
        scope.fetchSubnavList();
      });
    };

  };
});


/**
 * Generic list controller.
 */
module.controller('CrudListCtrl', function ($scope, CrudListBase) {
  CrudListBase.apply($scope);
});


/**
 * A constructor implementing $scope by the controllers that follow. 
 * [TODO: Should be removed and put in a separate file]
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
 * A controller to edit an existing model. [TODO: Make separate file]
 */
module.controller('CrudEditCtrl', function ($scope, $state, myApi, CrudFormBase) {
  CrudFormBase.apply($scope);

  var data = $state.current.data,
      failure = function () { $state.go('404'); };

  if(data) {
    $scope.model = myApi[data.modelName].get($state.params);
    $scope.model.$promise.catch(failure);
  }
  else {
    failure();
  }
});


/**
 * A controller to create an existing model. [TODO: Make separate file].
 */
module.controller('CrudCreateCtrl', function ($scope, $state, myApi, CrudFormBase) {
  CrudFormBase.apply($scope);

  var data = $state.current.data;
  if(data) {
    $scope.model = new myApi[data.modelName]();
  }
});