/**
 * myFocusManager
 * watched by the myFocus directive, this service can be called 
 *  from a controller to trigger focus() events, presumably on form inputs
 * @return {Object}  with "focus" and "select" methods
 */

angular.module(PKG.name+'.services').service('myFocusManager', 
function myFocusManagerService ($rootScope, $log) {

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
   * @param  {String} k  
   */
  this.focus = function(k) {
    _set.call(this, k, 'focus');
  };

  /**
   * triggers select() on element with my-focus = k
   * @param  {String} k  
   */
  this.select = function(k) {
    _set.call(this, k, 'select');
  };

});
