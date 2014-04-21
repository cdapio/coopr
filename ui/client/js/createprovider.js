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

    providerFields: {
      rackspace: [
        "rackspace_username",
        "rackspace_api_key",
        "rackspace_region"
      ],
      joyent: [
        "joyent_username",
        "joyent_keyname",
        "joyent_keyfile",
        "joyent_api_url",
        "joyent_version"
      ],
      openstack: [
        "openstack_username",
        "openstack_password",
        "openstack_tenant",
        "openstack_auth_url",
        "openstack_ssh_key_id",
        "identity_file"
      ]
    },

    init: function () {
      var self = this;
      $("#provisioner-select").change(function () {
        self.handleProviderType($(this).val());
      });

      $("#create-provider-form").submit(function (e) {
        e.preventDefault();
        self.getFormDataAndSubmit(e);
      });

      $(".provider-delete-form").submit(function (e) {
        e.preventDefault();
        Helpers.handleConfirmDeletion(e, '/providers');
      });

    },

    handleProviderType: function (provider) {
      $(".auth-group").each(function (index, group) {
        if ($(group).attr("id") === provider + "-auth-fields") {
          $(group).show();
        } else {
          $(group).hide();
        }
      })
    },

    getFormDataAndSubmit: function (e) {
      var self = this;
      var providerType = $("#provisioner-select").val();
      var postJson = {
        name: $("#inputName").val(),
        description: $("#inputDescription").val(),
        providertype: providerType,
        provisioner: {
          auth: {}
        }
      };
      if (providerType in self.providerFields) {
        for (var i = 0; i < self.providerFields[providerType].length; i++) {
          var key = self.providerFields[providerType][i];
          postJson.provisioner.auth[key] = $("#" + providerType + "-auth-fields #" + key).val();
        }
        Helpers.submitPost(e, postJson, '/providers');
      } else {
        $("#notification").text('Provider type empty.');
        $("html, body").animate({ scrollTop: 0 }, "slow");
      }
    }

  };

  return Page.init();

});
