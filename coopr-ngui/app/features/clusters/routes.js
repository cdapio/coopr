angular.module(PKG.name+'.feature.clusters')
  .config(function ($stateProvider, MYAUTH_ROLE, CRUD_STATE_HELP) {

    var crud = CRUD_STATE_HELP.mkState,
        abstractSubnav = CRUD_STATE_HELP.abstractSubnav;

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
