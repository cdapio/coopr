angular.module(PKG.name)
  .config(function ($stateProvider, MYAUTH_ROLE, myHelpers) {

    var crud = myHelpers.crud.mkState,
        abstractSubnav = myHelpers.crud.abstractSubnav;

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
