angular.module(PKG.name+'.feature.templates')
  .config(function ($stateProvider, MYAUTH_ROLE, MYHELPERS) {

    var crud = MYHELPERS.crud.mkState,
        abstractSubnav = MYHELPERS.crud.abstractSubnav;

    /**
     * State Configurations
     */
    $stateProvider


      /*
        /#/templates/...
       */

      .state(abstractSubnav('Template', {
        title: 'Catalog',
        ddLabel: 'Cluster Templates',
        authorizedRoles: MYAUTH_ROLE.admin
      }))
        .state(crud('Template', 'list', 'CrudListCtrl'))
        .state(crud('Template', 'edit', 'TemplateFormCtrl'))
          .state('templates.edit.tab', {
            url: '/tab/:tab'
          })
        .state(crud('Template', 'create', 'TemplateFormCtrl'))
          .state('templates.create.tab', {
            url: '/tab/:tab'
          })



      ;


  });
