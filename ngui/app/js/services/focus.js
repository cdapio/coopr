/**
 * Watched by the myFocus directive, myFocusManager can be called from a controller to trigger
 * focus() events, presumably on form inputs.
 */
var module = angular.module(PKG.name+'.services');

module.service('myFocusManager', function myFocusManagerService ($rootScope, $log) {

  var last = null;

  this.is = $rootScope.$new(true);

  function set (k, v) {
    $log.log('[myFocusManager]', v, k);
    this.is[last] = false;
    this.is[k] = {};
    this.is[k][v] = Date.now();
    last = k;
  }

  /**
   * Triggers focus() on element with my-focus = k.
   * @param {string} k  
   */
  this.focus = function(k) {
    set.call(this, k, 'focus');
  };

  /**
   * Triggers select() on element with my-focus = k.
   * @param {string} k  
   */
  this.select = function(k) {
    set.call(this, k, 'select');
  };

});
