angular.module(PKG.name)
  .config(function ($stateProvider, MYAUTH_ROLE, MYHELPERS) {

    var crud = MYHELPERS.crud.mkState,
        abstractSubnav = MYHELPERS.crud.abstractSubnav;

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
