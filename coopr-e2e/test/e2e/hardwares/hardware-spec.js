/**
 * E2E tests for /providers.
 */
var helper = require('../../protractor-help');

'use strict';

describe('hardware types test', function () {

  var hardwareTypes,
      hardwareNames,
      hardware;

  it('should log in', function () {
    helper.loginAsAdmin();
  });

  it('should create a hardware type', function () {
    browser.get('/hardwaretypes/create');
    hardware = Date.now() + '-hardware';
    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/hardwaretypes\/create$/);

    element(by.css('#inputHardwareName')).sendKeys(hardware);
    element(by.css('#inputHardwareDescription')).sendKeys('bar');
    element(by.partialButtonText('Create')).click();

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/hardwaretypes$/);
  });

  it('should verify a hardware type', function () {
    browser.get('/hardwaretypes');

    element(by.cssContainingText('tr', hardware))
      .element(by.cssContainingText('.btn', 'Edit')).click();

    expect(element(by.css('#inputHardwareName')).getAttribute('value')).toBe(hardware);
    expect(element(by.css('#inputHardwareDescription')).getAttribute('value')).toBe('bar');
  });

  it('should delete hardwaretype', function () {
    browser.get('/hardwaretypes');
    var selectedHardware;

    element(by.cssContainingText('tr', hardware))
      .element(by.cssContainingText('.btn', 'Delete')).click();
    element(by.css('.modal-dialog .modal-footer .btn-primary')).click();

    expect(element(by.cssContainingText('tr', hardware)).isPresent()).toBe(false);
  });


  it('should logout', function () {
    helper.logout();
  });

});
