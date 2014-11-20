/**
 * helper functions for e2e tests
 */

module.exports = {

  logout: logout,

  login: login,

  loginAsAdmin: function() {
    login('superadmin', 'admin', 'admin');
  }

};


// ----------------------------------------------------------------------------

function login(tenant, username, password) {

  browser.get('/login');
  element(by.id('loginTenant')).clear().sendKeys(tenant);
  element(by.id('loginUsername')).clear().sendKeys(username);
  element(by.id('loginPassword')).clear().sendKeys(password);
  element(by.partialButtonText('Submit')).click();
  expect(element(by.css('header .dropdown-toggle .fa-user')).isPresent()).toBe(true);

}

function logout() {

  element(by.css('header .navbar-right .dropdown-toggle')).click();
  element(by.css('.dropdown-menu a[ng-click^="logout"]')).click();
  expect(element(by.css('header .dropdown-toggle .fa-user')).isPresent()).toBe(false);

}
