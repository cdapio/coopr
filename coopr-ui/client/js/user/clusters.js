/**
 * Copyright © 2012-2014 Cask Data, Inc.
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
define(["../generic-form-control", "../base_page"],
  function (GenericFormControl, BasePage) {
  var Page = {

    clusters: $(".cluster-entry"),
    activeClusters: $("#active-clusters .cluster-entry"),

    init: function () {
      var self = this;

      self.setStatusTimers();
      $(".delete-cluster-header").click(function () {
        $("#deleted-clusters").toggle();
        $(".toggle-delete-cluster").toggleClass('open');
      });
    },

    setStatusTimers: function () {
      var self = this;

      for (var i = 0; i < self.clusters.length; i++) {
          self.getClusterStatus(self.clusters[i]);
      }
      self.interval = setInterval(function () {
        for (var i = 0; i < self.activeClusters.length; i++) {
          self.getClusterStatus(self.activeClusters[i]);
        }
      }, Helpers.CALL_INTERVAL);
    },

    getClusterStatus: function (clusterEl) {
      var self = this;
      var clusterId = $(clusterEl).attr('id');
      var clusterUrl = '/user/clusters/status/' + clusterId;
      Helpers.submitGet(clusterUrl,
        function (data) {
          if (Helpers.isValidJSON(data)) {
            data = JSON.parse(data);
            var statusColor = Helpers.getClassByStatus(data.actionstatus);
            var progressPercent = data.stepscompleted * 100 / data.stepstotal;
            var stepHtml;
            if (!isNaN(progressPercent)) {
              stepHtml = $('<span>' + progressPercent.toFixed(0) + '% </span>');
            } else {
              stepHtml = $('<span>Not available</span>');
            } 
            $("#" + data.clusterid).find('.cluster-progress').html(stepHtml);
            $("#" + data.clusterid).find('.progress-bar').css(
              {width: progressPercent + '%'}).addClass(statusColor);
            var statusHtml = $("<p></p>");
            var actionHtml =
              '<span class="lowercasify">' + Helpers.FRIENDLY_STATUS[data.actionstatus] + '</span>';
            statusHtml
              .html(Helpers.READABLE_ACTIONS[data.action] + ' ' + actionHtml)
              .addClass(statusColor);
            $("#" + data.clusterid).find('.cluster-status').html(statusHtml);
          }
        },
        function (error) {
          // Pass to prevent showing error notification for failed calls.
        }
      );
    }

  };

  return Page.init();
});
