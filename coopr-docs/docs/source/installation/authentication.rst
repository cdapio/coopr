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
   single: Authentication support

======================
Authentication Support
======================

Authentication in COOPR Server is carried out using **Authentication Server** - the authentication server integrates
with different authentication backends (LDAP, JASPI plugins) using a plugin API. Clients must first authenticate
with the authentication server through this configured backend. Once authenticated, clients are issued an access token
representing their identity.

Enabling Security
=================
To enable security in COOPR Server, add these properties to ``coopr-site.xml``:

==========================================  ==============  ==============
   Property                                   Value           Default
==========================================  ==============  ==============
security.enabled                              true            false
security.auth.server.address                  <hostname>      localhost
security.auth.server.bind.port                <port>          55059
==========================================  ==============  ==============

Configuring SSL for the Authentication Server
=============================================
To configure the granting of ``AccessToken``\s via SSL, add these properties to ``coopr-security.xml``:

=============================================     =====================     =======================================
   Property                                        Default Value                Description
=============================================     =====================     =======================================
security.auth.server.ssl.keystore.path              None                      Keystore file location. The file should
                                                                              be owned and readable only by the
                                                                              COOPR user
security.auth.server.ssl.keystore.password          None                      Keystore password
security.auth.server.ssl.keystore.keypassword       None                      Keystore key password
security.auth.server.ssl.keystore.type              JKS                       Keystore file type
=============================================     =====================     =======================================


Configuring Authentication Mechanisms
=====================================
COOPR provides several ways to authenticate a user's identity.

Basic Authentication
--------------------
The simplest way to identity a user is to authenticate against a realm file.
To configure basic authentication add the following properties to ``coopr-site.xml``:

==========================================  ===========
   Property                                   Value
==========================================  ===========
security.authentication.handlerClassName     co.cask.cdap.security.server.BasicAuthenticationHandler
security.authentication.basic.realmfile      <path>
==========================================  ===========

The realm file is of the following format::

  username: password[,rolename ...]

Note that it is not advisable to use this method of authentication. In production, we recommend using any of the
other methods described below.

LDAP Authentication
-------------------
You can configure COOPR to authenticate against an LDAP instance by adding these
properties to ``coopr-site.xml``:

================================================  ===========
   Property                                         Value
================================================  ===========
security.authentication.handlerClassName            co.cask.cdap.security.server.LDAPAuthenticationHandler
security.authentication.loginmodule.className       org.eclipse.jetty.plus.jaas.spi.LdapLoginModule
security.authentication.handler.debug               true/false
security.authentication.handler.hostname            <hostname>
security.authentication.handler.port                <port>
security.authentication.handler.userBaseDn          <userBaseDn>
security.authentication.handler.userRdnAttribute    <userRdnAttribute>
security.authentication.handler.userObjectClass     <userObjectClass>
================================================  ===========

In addition, you may also configure these optional properties:

=====================================================  ===========
   Property                                               Value
=====================================================  ===========
security.authentication.handler.bindDn                  <bindDn>
security.authentication.handler.bindPassword            <bindPassword>
security.authentication.handler.userIdAttribute         <userIdAttribute>
security.authentication.handler.userPasswordAttribute   <userPasswordAttribute>
security.authentication.handler.roleBaseDn              <roleBaseDn>
security.authentication.handler.roleNameAttribute       <roleNameAttribute>
security.authentication.handler.roleMemberAttribute     <roleMemberAttribute>
security.authentication.handler.roleObjectClass         <roleObjectClass>
=====================================================  ===========

Java Authentication Service Provider Interface (JASPI) Authentication
---------------------------------------------------------------------
To authenticate a user using JASPI add the following properties to ``coopr-site.xml``:

================================================  ===========
   Property                                         Value
================================================  ===========
security.authentication.handlerClassName            co.cask.cdap.security.server.JASPIAuthenticationHandler
security.authentication.loginmodule.className       <custom-login-module>
================================================  ===========

In addition, any properties with the prefix ``security.authentication.handler.``,
such as ``security.authentication.handler.hostname``, will also be used by the handler.
These properties, without the prefix, will be used to instantiate the ``javax.security.auth.login.Configuration`` used
by the ``LoginModule``.

Custom Authentication
---------------------
To provide a custom authentication mechanism you may create your own ``AuthenticationHandler`` by overriding
``AbstractAuthenticationHandler`` and implementing the abstract methods ::

  public class CustomAuthenticationHandler extends AbstractAuthenticationHandler {

    @Inject
    public CustomAuthenticationHandler(CConfiguration configuration) {
      super(configuration);
    }

    @Override
    protected LoginService getHandlerLoginService() {
      // ...
    }

    @Override
    protected IdentityService getHandlerIdentityService() {
      // ...
    }

    @Override
    protected Configuration getLoginModuleConfiguration() {
      // ...
    }
  }

.. highlight:: console

Testing Security
----------------
From here on out we will use::

  <base-url>

to represent the base URL that clients can use for the HTTP REST API::

  http://<host>:<port>

and::

  <base-auth-url>

to represent the base URL that clients can use for obtaining access tokens::

  http://<host>:<auth-port>

where ``<host>`` is the host name of the COOPR server, ``<port>`` is the port that is set as the ``server.port``
in ``coopr-site.xml`` (default: ``55054``), and ``<auth-port>`` is the port that is set as the
``security.auth.server.bind.port`` (default: ``55059``).

To ensure that you've configured security correctly, run these simple tests to verify that the
security components are working as expected:

- After configuring COOPR as described above, restart COOPR and attempt to use a service::

    curl <base-url>/status

- This should return a 401 Unauthorized response. Submit a username and password to obtain an ``AccessToken``::

    curl -u username:password <base-auth-url>/token

- This should return a 200 OK response with the ``AccessToken`` string in the response body.
  Reattempt the first command, but this time include the ``AccessToken`` as a header in the command::

    curl -H "Authorization: Bearer <AccessToken>" <base-url>/status

- This should return a 200 OK response.

Obtaining an Access Token
-------------------------
Obtain a new access token by calling::

   GET <base-auth-url>/token

The required header and request parameters may vary according to the external authentication mechanism
that has been configured.  For username and password based mechanisms, the ``Authorization`` header may be used::

   Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW

HTTP Responses
++++++++++++++

* ``200 OK`` - Authentication was successful and an access token will be returned
* ``401 Unauthorized`` - Authentication failed

Success Response Fields
+++++++++++++++++++++++

* ``access_token`` - The Access Token issued for the client. The serialized token contents are base-64 encoded
  for safe transport over HTTP.
* ``token_type`` - In order to conform with the OAuth 2.0 Bearer Token Usage specification (`RFC 6750`_), this
  value must be "Bearer".
* ``expires_in`` - Token validity lifetime in seconds.

Example
+++++++
Sample request::

   GET <base-auth-url>/token HTTP/1.1
   Host: server.example.com
   Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW

Sample response::

   HTTP/1.1 200 OK
   Content-Type: application/json;charset=UTF-8
   Cache-Control: no-store
   Pragma: no-cache

   {
     "access_token":"2YotnFZFEjr1zCsicMWpAA",
     "token_type":"Bearer",
     "expires_in":3600,
   }

Comments
--------
**Note:** Only ``Bearer`` tokens (`RFC 6750`_) are currently supported.

.. _RFC 6750: http://tools.ietf.org/html/rfc6750
