angular.module(PKG.name)
  .config(function ($stateProvider, MYAUTH_ROLE, myHelpers) {

    var crud = myHelpers.crud.mkState,
        abstractSubnav = myHelpers.crud.abstractSubnav;

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
