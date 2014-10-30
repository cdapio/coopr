angular.module(PKG.name)
  .config(function ($stateProvider, MYAUTH_ROLE, myHelpers) {

    var crud = myHelpers.crud.mkState,
        abstractSubnav = myHelpers.crud.abstractSubnav;

    /**
     * State Configurations
     */
    $stateProvider



      /*
        /#/tenants/...
       */
      .state(abstractSubnav('Tenant', {
        authorizedRoles: MYAUTH_ROLE.superadmin
      }))
        .state(crud('Tenant', 'list', 'TenantListCtrl'))
        .state(crud('Tenant', 'edit', 'TenantFormCtrl'))
        .state(crud('Tenant', 'create', 'TenantFormCtrl'))



      ;


  });
