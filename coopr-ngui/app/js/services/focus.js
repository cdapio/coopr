/**
 * myFocusManager
 * watched by the myFocus directive, this service can be called 
 *  from a controller to trigger focus() events, presumably on form inputs
 * @return {Object}  with "focus" and "select" methods
 */

angular.module(PKG.name+'.services').service('myFocusManager', 
function myFocusManagerService ($rootScope, $log, $timeout) {

  var last = null;

  this.is = $rootScope.$new(true);

  function set (k, v) {
    var scope = this.is;
    $timeout(function() {
      $log.log('[myFocusManager]', v, k);
      scope[last] = false;
      var o = {};
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
