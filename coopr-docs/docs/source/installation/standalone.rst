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

.. index::
   single: Coopr Sandboxes

==================
Coopr Sandboxes
==================

.. highlight:: console

For convenience, we have provided several ways to run Coopr on your laptop for experimentation. It is not recommended to run Coopr
in any of these modes for anything other than experimentation.

Standalone
----------
Standalone Coopr is a version of Coopr that can run on your Mac. It requires Java 6 or Java 7 to run the server, Node v0.10.26 or higher
for the UI, and Ruby 1.9.0p0 or higher for the provisioner. The server runs an embedded Derby database and an in-process zookeeper instance,
removing the need to setup a database or zookeeper instance. It is not recommended to run Coopr in this mode for anything other
than experimentation.

`Download <http://repository.cask.co/downloads/co/cask/coopr/coopr-standalone/0.9.9/coopr-standalone-0.9.9.zip>`_ the standalone zip.
After downloading it, unzip it and follow the instructions in the README.md file to install the required ruby gems for the provisioner.
After that, you can start and initialize Coopr standalone by running::

 $ bin/coopr.sh start

This should start up the server, provisioner, and UI, as well as initialize the server with some default templates. At this point you can
follow the instructions in the :ref:`Quickstart Guide <quickstart-getting-started>` to try provisioning a cluster.

Virtual Machine
---------------
You can also run Coopr in a virtual machine that you can `download here <http://repository.cask.co/downloads/co/cask/coopr/coopr-vm/0.9.9/coopr-vm-0.9.9-4.ova>`_. 
You will need virtualization software like Oracle VirtualBox or
VMWare player in order to run the VM. Once you have downloaded the image, import it using VirtualBox or VMWare Player. 
The VM has been set up with 4GB RAM and 10GB disk. No password is required to enter the machine. However, should you need to perform an administrative
operation, the coopr admin password is coopr.
Coopr has already been installed and configured in the virtual machine and will start up automatically when the machine start.
On start up a browser will also start with the Coopr UI loaded at http://localhost:8100. At this point you can
follow the instructions in the :ref:`Quickstart Guide <quickstart-getting-started>` to try provisioning a cluster. 

