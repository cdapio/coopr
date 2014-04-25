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
define([], function () {
  var Page = {

    init: function () {
      var self = this;
      $("#add-action").click(function () {
        var providerEntry = $(".action-entry").clone()[0];
        $(".action-entries").append(providerEntry);
      });

      $("#add-service").click(function (e) {
        self.handleAddServiceEntry(e);
      });

      $("#create-service-form").submit(function (e) {
        e.preventDefault();
        self.getFormDataAndSubmit(e);
      });

      $(".service-delete").click(function (e) {
        $(this).parent().remove();
      });

      $(".service-delete-form").submit(function (e) {
        e.preventDefault();
        Helpers.handleConfirmDeletion(e, '/services');
      });

    },

    handleAddServiceEntry: function (e) {
      var self = this;
      var context = $(e.currentTarget).attr('data-context');
      var valToAdd = $(e.currentTarget).parent().parent().find('select').val();
      if (!valToAdd) {
        return;
      }
      var serviceName = $('<span class="service-name"></span>').text(valToAdd);
      var serviceDelete = $(
        '<span class="service-delete pointer-cursor right-float glyphicon glyphicon-minus"></span>'
      );
      var div = $('<div class="form-control"></div>').append(serviceName).append(serviceDelete);
      $('.service-entries').append(div);
      Helpers.bindDeletion('service-delete');
    },

    getFormDataAndSubmit: function (e) {
      var self = this;
      var postJson = {
        name: $("#inputName").val(),
        description: $("#inputDescription").val(),
        dependencies: {
          provides: [],
          conflicts: [],
          install: {
            requires: [],
            uses: []
          },
          runtime: {
            requires: [],
            uses: []
          }
        },
        provisioner: {
          actions: {}
        }
      };
      var actionEntries = $(".action-entry");
      for (var i = 0; i < actionEntries.length; i++) {
        var configurables = { 
          type: $(actionEntries[i]).find("[name=inputType]").val(),
          script: $(actionEntries[i]).find("[name=inputScript]").val(),
        };
        if ($(actionEntries[i]).find("[name=inputData]").val()) {
          configurables['data'] = $(actionEntries[i]).find("[name=inputData]").val();
        }
        postJson.provisioner.actions[$(actionEntries[i]).find("[name=inputCategory]").val()] = 
          configurables;
          
      }
      var serviceEntries = $(".service-entries .service-name").each(function (index, item) {
        postJson.dependencies.runtime.requires.push($(item).text());
      });
      Helpers.submitPost(e, postJson, '/services');
    }

  };

  return Page.init();

});
