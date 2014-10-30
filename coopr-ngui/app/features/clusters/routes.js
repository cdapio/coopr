angular.module(PKG.name)
  .config(function ($stateProvider, MYAUTH_ROLE, MYHELPERS) {

    var crud = MYHELPERS.crud.mkState,
        abstractSubnav = MYHELPERS.crud.abstractSubnav;

    /**
     * State Configurations
     */
    $stateProvider


      /*
        /#/clusters/...
       */

      .state(abstractSubnav('Cluster', {
        authorizedRoles: MYAUTH_ROLE.all
      }))
        .state(crud('Cluster', 'list', 'ClusterListCtrl'))
        .state(crud('Cluster', 'edit', 'ClusterFormCtrl', { title: 'Reconfigure cluster' }))
        .state(crud('Cluster', 'create', 'ClusterFormCtrl', { title: 'Create a cluster' }))
        .state(crud('Cluster', 'detail', 'ClusterDetailCtrl'))
          .state('clusters.detail.node', {
            url: '/node/:nodeId'
          })

      ;


  });
