Continuuity Loom: Modern Cluster Management
================
Loom is used to provision cloud-based clusters using templates.

Take it for a spin
==================
To build Loom:

```
  $ git clone http://github.com/continuuity/loom.git
  $ cd loom
  $ mvn clean package assembly:single
```

This will create a zip in the target directory that can be used to run Loom on your machine. Unzip it 
and follow the instructions in the README to start up a demo version of Loom on your own machine. It 
comes pre-packaged with templates for Hadoop and LAMP. In order to create an actual 
cluster, you will need an account with a supported provider (Rackspace, Openstack, AWS), and to perform 
a couple steps to setup up Loom to use the right credentials to integrate with your provider. Follow 
the Quickstart Guide (http://continuuity.com/docs/loom/0.9.5/en/guide/quickstart/index.html#getting-started),
which steps through an example of creating a Hadoop cluster using Rackspace.

Learning More
=============
The User Guide (http://continuuity.com/docs/loom/0.9.5/en/overview/index.html) describes what Loom is and how
to use it, from the point of view of an end user and of an administrator. The Tech Guide 
(http://continuuity.github.io/loom/tech-docs/index.html) describes how Loom is implemented, and is a good 
overview for those interested in contributing to Loom.

API
===
REST endpoint are documented at http://continuuity.com/docs/loom/0.9.5/en/rest/index.html.
Javadocs can be seen at http://continuuity.github.io/loom/apidocs/index.html or can be
built locally by running:

mvn javadoc:javadoc

License
=======
Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
