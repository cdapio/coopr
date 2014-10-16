/**
 * E2E tests for /providers.
 */
var helper = require('../../protractor-help');

'use strict';

describe('hardware types test', function () {
  
  it('should log in', function () {
    helper.loginAsAdmin();
  });

  it('should select and delete providers correctly', function() {
    browser.get('/#/hardwaretypes/create');

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/#\/hardwaretypes\/create$/);

    element(by.partialButtonText('Add Provider')).click();
    var providers = element.all(by.repeater('item in content'));
    expect(providers.count()).toEqual(7);

    providers.first().click();
    var providerSelected = element.all(by.repeater('(key, value) in model'));
    expect(providerSelected.count()).toEqual(1);
    providers.first().element(by.css('.fa .fa-trash')).click();
    providerSelected = element.all(by.repeater('(key, value) in model'));
    expect(providerSelected.count()).toEqual(1);
  });

  it('should logout', function () {
    helper.logout();
  });

});


