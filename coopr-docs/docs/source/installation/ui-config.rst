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

:orphan:

.. index::
single: UI Configuration

================
UI Configuration
================

Running UI with SSL
^^^^^^^^^^^^^^^^^^^^^^^

To enable running UI with SSL, setup these environment variables:

====================================     ==========================    =======================================
   Environment variable                     Default Value                     Description
====================================     ==========================    =======================================
COOPR_UI_KEY_FILE                        COOPR_HOME/cert/server.key     Key file location.
COOPR_UI_CERT_FILE                       COOPR_HOME/cert/server.crt     Certificate password.
====================================     ==========================    =======================================

To configure UI to support server that uses mutual authentication with SSL, setup these environment variables:

====================================     ==========================    =======================================
   Environment variable                     Default Value                     Description
====================================     ==========================    =======================================
COOPR_UI_TLS_KEY_FILE                       None                        Trusted key file location.
COOPR_UI_TLS_CRT_FILE                       None                        Trusted certificate file location.
COOPR_UI_TLS_KEY_PASS                       None                        Trusted certificate password.
====================================     ==========================    =======================================
