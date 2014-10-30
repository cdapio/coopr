/**
 * various utility functions
 */
angular.module(PKG.name+'.services').constant('myHelpers', {

  /**
   * Parses milliseconds and converts to days, hours and minutes.
   * @param  {Number} milliseconds.
   * @return {Object} containing days, hours and minutes as keys.
   */
  parseMilliseconds: function parseMilliseconds (milliseconds) {
    var temp = milliseconds / 1000;
    return {
      days: Math.floor((temp %= 31536000) / 86400),
      hours: Math.floor((temp %= 86400) / 3600),
      minutes: Math.floor((temp %= 3600) / 60)
    };
  },

  /**
   * Get milliseconds from time object.
   * @param  {Object} timeObj with days, hours and mins as keys.
   * @return {Number} milliseconds. 
   */
  concatMilliseconds: function concatMilliseconds (timeObj) {
    var total = 0;
    if ('days' in timeObj) {
      total += timeObj.days * 86400;
    }
    if ('hours' in timeObj) {
      total += timeObj.hours * 3600;
    }
    if ('minutes' in timeObj) {
      total += timeObj.minutes * 60;
    }
    return total * 1000;
  },



  crud: {

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
        templateUrl: '/assets/features/_crud/subnav.html',
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
        tpl = '/assets/features/_crud/json.html';
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


  }


});
