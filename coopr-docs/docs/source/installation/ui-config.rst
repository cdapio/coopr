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

.. index::
   single: UI Configuration

================
UI Configuration
================

SSL Configuration Steps
^^^^^^^^^^^^^^^^^^^^^^^

SSL configuration requires these steps: (the first two steps are only necessary once per machine)

- Create PEM encoded SSL certificate and key
- Put the certificate and key in place (e.g. in /etc/pki/tls/certs/ and /etc/pki/tls/private/, see below)
- Set the environment variables shown below, to describe the locations
- Set and export the variables
- Correctly configure the UI (next step)


Running UI with SSL
^^^^^^^^^^^^^^^^^^^

To enable running the UI with SSL, set these environment variables:

==================================== ============================== =======================================
   Environment variable                     Default Value                     Description
==================================== ============================== =======================================
``COOPR_UI_KEY_FILE``                ``COOPR_HOME/cert/server.key`` Key file location
``COOPR_UI_CERT_FILE``               ``COOPR_HOME/cert/server.crt`` Certificate file location
``COOPR_SSL``                        ``true``                       Use SSL for the UI
``COOPR_UI_SSL_PORT``                ``8443``                       Port the UI listens on for connections
                                                                    (**optional**)
==================================== ============================== =======================================


.. highlight:: console

Create a file (example, ``/etc/profile.d/coopr-ui.sh``) with appropriate entries,
modifying the values as needed to point to the location of your certificate file and key,
and, optionally, a different port (``28443`` is used here as an example)::

  export COOPR_UI_KEY_FILE=/etc/pki/tls/private/ui-cert.key
  export COOPR_UI_CERT_FILE=/etc/pki/tls/certs/ui-cert.crt
  export COOPR_UI_SSL=true
  export COOPR_UI_SSL_PORT=28443


Logging into the UI with SSL Enabled
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

When logging into the UI with SSL enabled, the default port is ``8443``.
You only need to set ``COOPR_UI_SSL_PORT`` if you wish to use a different port for your UI
to listen on when using SSL.
