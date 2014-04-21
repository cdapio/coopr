/**
 * Copyright 2012-2014, Continuuity, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Helpers/common functions for Loom frontend.
 */

var Helpers = Helpers || {};

/**
 * Interval between calls.
 * @type {Number}
 */
Helpers.CALL_INTERVAL = 2000;

/**
 * Interval between tablesort retires.
 */
Helpers.TABLESORT_RETRIES = 500;

/**
 * Max retries for table sorting.
 */
Helpers.TABLESORT_RETRIES = 10;

/**
 * JSON error.
 * @type {String}
 */
Helpers.JSON_ERR = 'Invalid config JSON';

/**
 * Friendly status message mappings.
 */
Helpers.FRIENDLY_STATUS = {
  'COMPLETE': 'Completed',
  'FAILED': 'Failed',
  'NOT_SUBMITTED': 'Pending...',
  'CREATING': 'Building cluster...',
  'RUNNING': 'Running...'
};

Helpers.READABLE_ACTIONS = {
  CLUSTER_CREATE: "Creation",
  CLUSTER_DELETE: "Deletion",
  SOLVE_LAYOUT: "Solve layout",
  CLUSTER_EXPIRE: "Deletion on expiry",
  CLUSTER_CONFIGURE: "Configure",
  CLUSTER_CONFIGURE_WITH_RESTART: "Configure and restart",
  STOP_SERVICES: "Stop services",
  RESTART_SERVICES: "Restart services",
  START_SERVICES: "Start services",
  ADD_SERVICES: "Add services"
};

/**
 * Submits a post request.
 * @param  {Object|String} e Jquery submit event containing form data or url submit location.
 * @param  {String} redirectUrl Url for redirection.
 */
Helpers.submitPost = function (e, postJson, redirectUrl, overrideCallback) {
  var submitUrl;
  if (typeof(e) === 'object') {
    submitUrl = e.currentTarget.action;
  } else {
    submitUrl = e;
  }
  $("#notification").text('');
  var postBody, redirectLocation;
  if (typeof(arguments[1]) === 'object') {
    postBody = arguments[1];
  } else {
    postBody = '';
  }
  if (typeof(arguments[2]) === 'string') {
    redirectLocation = arguments[2];
  } else {
    redirectLocation = arguments[1];
  }
  $.post(submitUrl, postBody)
    .done(function (resp) {
      if (overrideCallback && typeof(overrideCallback) === 'function') {
        overrideCallback(resp);
      } else {
        if (resp === 'OK') {
          window.location.href = redirectLocation;
        } else {
          var errorMessage = '';
          if (resp) {
            errorMessage = resp;
          } else {
            errorMessage = 'Request unsuccessful';
          }
          $("#notification").text(errorMessage);
          $("html, body").animate({ scrollTop: 0 }, "slow");
        }
      }
    })
    .fail(function (error) {
      if (overrideCallback && typeof(overrideCallback) === 'function') {
        overrideCallback(error);
      } else {
        var errorMessage = '';
        if (error.statusText) {
          errorMessage = error.statusText;
        } else {
          errorMessage = 'Request unsuccessful';
        }
        $("#notification").text(errorMessage);
        $("html, body").animate({ scrollTop: 0 }, "slow");
      }
    });
};

Helpers.submitGet = function (url, successCallback, errorCallback) {
  $("#notification").text('');
  $.get(url)
    .done(function (resp) {
      successCallback(resp);
    })
    .fail(function (error) {
      if (errorCallback && typeof(errorCallback) === 'function') {
        errorCallback(errorCallback);
      } else {
        var errorMessage = '';
        if (error.statusText) {
          errorMessage = error.statusText;
        } else {
          errorMessage = 'Request unsuccessful';
        }
        $("#notification").text(errorMessage);
        $("html, body").animate({ scrollTop: 0 }, "slow");
      }
    });
};

