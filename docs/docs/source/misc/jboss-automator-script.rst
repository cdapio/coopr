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

===========================
JBoss Application Server
===========================

This document describes how to install and configure JBoss Application Server (AS) and template with Continuuity Loom in less than 10 minutes.
JBoss AS is an open source Java Application Server. For more information please visit the `JBoss AS site <http://www.jboss.org/jbossas>`_

Prerequisite
------------
Before getting started, export the following helper variables according to your specific environment:
::

  $ export REMOTE_LOOM_PLUGINS="http://tools.continuuity.com/downloads/loom/plugins"
  $ export LOOM_SHELL_AUTOMATORS="/opt/loom/provisioner/daemon/plugins/automators/shell_automator"
  $ export LOOM_SERVER_HOST=<loom-server>
  $ export LOOM_SERVER_PORT=<loom-port>

Download and Install Loom JBoss Automator Scripts
-----------------------------------------------------------
The JBoss Automator Scripts bundle contains a shell script plugin to install and configure JBoss AS on a node within a cluster. It also contains and configures a 'HelloWorld' application.

On every Loom provisioner node, download and install the JBoss bundle into the Loom shell automators directory:
::

 $ curl -o /tmp/jboss-automator-scripts-0.1.0.tgz  \
    $REMOTE_LOOM_PLUGINS/jboss-automator-scripts-0.1.0.tgz
 $ sudo tar xvf /tmp/jboss-automator-scripts-0.1.0.tgz -C $LOOM_SHELL_AUTOMATORS/scripts

Verify Installation
-----------------------------
After the JBoss bundle is installed, you should see the following directory structure:
::

 /opt/loom/provisioner/daemon/plugins/automators/shell_automator/scripts$ ls -tlr
 total 24
 -rwxrwxr-x 1 loom loom  629 Feb 14 22:15 loom_service_runner.sh
 -rwxrwxr-x 1 loom loom  962 Feb 27 01:40 chef_client_bootstrap.sh
 drwxr-xr-x 2 loom loom 4096 Mar  8 09:20 jboss-apps          [Directory of Apps]
 -rwxr-xr-x 1 loom loom 8010 Mar  8 10:38 jboss-installer     [Installer]
 /opt/loom/provisioner/daemon/plugins/automators/shell_automator/scripts$ 

.. note:: More JBoss applications can be added to the 'jboss-apps' directory and will be installed into the '$JBOSS_HOME/standalone/deployments' directory by default.

Configure Oracle Java 7 Service in Continuuity Loom
---------------------------------------------------
JBoss AS requires Oracle Java 7. The following will add a service to Continuuity Loom for installing and configuring Java:
::

 $ curl -o /tmp/oracle-java-7 $REMOTE_LOOM_PLUGINS/jboss/services/oracle-java-7
 $ curl -X POST -H "X-Loom-UserId:admin"\
     -d @/tmp/oracle-java-7 http://$LOOM_SERVER:$LOOM_PORT/v1/loom/services

Configure JBoss AS Service in Continuuity Loom
----------------------------------------------
Now that we have installed the scripts and dependent services, we must configure the JBoss AS service within Continuuity Loom and then define a cluster template to enable Loom provisioning of JBoss AS clusters.
This configuration can be achieved using either the Loom Admin UI or Webservices.

The following adds service 'jboss' to the service list.
::

 $ curl -o /tmp/jboss $REMOTE_LOOM_PLUGINS/jboss/services/jboss
 $ curl -X POST -H "X-Loom-UserId:admin"\
     -d @/tmp/jboss http://$LOOM_SERVER:$LOOM_PORT/v1/loom/services

Create JBoss AS Cluster Template in Continuuity Loom
-----------------------------------------------------
Once the 'jboss' service has been added to Continuuity Loom, you are ready to create a cluster template or even add this service to an existing cluster template.
The easiest way to get started running JBoss AS is to use the Webservices to add a new JBoss cluster template to Continuuity Loom.

.. sidebar:: Cluster template details

   Requires Java 7 and Ubuntu Image (jboss-installer has only been tested on Ubuntu).
   After you have executed the following command, please make sure to update the provider for 
   'jboss-application-server' to appropriate one.

Use the following command to create the 'jboss-application-server' cluster template:
::

 $ curl -o /tmp/jboss-application-server \
     $REMOTE_LOOM_PLUGINS/jboss/templates/jboss-application-server
 $ curl -X POST -H "X-Loom-UserId:admin"\
     -d @/tmp/jboss-application-server http://$LOOM_SERVER:$LOOM_PORT/v1/loom/clustertemplates

Provision an instance of 'jboss-application-cluster' with Continuuity Loom
--------------------------------------------------------------------------
Once the cluster template has been created, you are now ready to create a cluster. Please
follow the cluster creation procedure from the main Loom documentation. You can either choose
a singlenode or multi-node JBoss AS cluster. Once the cluster has been successfully provisioned
you can verify the installations by visiting http://<host-id>:8080 or can also check the default
helloworld application by visiting http://<host-id>:8080/helloworld/hi.jsp.
