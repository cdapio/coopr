/**
 * E2E tests for /imagetypes.
 */
var helper = require('../../protractor-help');

'use strict';

describe('imagetypes types test', function () {

  var imageTypes,
      image;

  it('should log in', function () {
    helper.loginAsAdmin();
  });

  it('should create a imagetype type', function () {
    browser.get('/imagetypes/create');

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/imagetypes\/create$/);
    image = Date.now() + '-image';
    element(by.css('#inputImageName')).sendKeys(image);
    element(by.css('#inputImageDescription')).sendKeys('bar');
    element(by.partialButtonText('Create')).click();

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/imagetypes$/);
  });

  it('should verify a imagetype type', function () {
    browser.get('/imagetypes');
    element(by.cssContainingText('tr', image))
      .element(by.cssContainingText('.btn', 'Edit')).click();

    expect(element(by.css('#inputImageName')).getAttribute('value')).toBe(image);
    expect(element(by.css('#inputImageDescription')).getAttribute('value')).toBe('bar');
  });

  it('should delete imagetype', function () {
    browser.get('/imagetypes');
    element(by.cssContainingText('tr', image))
      .element(by.cssContainingText('.btn', 'Delete')).click();
    element(by.css('.modal-dialog .modal-footer .btn-primary')).click();

    expect(element(by.cssContainingText('tr', image)).isPresent()).toBe(false);
  });


  it('should logout', function () {
    helper.logout();
  });

});
