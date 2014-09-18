/**
 * myFileReader
 * Prompt for a local file and read it as text
 * @return {Object}  an object with a "get" method
 */

angular.module(PKG.name+'.services').service('myFileReader', 
function myFileReaderService ($q, $document, $window, $log) {

  var input = angular.element('<input type="file" class="sr-only" />');
  $document.find('body').append(input);

  var deferred;

  input.on('change', function() {
    var val = input[0].files;
    if(val && val.length && deferred) {
      var reader, file = val[0];
      $log.log('[myFileReader]', file);
      if(file.size > 1048576) { // 1mb max
        return deferred.reject(file.name + ' is too big');
      }
      reader = new $window.FileReader();
      reader.onload = function () {
        deferred.resolve(reader);
      };
      reader.onerror = deferred.reject;
      reader.onprogress = deferred.notify;
      reader.readAsText(file);
    }
  });

  /**
   * trigger a filepicker
   * @param  {Function} callback optional callback
   * @return {Promise}  resolved with the content of file
   */
  this.get = function (callback) {
    deferred = $q.defer();

    if(!$window.FileReader) {
      deferred.reject('FileReader not available');
    }
    else { // trigger the file picker
      input.val(null);
      input[0].focus();
      input[0].click();
    }

    return deferred.promise.then(callback || angular.identity);
  };



});
