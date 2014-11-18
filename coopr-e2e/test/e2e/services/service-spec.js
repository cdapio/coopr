/**
 * E2E tests for /services.
 */
var helper = require('../../protractor-help');

'use strict';

describe('services test', function () {

  var services,
      serviceName;

  it('should log in', function () {
    helper.loginAsAdmin();
  });

  it('should create a service', function () {
    browser.get('/services/create');

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/services\/create$/);

    serviceName = Date.now() + '-service';
    element(by.css('#inputServiceName')).sendKeys(serviceName);
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
    browser.get('/services/edit/' + serviceName);
    expect(element(by.css('#inputServiceName')).getAttribute('value')).toBe(serviceName);
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
    var selectedService,
        servicesCount;

    var serviceNames = element.all(by.repeater('item in list').column("item.name"));

    servicesCount = serviceNames.count();
    serviceNames
      .then(function(s) {
        s.forEach(function(item, index) {
          item.getText().then(function(text) {
            if (text === serviceName) {
              selectedService = item;
            }
          });
        });
      })
      .then(function() {
        selectedService.element(by.xpath("ancestor::tr"))
          .element(by.cssContainingText('.btn', 'Delete')).click();
        element(by.css('.modal-dialog .modal-footer .btn-primary')).click();
      });
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
