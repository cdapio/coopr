/**
 * E2E tests for /services.
 */
var helper = require('../../protractor-help');

'use strict';

describe('services test', function () {

  var services,
      service;

  it('should log in', function () {
    helper.loginAsAdmin();
  });

  it('should create a service', function () {
    browser.get('/services/create');

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/services\/create$/);

    service = Date.now() + '-service';
    element(by.css('#inputServiceName')).sendKeys(service);
    element(by.css('#inputServiceDescription')).sendKeys('bar');

    element(by.cssContainingText('.my-thing-picker a.btn-warning:not([disabled])', 'add feature')).click();
    element(by.css('#newThingName-feature')).sendKeys('feature1');
    element(by.partialButtonText('Add')).click();

    // This assumes that there is a "base" service created by default
    // and in addition to that we have added a service called "feature1" under provides.
    var provides = element.all(by.repeater('name in model'));
    expect(provides.count()).toEqual(2);

    element(by.css('.btn.add-automator-action')).click();
    element.all(by.repeater('item in content')).get(1).click();

    var automators = element.all(by.repeater('(category, action) in model.provisioner.actions'));
    expect(automators.count()).toEqual(1);

    var fields = element.all(by.repeater('(name,fieldData) in config.fields'));
    expect(fields.count()).toEqual(2);

    expect(fields.first().element(by.css('.control-label')).getText()).toEqual('JSON attributes');
    expect(fields.last().element(by.css('.control-label')).getText()).toEqual('run-list');

    element(by.cssContainingText('option', 'shell')).click();
    expect(fields.first().element(by.css('.control-label')).getText()).toEqual('Arguments');
    expect(fields.last().element(by.css('.control-label')).getText()).toEqual('Script');

    element(by.partialButtonText('Create')).click();

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/services$/);
  });

  it('should verify a service', function () {
    //browser.get('/services/edit/' + service);
    browser.get('/services');

    var serviceNames = element.all(by.repeater('item in list').column("item.name"));
    helper.editAssetFromList(serviceNames, service);
    expect(element(by.css('#inputServiceName')).getAttribute('value')).toBe(service);
    expect(element(by.css('#inputServiceDescription')).getAttribute('value')).toBe('bar');
    var provides = element.all(by.repeater('name in model'));
    expect(provides.count()).toEqual(2);
    var automators = element.all(
      by.repeater('(category, action) in model.provisioner.actions')
    );
    expect(automators.count()).toEqual(1);
  });

  it('should delete service', function () {
    browser.get('/services');
    var servicesCount,
        serviceNames = element.all(by.repeater('item in list').column("item.name"));

    servicesCount = serviceNames.count();
    helper.deleteAssetFromList(serviceNames, service);
    expect(servicesCount.then(function(i) {
      return i - 1;
    })).toBe(
      serviceNames.count()
    ); //Lame..
  });


  it('should logout', function () {
    helper.logout();
  });

});
