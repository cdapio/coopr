..
   Copyright © 2012-2015 Cask Data, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
 
       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

.. _faq_toplevel:

.. index::
   single: Frequently Asked Questions

============================
FAQs
============================

`General <general.html>`__
==========================

#. :ref:`What are the differences between Coopr and Ambari/Savanna? <faq-general-1>`
#. :ref:`Does Coopr work with Ambari? <faq-general-2>`
#. :ref:`What are the differences between Coopr and Amazon EMR? <faq-general-3>`
#. :ref:`Will Coopr support docker based clusters ? <faq-general-4>`
#. :ref:`Does Coopr support bare metal ? <faq-general-5>`
#. :ref:`What providers are supported by Coopr ? <faq-general-6>`
#. :ref:`Does Coopr make it easy for me to move from one cloud to another ? <faq-general-7>`
#. :ref:`Can Coopr work on my laptop ? <faq-general-8>`
#. :ref:`How long has Coopr been used in a production enviroment and where is it being used? <faq-general-9>`
#. :ref:`Is Coopr designed only for provisioning compute and storage? <faq-general-10>`
#. :ref:`What is the recommended setup for Coopr in terms of hardware and configuration? <faq-general-11>`
#. :ref:`Does Coopr support monitoring and alerting of services deployed ? <faq-general-12>`
#. :ref:`Does Coopr support metering ? <faq-general-13>`
#. :ref:`I use Puppet; will I be able to use Puppet with Coopr? <faq-general-14>`
#. :ref:`Can Coopr support approval workflows or ability to pause provisioning for approval ? <faq-general-15>`

`Coopr Server <server.html>`__
==============================

#. :ref:`How many concurrent provisioning jobs can Coopr handle? <faq-server-1>`
#. :ref:`Can I scale-up or scale-down a cluster? <faq-server-2>`
#. :ref:`Do I have the ability to import and export configurations from one cluster to another? <faq-server-3>`
#. :ref:`Where are the configurations of cluster template and it's metadata stored? <faq-server-4>`
#. :ref:`How do I setup a database for Coopr to use it? <faq-server-5>`
#. :ref:`Is node pooling supported? <faq-server-6>`
#. :ref:`What is node pooling? <faq-server-7>`
#. :ref:`Can I run multiple servers concurrently for HA? <faq-server-8>`
#. :ref:`Can I look at the plan before the cluster is being provisioned? <faq-server-9>`
#. :ref:`Is there a way to plugin my own planner or layout solver? <faq-server-10>`
#. :ref:`Is there anyway to inspect the plan for cluster being provisioned? <faq-server-11>`

`Coopr Provisioner <provisioner.html>`__
========================================

#. :ref:`When something goes wrong, how can I look at the logs? <faq-provisioner-1>`
#. :ref:`How many provisioners should I run? <faq-provisioner-2>`
#. :ref:`Can I increase the number of provisioners on the fly? <faq-provisioner-3>`
#. :ref:`How many resources does each provisioner need? <faq-provisioner-4>`
#. :ref:`Is it possible for multiple provisioners to perform operations on the same node at the same time? <faq-provisioner-5>`
#. :ref:`Can I run different types of provisioners at the same time? <faq-provisioner-6>`
#. :ref:`Can I customize provisioners? <faq-provisioner-7>`
#. :ref:`What happens when I stop a provisioner while it is performing a task? <faq-provisioner-8>`
#. :ref:`Can the Chef Solo Automator plugin use a chef server ? <faq-provisioner-9>`

`Coopr Administration <admin.html>`__
==============================================

#. :ref:`What operations are only available to the admin versus other users? <faq-admin-1>`
#. :ref:`What happens to existing clusters when the template used to create them changes? <faq-admin-2>`
#. :ref:`How can I write configuration settings that reference hostnames of other nodes in the cluster? <faq-admin-3>`
#. :ref:`Can I configure clusters to delete themselves after some amount of time? <faq-admin-4>`
#. :ref:`What is the admin password? <faq-admin-5>`
#. :ref:`Any user password works. What are the user passwords for? <faq-admin-6>`

`Security <security.html>`__
============================

#. :ref:`Does Coopr support authentication? <faq-security-1>`
#. :ref:`Are all the communication between Coopr Server and Coopr Provisioners secure? <faq-security-2>`
#. :ref:`Can Coopr integrate with any authentication system? <faq-security-3>`
#. :ref:`Will Coopr support authorization and granular control in the future? <faq-security-4>`

`Licensing and Open Source <oss.html>`__
========================================

#. :ref:`What type of license is Coopr open sourced under? <faq-oss-1>`
#. :ref:`How can I contribute? <faq-oss-2>`
