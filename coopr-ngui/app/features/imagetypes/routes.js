angular.module(PKG.name+'.features')
  .config(function ($stateProvider, MYAUTH_ROLE, MYHELPERS) {

    var crud = MYHELPERS.crud.mkState,
        abstractSubnav = MYHELPERS.crud.abstractSubnav;

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
