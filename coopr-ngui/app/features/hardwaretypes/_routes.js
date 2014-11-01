angular.module(PKG.name+'.feature.hardwaretypes')
  .config(function ($stateProvider, MYAUTH_ROLE, CRUD_STATE_HELP) {

    var crud = CRUD_STATE_HELP.mkState,
        abstractSubnav = CRUD_STATE_HELP.abstractSubnav;

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
