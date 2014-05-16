..
   Copyright 2012-2014, Continuuity, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
 
       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

.. _overview_release-notes:

.. index::
   single: Release Notes

=============
Release Notes
=============
.. _release-notes:

Welcome to Continuuity Loom |release| Release. In today's release, we have updated Loom Server, Loom Provisioners, and Loom UI. Continuuity Loom overall has new and improved functionality and ton of bug fixes.

We hope you enjoy this release.  If you encounter any issues, please don't hesitate to post on our `Continuuity support portal
<https://continuuity.uservoice.com/clients/widgets/classic_widget?mode=support&link_color=162e52&primary_color=42afcf&embed
_type=lightbox&trigger_method=custom_trigger&contact_enabled=true&feedback_enabled=false&smartvote=true&referrer=http%3A%2F%2Fcontinuuity.com%2F#contact_us>`_.

Fixed Issues
^^^^^^^^^^^^^
• Unbounded job list stored in cluster object 
• Provisioner bootstrapping performing unnecessary work 
• Various cookbook improvements, updates, and fixes.

New Features
^^^^^^^^^^^^^
• UI Skin updated and selectable 
• Add a compatible service to an active cluster 
• Start, stop, and restart services on an active cluster 
• Reconfigure services on an active cluster 
• Plugin authors can specify what fields their plugin needs from the admin and user 
• Users able to provide provider related information at cluster create time 
• Extended cookbook support for Hadoop clusters with Kerberos and Hive 
• Customizable callbacks on start, success, and failure of cluster operations 
• Expanded service dependency capabilities 

Released Versions
^^^^^^^^^^^^^^^^^
• |release|
• 0.9.6 Beta
• 0.9.5 Beta
• 0.5.0 Alpha
• 0.1.0  

Known Issues
^^^^^^^^^^^^
• Minimal authentication 
• Key files must be stored in plugin 
• Provisioner does not enforce timeouts 

Licenses
^^^^^^^^
This section specifies all the components and their respective :doc:`licenses <licenses>` that are used in Loom Server, Loom UI and Loom Provisioner

