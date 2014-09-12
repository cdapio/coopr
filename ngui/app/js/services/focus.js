var module = angular.module(PKG.name+'.services');

/**
 * watched by the myFocus directive, myFocusManager can be called 
 *  from a controller to trigger focus() events, presumably on form inputs
 */

module.service('myFocusManager', function myFocusManagerService ($rootScope, $log) {

  var _last = null;

  this.is = $rootScope.$new(true);

  function _set (k, v) {
    $log.log('[myFocusManager]', v, k);
    this.is[_last] = false;
    this.is[k] = {};
    this.is[k][v] = Date.now();
    _last = k;
  }

  /**
   * triggers focus() on element with my-focus = k
   * @param  {string} k  
   */
  this.focus = function(k) {
    _set.call(this, k, 'focus');
  };

  /**
   * triggers select() on element with my-focus = k
   * @param  {string} k  
   */
  this.select = function(k) {
    _set.call(this, k, 'select');
  };

});
