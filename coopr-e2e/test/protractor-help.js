/**
 * helper functions for e2e tests
 */

module.exports = {

  logout: logout,

  login: login,

  deleteAssetFromList: deleteAssetFromList,

  editAssetFromList: editAssetFromList,

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

function deleteAssetFromList(list, name) {
  var selectedItem;
  list
    .then(function(s) {
      s.forEach(function(item, index) {
        item.getText().then(function(text) {
          if (text === name) {
            selectedItem = item;
          }
        });
      });
    })
    .then(function() {
      selectedItem.element(by.xpath("ancestor::tr"))
        .element(by.cssContainingText('.btn', 'Delete')).click();
      element(by.css('.modal-dialog .modal-footer .btn-primary')).click();
    });
}

function editAssetFromList(list, name) {
  var selectedItem;
  list
    .then(function(s) {
      s.forEach(function(item, index) {
        item.getText().then(function(text) {
          if (text === name) {
            selectedItem = item;
          }
        });
      });
    })
    .then(function() {
      selectedItem.element(by.xpath("ancestor::tr"))
        .element(by.cssContainingText('.btn', 'Edit')).click();
    });
}



function logout() {

  element(by.css('header .navbar-right .dropdown-toggle')).click();
  element(by.css('.dropdown-menu a[ng-click^="logout"]')).click();
  expect(element(by.css('header .dropdown-toggle .fa-user')).isPresent()).toBe(false);

}
