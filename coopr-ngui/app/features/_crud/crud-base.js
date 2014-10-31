var module = angular.module(PKG.name+'.feature.crud');



/**
 * CrudListBase
 * a base to be extended by list controllers
 */
module.factory('CrudListBase', function CrudListBaseFactory ($injector) {
  return function CrudListBase () {
    var $state = $injector.get('$state'),
        $alert = $injector.get('$alert'),
        scope = this;

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

        $alert({
          title: $state.current.data.modelName,
          content: 'delete succeeded!',
          type: 'success'
        });

      });
    };

  };
});



/**
 * CrudFormBase
 * a base to be extended by CrudEditCtrl and CrudCreateCtrl
 */
module.factory('CrudFormBase', function CrudFormBaseFactory ($injector) {
  return function CrudFormBase () {
    var $state = $injector.get('$state'),
        $alert = $injector.get('$alert'),
        myApi = $injector.get('myApi'),
        editing = !$state.current.name.match(/\.create/),
        scope = this;

    scope.editing = editing;

    scope.doSubmit = function (model) {
      doThenList(model, editing ? '$update' : '$save');
    };

    scope.doDelete = function (model) {
      doThenList(model, '$delete');
    };

    /* ----------------------------------------------------------------------- */

    function doThenList(model, method) {
      scope.submitting = true;

      if(!angular.isFunction(model[method]) ) {
        // happens using jsonEdit directive directly on model
        model = new myApi[$state.current.data.modelName](model);
      }

      model[method]()
        .then(function () {
          scope.fetchSubnavList();
          $state.go($state.current.name.split('.')[0] + '.list');

          $alert({
            title: $state.current.data.modelName,
            content: method.substring(1) + ' succeeded!',
            type: 'success'
          });

        })
        .finally(function () {
          scope.submitting = false;
        });
    }


  };
});