/**
 * Clear out values for all form subfields.
 * @param  {Object} el HTML element with input boxes.
 * @return {Object} parent HTML element.
 */
Helpers.clearValues = function (el) {
  $(el).find('input').val('').end();
  return el;
};

/**
 * Tests for valid JSON. Based on Douglas Crockford's implementation here:
 * https://github.com/douglascrockford/JSON-js/blob/master/json2.js
 * @param  {String} input Input string to test for JSON validity.
 * @return {Boolean} If JSON is valid.
 */
Helpers.isValidJSON = function (input) {
  try {
    JSON.parse(input);
    return true;
  } catch (e) {
    return false;
  }
};

/**
 * Gets class name based on status text.
 * @param  {String} statusText.
 * @return {String} class name.
 */
Helpers.getClassByStatus = function (statusText) {
  switch(statusText) {
    case 'FAILED':
      return 'text-danger';
    case 'COMPLETE':
      return 'text-success';
    case 'IN_PROGRESS':
      return 'text-info';
    default:
      return '';    
  }
};

/**
 * Deletes parent node upon click.
 * @param  {String} className identifier.
 */
Helpers.bindDeletion = function (className) {
  $('.' + className).unbind().click(function() {
    $(this).parent().remove();
  });
};

/**
 * Manages a confirm deletion dialog.
 */
Helpers.handleConfirmDeletion = function (e, redirectUrl) {
  var message = '<div class="row"><div class="col-sm-2 delete-trashcan">' +
    '</div><div class="col-sm-10 modal-text">This action is not reversible, are you sure you want' +
    ' to delete?</div></div>';
  bootbox.dialog({
    /**
     * Message shown inside the body of delete dialog.
     */
    message: $(message),
    
    /**
     * Adds a header to the dialog and places this text in an h4.
     */
    title: "Confirm deletion",
    
    /**
     * Additional class to apply to the dialog wrapper.
     */
    className: "my-modal",
    
    /**
     * Any buttons shown in the dialog's footer.
     */
    buttons: {
      "Cancel": {
        className: "btn-spl btn-default"
      },
      "Delete": {
        className: "btn-spl btn-danger action-submit-delete",
        callback: function() {
          Helpers.submitPost(e, redirectUrl);
        }
      }
    }
  });
};

/**
 * Enables table sorting for given set of tables. This checks if the table has rendered and keeps
 * trying to sort until it has become avaialble. It can use either Angular's $interval or the native
 * js setInterval. It retries a predetermined number of times within a given interval.
 * @param $interval AngularJS interval service.
 */
Helpers.enableTableSorting = function ($interval) {
  var self = this;
  var intervalService;
  var numTries = 0, retries = self.TABLESORT_RETRIES;
  if ($interval) {
    intervalService = $interval;
  } else {
    intervalService = setInterval;
  }
  var waitSort = intervalService(function () {
    numTries++;
    if (numTries <= retries) {
      if ($(".tablesorter").length) {
        $(".tablesorter").each(function(index, item) {
          var headers = {}
          $(item).find('th.no-sort').each(function(i,el){
            headers[$(this).index()] = { sorter: false };
          });
          $(item).tablesorter({
            headers: headers
          });
        });
        $interval.cancel(waitSort);
      }
    }
  }, self.TABLESORT_RETRY_INTERVAL);
};

/**
 * Makes timestamp human readable.
 * @param  {Number} timestamp.
 * @return {String} human readable time string.
 */
Helpers.prettifyTimestamp = function (timestamp) {
  var dt = new Date(timestamp);
  return dt.toISOString().replace(/T/, ' ').replace(/\..+/, '');
};

/**
 * Stringifies milliseconds into human readable format i.e.
 * 6000 => 6 seconds.
 * @param  {Number} milliseconds.
 * @return {String} Human readable time.
 */
