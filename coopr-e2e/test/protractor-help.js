/**
 * helper functions for e2e tests
 */

module.exports = {

  isLoggedIn: isLoggedIn,

  logout: logout,

  login: login,

  loginAsAdmin: function() {
    login('superadmin', 'admin', 'admin');
  }

};

// ----------------------------------------------------------------------------

function login(tenant, username, password) {

  isLoggedIn()
    .then(function (needLogout) {
      if(needLogout) {
        logout();
      }

      browser.get('/login');

      element(by.id('loginTenant')).clear().sendKeys(tenant);
      element(by.id('loginUsername')).clear().sendKeys(username);
      element(by.id('loginPassword')).clear().sendKeys(password);
      element(by.partialButtonText('Submit')).click();

      browser.wait(isLoggedIn, 5000);
    });
}


function logout() {
  browser.get('/');

  ddIsOpen()
    .then(function (dd) {

      if(!dd) {
        element(by.css('header .navbar-right .dropdown-toggle')).click();
        browser.wait(ddIsOpen, 5000);
      }

      element(by.css('.dropdown-menu a[ng-click^="logout"]')).click();

      browser.wait(function() {
        return element(
          by.cssContainingText('#alerts .alert-info', 'You are now logged out')
        ).isPresent();
      }, 5000);
    });
}

function isLoggedIn () {
  return element(
    by.css('header .dropdown-toggle .fa-user')
  ).isPresent();
};

function ddIsOpen() {
  return element(
    by.css('header .dropdown.open .dropdown-menu')
  ).isPresent();
}

