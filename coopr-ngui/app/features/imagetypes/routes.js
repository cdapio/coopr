angular.module(PKG.name)
  .config(function ($stateProvider, MYAUTH_ROLE, myHelpers) {

    var crud = myHelpers.crud.mkState,
        abstractSubnav = myHelpers.crud.abstractSubnav;

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
