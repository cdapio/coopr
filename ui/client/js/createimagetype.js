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
      $("#add-provider").click(function () {
        var providerEntry = $(".provider-entry").clone()[0];
        providerEntry = Helpers.clearValues(providerEntry);
        $(".provider-entries").append(providerEntry);
      });

      $("#create-imagetype-form").submit(function (e) {
        e.preventDefault();
        self.getFormDataAndSubmit(e);
      });

      $(".imagetype-delete-form").submit(function (e) {
        e.preventDefault();
        Helpers.handleConfirmDeletion(e, '/imagetypes');
      });

    },

    getFormDataAndSubmit: function (e) {
      var self = this;
      var postJson = {
        name: $("#inputName").val(),
        description: $("#inputDescription").val(),
        providermap: {}
      };

      $(".provider-entry").each(function() {
        var entry = $(this),
            image = entry.find("[name=inputImage]").val(),
            provider = entry.find("[name=inputProvider]").val();
        if (image && provider) {
          postJson.providermap[provider] = { 
            image: image,
            sshuser: entry.find("[name=inputSshuser]").val(),
          };
        }
      })
      for (var i = 0; i < providerEntries.length; i++) {

      }
      Helpers.submitPost(e, postJson, '/imagetypes');
    }

  };

  return Page.init();
});
