/**
 * E2E tests for /providers.
 */
var helper = require('../../protractor-help');

'use strict';

describe('hardware types test', function () {

  it('should log in', function () {
    browser.get('/login');

    element(by.id('loginTenant')).clear().sendKeys('superadmin');
    element(by.id('loginUsername')).clear().sendKeys('admin');
    element(by.id('loginPassword')).clear().sendKeys('admin');
    element(by.partialButtonText('Submit')).click();
  });

  it('should show the correct fields for provider', function () {
    browser.get('/hardwaretypes/create');
    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/hardwaretypes\/create$/);

    element(by.cssContainingText('.btn.btn-info', 'Add Provider')).click();
    var providers = element.all(by.repeater('item in content'));
    expect(providers.count()).toEqual(7);
    element(by.cssContainingText('.ng-binding.ng-scope', 'google')).click();
    expect(element.all(by.css('.coopr-control-group')).count()).toEqual(1);
    element(by.css('.coopr-control-group .remove')).click();
    expect(element.all(by.css('.coopr-control-group')).count()).toEqual(0);
  });

  it('should create a new provider', function () {
    browser.get('/hardwaretypes/create');
    element(by.binding('model.name')).sendKeys('test hardwaretype');
    element(by.binding('model.description')).sendKeys('test description');
    element(by.cssContainingText('.btn.btn-info', 'Add Provider')).click();
    element(by.cssContainingText('.ng-binding.ng-scope', 'joyent')).click();
    element(by.css('#my-ddtc-joyent-0')).sendKeys('test flavor');
    expect(element(by.partialButtonText('Create')).getAttribute('disabled')

  });



  it('should logout', function () {
    element(by.css('header .navbar-right .dropdown-toggle')).click();
    element(by.css('.dropdown-menu a[ng-click^="logout"]')).click();
  });

});


