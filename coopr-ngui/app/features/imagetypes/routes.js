angular.module(PKG.name+'.feature.imagetypes')
  .config(function ($stateProvider, MYAUTH_ROLE, CRUD_STATE_HELP) {

    var crud = CRUD_STATE_HELP.mkState,
        abstractSubnav = CRUD_STATE_HELP.abstractSubnav;

    /**
     * State Configurations
     */
    $stateProvider


      /*
        /#/imagetypes/...
       */

      .state(abstractSubnav('ImageType', {
        title: 'Images',
        ddLabel: 'Image Types',
        authorizedRoles: MYAUTH_ROLE.admin
      }))
        .state(crud('ImageType', 'list', 'CrudListCtrl'))
        .state(crud('ImageType', 'edit', 'ImageFormCtrl'))
        .state(crud('ImageType', 'create', 'ImageFormCtrl'))



      ;


  });
