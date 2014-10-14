/**
 * myFocusManager
 * watched by the myFocus directive, this service can be called 
 *  from a controller to trigger focus() events, presumably on form inputs
 * @return {Object}  with "focus" and "select" methods
 */

angular.module(PKG.name+'.services').service('myFocusManager', 
function myFocusManagerService ($rootScope, $log) {

  var last = null;

  this.is = $rootScope.$new(true);

  function set (k, v) {
    $log.log('[myFocusManager]', v, k);
    var o = {},
        scope = this.is;
    scope.$apply(function() {
      scope[last] = false;
      o[v] = Date.now();
      scope[k] = o;
      last = k;
    });
  }

  /**
   * triggers focus() on element with my-focus = k
   * @param  {String} k  
   */
  this.focus = function(k) {
    set.call(this, k, 'focus');
  };

  /**
   * triggers select() on element with my-focus = k
   * @param  {String} k  
   */
  this.select = function(k) {
    set.call(this, k, 'select');
  };

});
