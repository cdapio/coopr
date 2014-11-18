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
    browser.get('/imagetypes/edit/' + image);

    expect(element(by.css('#inputImageName')).getAttribute('value')).toBe(image);
    expect(element(by.css('#inputImageDescription')).getAttribute('value')).toBe('bar');
  });

  it('should delete imagetype', function () {
    browser.get('/imagetypes');
    var selectedImage;
    var imageNames = element.all(by.repeater('item in list').column("item.name"));

    imageCount = imageNames.count();
    helper.deleteAssetFromList(imageNames, image);
    expect(imageCount.then(function(i) {
      return i - 1;
    })).toBe(
      imageNames.count()
    ); //Lame..
  });


  it('should logout', function () {
    helper.logout();
  });

});
