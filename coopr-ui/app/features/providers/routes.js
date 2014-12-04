angular.module(PKG.name+'.feature.providers')
  .config(function ($stateProvider, MYAUTH_ROLE, CRUD_STATE_HELP) {

    var crud = CRUD_STATE_HELP.mkState,
        abstractSubnav = CRUD_STATE_HELP.abstractSubnav;

    /**
     * State Configurations
     */
    $stateProvider


      /*
        /#/providers/...
       */

      .state(abstractSubnav('Provider', {
        authorizedRoles: MYAUTH_ROLE.admin
      }))
        .state(crud('Provider', 'list', 'CrudListCtrl'))
        .state(crud('Provider', 'edit', 'ProviderFormCtrl'))
        .state(crud('Provider', 'create', 'ProviderFormCtrl'))



      ;


  });
