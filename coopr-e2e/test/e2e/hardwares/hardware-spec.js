/**
 * E2E tests for /providers.
 */
var helper = require('../../protractor-help');

'use strict';

describe('hardware types test', function () {

  var hardwareTypes;

  it('should log in', function () {
    helper.loginAsAdmin();
  });
  
  it('should show no hardware types', function () {
    hardwareTypes = element.all(by.repeater('item in list'));
    expect(hardwareTypes.count()).toEqual(0);
  });

  it('should create a hardware type', function () {
    browser.get('/hardwaretypes/create');
    
    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/hardwaretypes\/create$/);

    element(by.css('#inputHardwareName')).sendKeys('foo');
    element(by.css('#inputHardwareDescription')).sendKeys('bar');
    element(by.partialButtonText('Create')).click();

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/hardwaretypes$/);
  });

  it('should verify a hardware type', function () {
    browser.get('/hardwaretypes');
    hardwareTypes = element.all(by.repeater('item in list'));
    expect(hardwareTypes.count()).toEqual(1);

    browser.get('/hardwaretypes/edit/foo');

    expect(element(by.css('#inputHardwareName')).getAttribute('value')).toBe('foo');
    expect(element(by.css('#inputHardwareDescription')).getAttribute('value')).toBe('bar');
  });

  it('should delete hardwaretype', function () {
    browser.get('/hardwaretypes');
    hardwareTypes = element.all(by.repeater('item in list'));
    hardwareTypes.first().element(by.cssContainingText('.btn', 'Delete')).click();
    element(by.css('.modal-dialog .modal-footer .btn-primary')).click();
    hardwareTypes = element.all(by.repeater('item in list'));
    expect(hardwareTypes.count()).toEqual(0);
  });


  it('should logout', function () {
    helper.logout();
  });

});


