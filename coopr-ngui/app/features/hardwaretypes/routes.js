angular.module(PKG.name)
  .config(function ($stateProvider, MYAUTH_ROLE, MYHELPERS) {

    var crud = MYHELPERS.crud.mkState,
        abstractSubnav = MYHELPERS.crud.abstractSubnav;

    /**
     * State Configurations
     */
    $stateProvider


      /*
        /#/hardwaretypes/...
       */

      .state(abstractSubnav('HardwareType', {
        title: 'Hardware',
        ddLabel: 'Hardware Types',
        authorizedRoles: MYAUTH_ROLE.admin
      }))
        .state(crud('HardwareType', 'list', 'CrudListCtrl'))
        .state(crud('HardwareType', 'edit', 'HardwareFormCtrl'))
        .state(crud('HardwareType', 'create', 'HardwareFormCtrl'))


      ;


  });
