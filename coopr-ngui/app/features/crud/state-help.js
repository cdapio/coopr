angular.module(PKG.name+'.feature.crud').constant('CRUD_STATE_HELP', {


  /**
   * create an abstract state object by assuming defaults
   * @param  {String} name capitalized name of the model eg 'Cluster'
   * @param  {Object} data optional overrides
   * @return {Object}      state object
   */
  abstractSubnav: function (name, data) {
    var plural = name + 's',
        stateName = plural.toLowerCase();
    return {
      name: stateName,
      abstract: true,
      templateUrl: '/assets/features/crud/subnav.html',
      controller: 'SubnavCtrl',
      url: '/' + stateName,
      data: angular.extend({
        title: plural,
        ddLabel: plural,
        modelName: name
      }, data || {})
    };
  },


  /**
   * create a CRUD state object by assuming defaults
   * @param  {String} name capitalized name of the model eg 'Cluster'
   * @param  {String} action eg 'edit' or 'list'
   * @param  {String} ctrl controller to use eg 'CrudEditCtrl'
   * @param  {Object} data optional overrides
   * @return {Object}      state object
   */
  mkState: function crud (name, action, ctrl, data) {
    var path = name.toLowerCase() + 's',
        tpl = '/assets/features/' + path + '/',
        url = '';
    switch(action) {
      case 'create':
        url = '/create';
        /* falls through */
      case 'edit':
        tpl += 'form.html';
        /* falls through */
      default:
        url = url || '/' + action + (name.match(/Cluster|Provisioner/) ? '/:id' : '/:name');
        if(action.match(/create|edit/)) {
          break;
        }
        /* falls through */
      case 'list':
        tpl += action + '.html';
    }
    if(!ctrl) {
      tpl = '/assets/features/crud/json.html';
      ctrl = 'Crud' + action.substr(0,1).toUpperCase() + action.substr(1) + 'Ctrl';
    }
    return {
      name: path+'.'+action,
      url: url,
      templateUrl: tpl,
      controller: ctrl,
      data: angular.extend({
        title: name + ' ' + action
      }, data || {})
    };
  }

});


