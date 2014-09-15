/**
 * Constants used throughout app. These can be dependency injected wherever appropriate.
 */

var module = angular.module(PKG.name+'.constants');

module.constant('MYAUTH_EVENT', {
  loginSuccess: 'myauth-login-success',
  loginFailed: 'myauth-login-failed',
  logoutSuccess: 'myauth-logout-success',
  sessionTimeout: 'myauth-session-timeout',
  notAuthenticated: 'myauth-not-authenticated',
  notAuthorized: 'myauth-not-authorized'
});


module.constant('MYAUTH_ROLE', {
  all: '*',
  superadmin: 'superadmin',
  admin: 'admin'
});


module.constant('MYAPI_EVENT', {
  error: 'myapi-error'
});

module.constant('MYTHEME_NAMES', [
  'default',
  'yellow'
]);

module.constant('MYTHEME_EVENT', {
  changed: 'mytheme-changed'
});