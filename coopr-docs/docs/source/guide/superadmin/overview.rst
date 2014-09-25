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

.. _guide_superadmin_toplevel:

.. index::
   single: Super Admin Overview

========
Overview
========

Types of Users
==============

There are 3 types of users in the system. The first is the superadmin, the second is the tenant admin, and the third is the tenant
user. The superadmin can create, edit, and delete tenants. The superadmin is also an admin of his own superadmin tenant. The 
tenant admin can create, edit, and delete providers, services, hardware types, image types, cluster templates, and plugin resources
for his own tenant. Any object created in one tenant is invisible to another tenant. Finally, tenant users are able to read cluster
templates in their tenant to create and manage their own clusters. Clusters created by one user within a tenant are invisible to
other users in their own tenant.

System Tools
============
Coopr allows administrators to :doc:`configure their servers </installation/server-config>`
and  :doc:`write custom plugins </guide/superadmin/plugins>` for allocating machines with your providers or to implement custom services.
Administrators who are more command line driven, or who wish to write quick administrative scripts,
can employ the :doc:`Web services API </rest/index>`.
There are also several additional :doc:`metrics and monitoring </guide/superadmin/monitoring>` tools.

Please refer to the following pages for more details:

        * :doc:`Server Configuration </installation/server-config>`

        * :doc:`Monitoring and Metrics </guide/superadmin/monitoring>`

        * :doc:`Provisioner Plugins </guide/superadmin/plugins>`

        * :doc:`REST Web Service Interface </rest/index>`
