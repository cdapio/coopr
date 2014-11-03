angular.module(PKG.name+'.feature.services')
  .config(function ($stateProvider, MYAUTH_ROLE, CRUD_STATE_HELP) {

    var crud = CRUD_STATE_HELP.mkState,
        abstractSubnav = CRUD_STATE_HELP.abstractSubnav;

    /**
     * State Configurations
     */
    $stateProvider


      /*
        /#/services/...
       */
      .state(abstractSubnav('Service', {
        authorizedRoles: MYAUTH_ROLE.admin
      }))
        .state(crud('Service', 'list', 'CrudListCtrl'))
        .state(crud('Service', 'edit', 'ServiceFormCtrl'))
        .state(crud('Service', 'create', 'ServiceFormCtrl'))



      ;


  });
