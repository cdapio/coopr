/**
 * ClusterFormCtrl
 * handles both "reconfigure" and "create" views
 */

angular.module(PKG.name+'.feature.clusters').controller('ClusterFormCtrl',
function ($scope, $state, $q, $alert, CrudFormBase, myApi, caskFocusManager, myHelpers) {
  CrudFormBase.apply($scope);

  var id = $state.params.id;

  $scope.model = new myApi.Cluster({id:id, clusterTemplate:'', numMachines:1});

  $scope.showAdvanced = false;
  $scope.showConfig = !!id;

  var allHardware  = myApi.HardwareType.query(),
      allImages = myApi.ImageType.query(),
      allServices = myApi.Service.query();

  $scope.availableTemplates = myApi.Template.query();
  $scope.availableProviders = myApi.Provider.query();
  $scope.chosenProvider = {};
  $scope.availableHardware = [];
  $scope.availableImages = [];
  $scope.availableServices = [];

  $q.all([
    allHardware.$promise,
    allImages.$promise,
    allServices.$promise,
    $scope.availableProviders.$promise,
    $scope.availableTemplates.$promise
  ]).then(function () {

    $scope.$watch('model.clusterTemplate', function (name) {
      if(!name) {
        return;
      }

      var chosen = $scope.availableTemplates.filter(function (tpl) {
        return tpl.name === name;
      })[0];

      $scope.chosenTemplate = chosen;

      $scope.model.numMachines = Math.min(
        Math.max(
          $scope.model.numMachines,
          chosen.constraints ? chosen.constraints.size.min : 1
        ),
        chosen.constraints ? chosen.constraints.size.max : Infinity
      );

      $scope.availableHardware = allHardware.filter(function (item) {
        return chosen.compatibility.hardwaretypes.indexOf(item.name)>=0;
      });

      $scope.availableImages = allImages.filter(function (item) {
        return chosen.compatibility.imagetypes.indexOf(item.name)>=0;
      });

      $scope.availableServices = allServices.filter(function (item) {
        return chosen.compatibility.services.indexOf(item.name)>=0;
      });


      $scope.leaseDuration = myHelpers.parseMilliseconds(
        chosen.administration.leaseduration.initial
      );

      // set the template defaults on the model
      angular.extend($scope.model, chosen.defaults);
    });


    $scope.$watch('model.provider', function (name) {
      $scope.chosenProvider = $scope.availableProviders.filter(function (p) {
        return p.name === name;
      })[0];
    });

  });



  if(!$scope.editing) {
    /*
    creating a new cluster
     */
    $scope.$watchCollection('leaseDuration', function (timeObj) {
      if(timeObj) {
        var ms = myHelpers.concatMilliseconds(timeObj),
            max = $scope.chosenTemplate.administration.leaseduration.initial;

        if(!max || (ms <= max)) {
          $scope.leaseMaxReached = false;
          $scope.model.initialLeaseDuration = ms;
        }
        else {
          $scope.leaseMaxReached = max;
          $scope.model.initialLeaseDuration = max;
        }

      }
    });


    caskFocusManager.focus('inputClusterName');



  } else {
    /*
    reconfiguring
     */
    myApi.Cluster.get( {id:id}).$promise
      .then(function (data) {
        // what we "get" doesnt look at all like what we will "put"
        // but we still need a populated model for display purposes
        angular.extend($scope.model, {
          name: data.name,
          description: data.description,
          numMachines: data.nodes.length,
          clusterTemplate: data.clusterTemplate.name,
          provider: data.provider.name,
          hardwaretype: data.nodes[0].properties.hardwaretype,
          imagetype: data.nodes[0].properties.imagetype
        });

        $scope.leaseDuration = myHelpers.parseMilliseconds(data.expireTime);

        caskFocusManager.select('inputClusterConfig');
      })
      .catch(function () {
        $state.go('404');
      });



  }



  /*
  updating a cluster means reconfiguring it, so we cannot use CrudFormBase
   */
  $scope.doSubmit = function (model){
    $scope.submitting = true;

    var promise;

    if($scope.editing) { // reconfiguring
      promise = myApi.ClusterConfig.update({clusterId:id}, {
        config: model.config
      }).$promise;
    }
    else { // creating
      promise = model.$save();
    }

    promise
      .then(function () {
        $scope.fetchSubnavList();
        $state.go('^.list');

        $alert({
          title: 'Cluster',
          content: $scope.editing ? 'reconfiguration succeeded!' : 'provisioning request submitted!',
          type: 'success'
        });

      })
      .finally(function () {
        $scope.submitting = false;
      });
  };



});
