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

.. _faq-security:

.. index::
   single: FAQ: Security

====================================
Security
====================================

.. _faq-security-1:

Does Coopr support authentication?
----------------------------------------------
Coopr backend has minimal support for authentication. In the next version, there will
be integration with Crowd and LDAP servers allowing users to authenticate against the 
available directories.

.. _faq-security-2:

Are all communications between Coopr Server and Coopr Provisioners secure?
------------------------------------------------------------------------------------
Not right now, but the plan is to move them to communicate on https in future releases. 
This is not an immediate concern, since there is no user sensitive data passed between 
them.

.. _faq-security-3:

Can Coopr integrate with any authentication system?
---------------------------------------------------------------
It's designed to integrate with any authentication system. The next release will include support
for OpenID, LDAP and OAuth, and the later releases will open up integration with different systems.

.. _faq-security-4:

Will Coopr support authorization and granular control in future?
---------------------------------------------------------------------------
Absolutely, this feature is few releases down the lane. Indeed, it's one of the most important features that Coopr
will be supporting. In large scale deployment of clusters and nodes, having granular and role based access control are 
imperative for auditing and accountability.
