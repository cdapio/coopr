/**
 * helper functions for e2e tests
 */

function login(tenant, username, password) {
  var ptor = protractor.getInstance();

  function isLoggedIn () {
    return element(
      by.css('header .dropdown-toggle .fa-user')
    ).isDisplayed();
  };

  isLoggedIn()
    .then(function (needLogout) {
      if(needLogout) {
        logout();
      }
    })
    .then(function () {
      // console.log('logging in...');
      browser.get('/#/login');

      element(by.id('loginTenant')).clear().sendKeys(tenant);
      element(by.id('loginUsername')).clear().sendKeys(username);
      element(by.id('loginPassword')).clear().sendKeys(password);
      element(by.partialButtonText('Submit')).click();

      browser.wait(isLoggedIn, 5000);
    });
}


function logout() {
  // console.log('logging out...');
  var ptor = protractor.getInstance();

  function ddIsOpen() {
    return ptor.isElementPresent(
      by.css('header .dropdown.open .dropdown-menu')
    );
  }

  ddIsOpen()
    .then(function (openDropdown) {
      element(by.css('header .navbar-right .dropdown-toggle')).click();
      browser.wait(ddIsOpen, 5000);
    })
    .then(function () {
      element(by.cssContainingText('.dropdown-menu a', 'Logout')).click();

      browser.wait(function() {
        return ptor.isElementPresent(
          by.cssContainingText('#alerts .alert-info', 'You are now logged out')
        );
      }, 5000);
    });
}

// ----------------------------------------------------------------------------

module.exports = {

  logout: logout,

  login: login,

  loginAsAdmin: function() {
    login('superadmin', 'admin', 'admin');
  }

};