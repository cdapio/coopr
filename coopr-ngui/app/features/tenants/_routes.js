angular.module(PKG.name+'.feature.tenants')
  .config(function ($stateProvider, MYAUTH_ROLE, CRUD_STATE_HELP) {

    var crud = CRUD_STATE_HELP.mkState,
        abstractSubnav = CRUD_STATE_HELP.abstractSubnav;

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
