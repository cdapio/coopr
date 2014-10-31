angular.module(PKG.name+'.features')
  .config(function ($stateProvider, MYAUTH_ROLE, MYHELPERS) {

    var crud = MYHELPERS.crud.mkState,
        abstractSubnav = MYHELPERS.crud.abstractSubnav;

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
