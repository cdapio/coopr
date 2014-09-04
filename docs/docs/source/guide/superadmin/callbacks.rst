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

.. callbacks-reference:

.. index::
   single: Cluster Callbacks

=================
Cluster Callbacks
=================

.. include:: /guide/admin/admin-links.rst

Sometimes there is a need to insert custom code that needs to happen before and after cluster operations.
For example, you may want to send an email that notifies users when their clusters have been succesfully 
created and page your operations team if a cluster create operation failed. Or you may want to
hook up created clusters to your existing monitoring system once they are created, and remove them from your 
monitoring system after they are deleted.

The Server can perform custom callbacks before the start of any cluster operation, after an operation has successfully
completed, and after an operation has failed. To activate callbacks, you must set the ``server.callback.class`` setting
in the server config to the fully qualified name of the class you want to use. The class must implement the 
``ClusterCallback`` interface, and the jar containing the class must be included in the lib directory for the Server. 
The Server comes with a HttpPostRequestCallback class that can send a HTTP POST request containing the cluster, nodes,
and job to a configurable URL at start, success, and failure of different types of cluster operations. 

You can view the :doc:`javadocs </javadocs/index>` for more information about the interface and example callback class.
