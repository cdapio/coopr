/**
 * ClusterDetailCtrl
 */

angular.module(PKG.name+'.controllers').controller('ClusterDetailCtrl', 
function ($scope, MYSERVICEPICKER_EVENT, CrudFormBase, $state, $modal, $alert, myApi, $timeout, moment) {

  CrudFormBase.apply($scope);


  if($state.params.id) {
    $scope.model = myApi.Cluster.get( {id:$state.params.id});
    $scope.model.$promise
      .then(function (model) {
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


  $scope.$on('$destroy', function () {
    $timeout.cancel(timeoutPromise);
  });


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


    // figure out the max value for lease extension
    var ld = data.clusterTemplate.administration.leaseduration,
        maxDate = moment(data.createTime);
    if(ld.max) {
      maxDate.add(ld.max, 'ms');
    }
    else {
      maxDate.add(100, 'days');
    }
    if(ld.step) {
      maxDate = moment.min(maxDate, moment().add(ld.step, 'ms'));
    }
    $scope.leaseMaxMs = maxDate.valueOf() - Date.now();
  });


  $scope.leaseExtendMs = 0;

  $scope.$watch('leaseExtendMs', function (ms) {
    ms = parseInt(ms, 10); // work around input[type=range] ng-model being a string
    $scope.leaseExtendDate = moment($scope.model.expireTime).add(ms, 'ms').toDate();
    $scope.leaseExtendHumanized = moment.duration(ms, 'ms').humanize();
  });

  $scope.doLeaseExtend = function () {      
    myApi.Cluster.save(
      { id: $scope.model.id }, 
      { expireTime: $scope.leaseExtendDate.valueOf() }, 
      function () {
        $alert({
          title: 'Lease extended until:', 
          content: moment($scope.leaseExtendDate).format('LLL'), 
          type: 'success', 
          duration: 3
        });
        $scope.leaseExtendMs = 0;
        update();
      }
    );
  };



  $scope.$on(MYSERVICEPICKER_EVENT.manage, update);



  $scope.doSubmitServices = function (arrSvcs) {
    myApi.ClusterService.save( 
      { clusterId: $scope.model.id }, 
      { services: arrSvcs }, 
      update
    );
  };



  $scope.doActionsModal = doActionsModal;


  /* ----------------------------------------------------------------------- */


  function doActionsModal (nodeId) {
    $state.go('clusters.detail.node', {nodeId: nodeId});

    var modalScope = $scope.$new(true);

    modalScope.node = $scope.model.nodes.filter( function(node) {
      return node.id === nodeId;
    })[0];

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

});