Helpers.stringifyTime = function (milliseconds) {
  function numberEnding (number) {
      return (number > 1) ? 's' : '';
  }
  var timeStr = '';
  var temp = milliseconds / 1000;
  var years = Math.floor(temp / 31536000);
  if (years) {
    timeStr += years.toFixed(0) + ' year' + numberEnding(years);
  }
  var days = Math.floor((temp %= 31536000) / 86400);
  if (days) {
    timeStr += ' ' + days.toFixed(0) + ' day' + numberEnding(days);
  }
  var hours = Math.floor((temp %= 86400) / 3600);
  if (hours) {
    timeStr += ' ' + hours.toFixed(0) + ' hour' + numberEnding(hours);
  }
  var minutes = Math.floor((temp %= 3600) / 60);
  if (minutes) {
    timeStr += ' ' + minutes.toFixed(0) + ' minute' + numberEnding(minutes);
  }
  var seconds = temp % 60;
  if (seconds) {
    timeStr += ' ' + seconds.toFixed(0) + ' second' + numberEnding(seconds);
  }
  return timeStr || 'less then a second';
};

/**
 * Parses milliseconds and converts to days, hours and minutes.
 * @param  {Number} milliseconds.
 * @return {Object} containing days, hours and minutes as keys.
 */
Helpers.parseMilliseconds = function (milliseconds) {
  var timeObj = {
    days: 0,
    hours: 0,
    minutes: 0
  };
  var temp = milliseconds / 1000;
  var days = Math.floor((temp %= 31536000) / 86400);
  if (days) {
    timeObj['days'] = days.toFixed(0);
  }
  var hours = Math.floor((temp %= 86400) / 3600);
  if (hours) {
    timeObj['hours'] = hours.toFixed(0);
  }
  var minutes = Math.floor((temp %= 3600) / 60);
  if (minutes) {
    timeObj['minutes'] = minutes.toFixed(0);
  }
  return timeObj;
};

/**
 * Get milliseconds from time object.
 * @param  {Object} timeObj with days, hours and mins as keys.
 * @return {Number} milliseconds. 
 */
Helpers.concatMilliseconds = function (timeObj) {
  var total = 0;
  if ('days' in timeObj) {
    total += timeObj['days'] * 86400000;
  }
  if ('hours' in timeObj) {
    total += timeObj['hours'] * 3600000;
  }
  if ('minutes' in timeObj) {
    total += timeObj['minutes'] * 60000;
  }
  return total;
};

/**
 * Escapes HTML for display.
 * @param  {String} unsafe string with HTML tags.
 * @return {String} with escaped HTML tags.
 */
Helpers.escapeHtml = function(str) {
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
 }

/**
 * Adds item to collection if it doesn't already exist.
 * @param  {String|Number} item.
 * @param  {Array} collection.
 * @return {Array} collection.
 */
Helpers.checkAndAdd = function (item, collection) {
  if (!collection) {
    return [item.slice(0)];
  }
  if (item && collection.indexOf(item) === -1) {
    collection.push(item.slice(0));
  }
  return collection;
};

/**
 * Removes item from collection if it exists. Depends on angular's implementation of equals, use
 * only inside of an angular app.
 * @param  {String|Array|Number} item.
 * @param  {Array} collection.
 * @return {Array} collection.
 */
Helpers.checkAndRemove = function (item, collection) {
  for (var i = 0; i < collection.length; i++) {
    if (angular.equals(item, collection[i])) {
      collection.splice(i, 1);
    }
  }
  return collection;
};

/**
 * Time object to string representation.
 * @param  {Object} timeObj containing days, hours and minutes.
 * @return {String} description of time obj in human readable format.
 */
Helpers.timeToStr = function (timeObj) {
  var strTime = '';

  // Set explicit order of keys.
  var keys = ["days", "hours", "minutes"];

  for (var i = 0; i < keys.length; i++) {
    if (keys[i] in timeObj) {
      if (timeObj[keys[i]]) {
        strTime += timeObj[keys[i]] + ' ' + keys[i];
      }
    }
  }
  return strTime;
};
