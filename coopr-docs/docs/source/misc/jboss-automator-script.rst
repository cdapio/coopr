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

===========================
JBoss Application Server
===========================

This document describes how to install and configure JBoss Application Server (AS) and template with Coopr in less than 10 minutes.
JBoss AS is an open source Java Application Server. For more information please visit the `JBoss AS site <http://www.jboss.org/jbossas>`_

Prerequisite
------------
Before getting started, export the following helper variables according to your specific environment:
::

  $ export REMOTE_COOPR_PLUGINS="http://tools.cask.co/downloads/coopr/plugins"
  $ export COOPR_SHELL_AUTOMATORS="/opt/coopr/provisioner/daemon/plugins/automators/shell_automator"
  $ export COOPR_SERVER_HOST=<coopr-server>
  $ export COOPR_SERVER_PORT=<coopr-port>

Download and Install Coopr JBoss Automator Scripts
-----------------------------------------------------------
The JBoss Automator Scripts bundle contains a shell script plugin to install and configure JBoss AS on a node within a cluster. It also contains and configures a 'HelloWorld' application.

On every Coopr provisioner node, download and install the JBoss bundle into the Coopr shell automators directory:
::

 $ curl -o /tmp/jboss-automator-scripts-0.1.0.tgz  \
    $REMOTE_COOPR_PLUGINS/jboss-automator-scripts-0.1.0.tgz
 $ sudo tar xvf /tmp/jboss-automator-scripts-0.1.0.tgz -C $COOPR_SHELL_AUTOMATORS/scripts

Verify Installation
-----------------------------
After the JBoss bundle is installed, you should see the following directory structure:
::

 /opt/coopr/provisioner/daemon/plugins/automators/shell_automator/scripts$ ls -tlr
 total 24
 -rwxrwxr-x 1 coopr coopr  629 Feb 14 22:15 coopr_service_runner.sh
 -rwxrwxr-x 1 coopr coopr  962 Feb 27 01:40 chef_client_bootstrap.sh
 drwxr-xr-x 2 coopr coopr 4096 Mar  8 09:20 jboss-apps          [Directory of Apps]
 -rwxr-xr-x 1 coopr coopr 8010 Mar  8 10:38 jboss-installer     [Installer]
 /opt/coopr/provisioner/daemon/plugins/automators/shell_automator/scripts$ 

.. note:: More JBoss applications can be added to the 'jboss-apps' directory and will be installed into the '$JBOSS_HOME/standalone/deployments' directory by default.

Configure Oracle Java 7 Service in Coopr
---------------------------------------------------
JBoss AS requires Oracle Java 7. The following will add a service to Coopr for installing and configuring Java:
::

 $ curl -o /tmp/oracle-java-7 $REMOTE_COOPR_PLUGINS/jboss/services/oracle-java-7
 $ curl -X POST -H "Coopr-UserId:admin"\
     -d @/tmp/oracle-java-7 http://$COOPR_SERVER:$COOPR_PORT/v1/coopr/services

Configure JBoss AS Service in Coopr
----------------------------------------------
Now that we have installed the scripts and dependent services, we must configure the JBoss AS service within Coopr and then define a cluster template to enable Coopr provisioning of JBoss AS clusters.
This configuration can be achieved using either the Coopr Admin UI or Webservices.

The following adds service 'jboss' to the service list.
::

 $ curl -o /tmp/jboss $REMOTE_COOPR_PLUGINS/jboss/services/jboss
 $ curl -X POST -H "Coopr-UserId:admin"\
     -d @/tmp/jboss http://$COOPR_SERVER:$COOPR_PORT/v1/coopr/services

Create JBoss AS Cluster Template in Coopr
-----------------------------------------------------
Once the 'jboss' service has been added to Coopr, you are ready to create a cluster template or even add this service to an existing cluster template.
The easiest way to get started running JBoss AS is to use the Webservices to add a new JBoss cluster template to Coopr.

.. sidebar:: Cluster template details

   Requires Java 7 and Ubuntu Image (jboss-installer has only been tested on Ubuntu).
   After you have executed the following command, please make sure to update the provider for 
   'jboss-application-server' to appropriate one.

Use the following command to create the 'jboss-application-server' cluster template:
::

 $ curl -o /tmp/jboss-application-server \
     $REMOTE_COOPR_PLUGINS/jboss/templates/jboss-application-server
 $ curl -X POST -H "Coopr-UserId:admin"\
     -d @/tmp/jboss-application-server http://$COOPR_SERVER:$COOPR_PORT/v1/coopr/clustertemplates

Provision an instance of 'jboss-application-cluster' with Coopr
--------------------------------------------------------------------------
Once the cluster template has been created, you are now ready to create a cluster. Please
follow the cluster creation procedure from the main Coopr documentation. You can either choose
a singlenode or multi-node JBoss AS cluster. Once the cluster has been successfully provisioned
you can verify the installations by visiting ``http://<host-id>:8080`` or can also check the default
*helloworld* application by visiting ``http://<host-id>:8080/helloworld/hi.jsp``.
