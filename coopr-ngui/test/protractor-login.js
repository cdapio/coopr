'use strict';

describe('providers list page tests login', function () {
  
  it('should log in', function () {
    browser.get('/#/login');
    var username = element(by.name('username'));
    var password = element(by.name('password'));
    var tenant = element(by.name('tenant'));
    username.clear();
    password.clear();
    tenant.clear();

    username.sendKeys('admin');
    password.sendKeys('admin');
    tenant.sendKeys('superadmin');
    element(by.partialButtonText('Submit')).click();
    expect(element(by.binding('currentUser')).getText()).toBe('Admin');
    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/#\/$/);
  });
});
