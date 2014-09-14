angular.module(PKG.name+'.controllers').controller('ClusterDetailCtrl', 
  function ($scope, MYSERVICEPICKER_EVENT, CrudFormBase, $state, $modal, myApi, $timeout) {
    CrudFormBase.apply($scope);



    if($state.params.id) {
      $scope.model = myApi.Cluster.get( {id:$state.params.id});
      $scope.model.$promise
        .then(function () {
          if($state.is('clusters.detail.node')) {
            doActionsModal($state.params.nodeId);
          }
        })
        ['catch'](failure);
    }
    else {
      failure();
    }




    var timeoutPromise;

    $scope.$watchCollection('model', function (data) {
      if(!data.$resolved) { return; }

      $scope.availableServices = data.clusterTemplate.compatibility.services
        .filter(function(name) {
          return data.services.indexOf(name)===-1; // filter out services that are already installed
        })
        .map(function(name) { return { name: name }; }); // mimic myApi.Service.query()

      $scope.additionalServices = [];

      if(data.status === 'pending') {
        timeoutPromise = $timeout(update, 1000);
      }


      $scope.serviceSets = data.nodes.reduce(function (memo, node) {
        var services = node.services.map( function (one) {
                         return one.name;
                       }).sort(),
            svcStr = services.join('_');

        if(!memo[svcStr]) {
          memo[svcStr] = {
            services: services,
            nodes: []
          };
        }
        memo[svcStr].nodes.push(node);

        return memo;
      }, {});
    });

    $scope.$on('$destroy', function () {
      $timeout.cancel(timeoutPromise);
    });


    $scope.$on(MYSERVICEPICKER_EVENT.manage, update);



    $scope.doSubmitServices = function (arrSvcs) {
      myApi.ClusterService.save( {clusterId: $scope.model.id}, { services: arrSvcs }, update);
    };



    $scope.doActionsModal = doActionsModal;



    function doActionsModal (nodeId) {
      $state.go('clusters.detail.node', {nodeId: nodeId});

      var modalScope = $scope.$new(true);

      modalScope.node = $scope.model.nodes.filter( function(node) {
        return node.id === nodeId;
      })[0];

      console.log(modalScope.node.actions);

      modalScope.$on('modal.hide', function () {
        $state.go('^');
      });

      $modal({
        title: nodeId,
        contentTemplate: '/partials/clusters/detail-node.html', 
        placement: 'center',
        scope: modalScope,
        show: true
      });
    }


    function failure () {
      $state.go('404');
    }




    function update () {
      $scope.model.$get();
    }

  }
);


