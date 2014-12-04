..
   Copyright © 2012-2014 Cask Data, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
 
       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

:orphan:

.. plugin-resources-reference:

.. index::
   single: Plugin Resources

================
Plugin Resources
================

.. include:: /guide/admin/admin-links.rst

There is out of the box support for creating Hadoop clusters. Internally, the installation, configuration, and management of Hadoop
services is handled on the provisioner by the Chef Automator plugin. The Chef Automator plugin uses out of the box cookbooks to 
execute all tasks it needs to perform. However, administrators may wish to manage other sorts of clusters using other sorts of 
services. For example, you may want to create an Elastic Search template to create an indexing and search cluster. 
In order to allow simple and easy expansion of service and template capabilities, plugins are able to define different
types of resources they can use to perform tasks. For example, the Chef Automator plugin defines 3 types of resources: 
cookbooks, data bags, and roles. We include several cookbooks out of the box, but the plugin resource system allows administrators to
add and manage any number of cookbooks, enabling support of any service desired. Similarly, plugin resources can be used on the 
provider plugin side to upload and manage tenant specific data, such as user keys or credentials. 


Overview
========

Plugin resources can only be managed by the administrator of a tenant.  As with all other entities, plugin resources are isolated to within
a tenant. Users from one tenant cannot see plugin resources in another tenant. The basic flow is that an administrator uploads resources
as needed. Each time a resource is uploaded, a new version of it is created. These resources are not available for use until the administrator
stages a specific version of the resource, and then performs a sync to push all staged versions from the server to provisioners. If a resource
needs to be removed, it can be recalled, which means it will be unavailable for use after the next sync.  Thus there are four
possible states in the lifecycle of a resource: inactive, staged, active, and recalled. 

An inactive resource is not available to provisioners and is not scheduled to be available any time in the future. A staged resource is not
available to provisioners, but is scheduled to be available after the next resource sync. An active resource is available to provisioners
and will continue to be available after the next sync. A recalled resource is available to provisioners, but scheduled to be removed from
provisioners after the next sync.

Generally, an administrator will upload some resources, stage the versions that should be active, then sync all resources. As resources
evolve and update, new versions are uploaded and synced. If a resource ever needs to be removed completely, it is recalled, synced,
and finally deleted. All resource operations can be performed through the UI or directly through the REST APIs documented :doc:`here </rest/plugins>`  

Uploading Resources
===================

Plugin resources can only be uploaded by a tenant administrator. It can be done through the UI, or through the REST API
documented :doc:`here </rest/plugins>`. Each time a resource is uploaded, a new version of the resource is created. Subsequent
interactions require the version of a resource as well as the name. Archive resources are assumed to be constructed such that
the name of the resource is a top level folder in the expanded archive. A newly uploaded resource begins in the 'inactive' state.
An inactive resource is not visible to provisioners, meaning it cannot be used. It is also not scheduled to be used at any point 
in the future.  

Staging Resources
=================

Before a resource can be made available for use by provisioners, a specific version of the resource must be staged.
In the 'staged' state, a resource is still not available to provisioners, but is scheduled to be pushed out after the next sync. 

Staging an inactive resource will move it to the staged state. Additionally, if there is an active version of the resource, it
will be moved to the recalled state. Similarly, if there is already a staged version, it will be moved to the inactive state.
For example, suppose version 2 of the mysql cookbook is active. Staging version 3 of the mysql cookbook will move version 2 from
the active state to the recalled state. If version 4 of the mysql cookbook is now staged, version 2 will move back to the inactive
state.

Staging a recalled resource will move it back to the active state.
Staging a staged resource has no effect. Staging an active resource also has no effect. 

Recalling Resources
===================

The admin can remove a resource from use by recalling it. Recalling the active version of a resource moves it to the recalled state. 
A resource in the recalled state is still available for use by provisioners, but will be removed after the next sync. Recalling 
a staged resource will move it back to the inactive state. Recalling a recalled resource has no effect. Recalling an inactive resource
also has no effect.

Syncing Resources
=================

Syncing resources will make all staged resources active and all recalled resources inactive. Any active resources will stay active,
and any inactive resources will stay inactive. Any tasks that were already running when the sync began will continue to run with
the old resources. However, all new tasks are guaranteed to use the new active set of resources.

Tools
=====

There is also a resource upload tool provided in the provisioner package. You can use it to create an archive from a directory on 
your local filesystem and upload it to the server. You can see usage information by running: 

.. code-block:: bash

 $ /opt/coopr/provisioner/embedded/bin/ruby /opt/coopr/provisioner/bin/data-uploader.rb -h
 Usage: /opt/coopr/provisioner/bin/data-uploader.rb [options] <action> <local-path> <remote-target>
    -u, --uri URI                    Server URI, defaults to ENV['COOPR_SERVER_URI'] else "http://localhost:55054"
    -t, --tenant TENANT              Tenant, defaults to ENV['COOPR_TENANT'] else "superadmin"
    -U, --user USER                  User, defaults to ENV['COOPR_API_USER'] else "admin"
    -q, --quiet                      Suppress all non-error output
    --cert-path CERTPATH             Trust certificate path
    --cert-pass CERTPASS             Trust certificate password

 Required Arguments:
         <action>: one of upload, stage, or sync:
                     upload: uploads a single resource to the server
                     stage: uploads and stages a single resource to the server
                     sync: uploads and stages a single resource, then executes a sync on all staged resources
         <local-path>: path to the local copy of the resource to upload
         <remote-target>: api path defining the resource

 Example:
  /opt/coopr/provisioner/bin/data-uploader.rb -u http://localhost:55054 -t superadmin -U admin sync ./my/local/cookbooks/hadoop automatortypes/chef-solo/cookbooks/hadoop

.. note:: If you are uploading an archive of a directory, the top level directory of your unpacked archive must be named exactly the same as your resource name. For example, if you are uploading an archive of a directory named 'mycookbook', you *must* name your resource 'mycookbook' in order for it to be used correctly. This issue will be fixed in the next release. 
