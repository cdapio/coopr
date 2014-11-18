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
    browser.get('/hardwaretypes/edit/' + hardware);

    expect(element(by.css('#inputHardwareName')).getAttribute('value')).toBe(hardware);
    expect(element(by.css('#inputHardwareDescription')).getAttribute('value')).toBe('bar');
  });

  it('should delete hardwaretype', function () {
    browser.get('/hardwaretypes');
    var selectedHardware;
    hardwareNames = element.all(by.repeater('item in list').column("item.name"));
    hardwareCount = hardwareNames.count();

    hardwareNames
      .then(function(s) {
        s.forEach(function(item, index) {
          item.getText().then(function(text) {
            if (text === hardware) {
              selectedHardware = item;
            }
          });
        });
      })
      .then(function() {
        selectedHardware.element(by.xpath("ancestor::tr"))
          .element(by.cssContainingText('.btn', 'Delete')).click();
        element(by.css('.modal-dialog .modal-footer .btn-primary')).click();
      });
    expect(hardwareCount.then(function(i) {
      return i - 1;
    })).toBe(
      hardwareNames.count()
    ); //Lame..
  });


  it('should logout', function () {
    helper.logout();
  });

});
