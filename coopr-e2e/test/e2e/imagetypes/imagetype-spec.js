/**
 * E2E tests for /imagetypes.
 */
var helper = require('../../protractor-help');

'use strict';

describe('imagetypes types test', function () {

  var hardwareTypes;

  it('should log in', function () {
    helper.loginAsAdmin();
  });
  
  it('should show no imagetype types', function () {
    hardwareTypes = element.all(by.repeater('item in list'));
    expect(hardwareTypes.count()).toEqual(0);
  });

  it('should create a imagetype type', function () {
    browser.get('/imagetypes/create');
    
    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/imagetypes\/create$/);

    element(by.css('#inputImageName')).sendKeys('foo');
    element(by.css('#inputImageDescription')).sendKeys('bar');
    element(by.partialButtonText('Create')).click();

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/imagetypes$/);
  });

  it('should verify a imagetype type', function () {
    browser.get('/imagetypes');
    hardwareTypes = element.all(by.repeater('item in list'));
    expect(hardwareTypes.count()).toEqual(1);

    browser.get('/imagetypes/edit/foo');

    expect(element(by.css('#inputImageName')).getAttribute('value')).toBe('foo');
    expect(element(by.css('#inputImageDescription')).getAttribute('value')).toBe('bar');
  });

  it('should delete imagetype', function () {
    browser.get('/imagetypes');
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


