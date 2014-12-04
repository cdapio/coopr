/**
 * myStatusLabel
 *
 *  if item.status==='pending' show item.progress.actionstatus
 *  else show item.status
 *
 *  <my-status-label 
 *       data-value="item.status" 
 *       data-display="{
 *         'pending': item.progress.actionstatus
 *       }"
 *     ></my-status-label>
 *
 */

angular.module(PKG.name+'.commons').directive('myStatusLabel', 
function myStatusLabelDirective () {
  return {
    restrict: 'E',
    templateUrl: 'statuslabel/statuslabel.html',
    replace: true,
    scope: {
      status: '=value',
      display: '='
    }
  };
});
