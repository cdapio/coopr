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

======================
Developers
======================

What is Continuuity Loom ?
==========================
Continuuity Loom is a cluster provisioning system where the smallest entity that is managed by Continuuity Loom is a cluster. Continuuity Loom allows the user
to provision clusters: be a cluster of JBoss Application Servers or a Hadoop Cluster. It does so using templates that are 
designed by devops or administrators. Continuuity Loom exposes developer friendly REST interfaces to manage configurations of clusters
and as well as for provisioning clusters. Templates allow developers and devops to create a repeatable process for deploying
complex infrastructure components and also allows them scale up or scale down as required. 

This documentation describes what Continuuity Loom is, as well as how it works. This section of Continuuity Loom documentation is mainly for developers
who would like to understand the internals of Continuuity Loom.

Getting started
===============
This section will help you understand how to checkout and build Continuuity Loom.

Build Continuuity Loom
=======================
It's easy to build and run Continuuity Loom.
::
  $ git clone http://github.com/continuuity/loom.git
  $ cd loom
  $ mvn package

Contents
========

.. toctree::
   :maxdepth: 2

   /developer/prior-art/index
   /developer/overview/index
   /developer/server/index
   /developer/provisioner/index
