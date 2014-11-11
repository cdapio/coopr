/**
 * E2E tests for /services.
 */
var helper = require('../../protractor-help');

'use strict';

describe('services test', function () {

  var services;

  it('should log in', function () {
    helper.loginAsAdmin();
  });
  
  it('should show no services', function () {
    services = element.all(by.repeater('item in list'));
    expect(services.count()).toEqual(0);
  });

  it('should create a service', function () {
    browser.get('/services/create');
    
    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/services\/create$/);

    element(by.css('#inputServiceName')).sendKeys('foo');
    element(by.css('#inputServiceDescription')).sendKeys('bar');

    element(by.cssContainingText('.my-thing-picker a.btn-warning:not([disabled])', 'add feature')).click();
    element(by.css('#newThingName-feature')).sendKeys('feature1');
    element(by.partialButtonText('Add')).click();

    var provides = element.all(by.repeater('name in model'));
    expect(provides.count()).toEqual(2);

    element(by.css('.add-automator-action-btn')).click();
    var options = element.all(by.repeater('item in content'));
    options.get(1).click();

    var automators = element.all(by.repeater('(category, action) in model.provisioner.actions'));
    expect(automators.count()).toEqual(1);

    var fields = element.all(by.repeater('(name,fieldData) in config.fields'));

    expect(fields.count()).toEqual(2);

    expect(fields.first().element(by.css('.control-label')).getText()).toEqual('JSON attributes');
    expect(fields.last().element(by.css('.control-label')).getText()).toEqual('run-list');

    element(by.cssContainingText('option', 'shell')).click();
    expect(fields.first().element(by.css('.control-label')).getText()).toEqual('Arguments');
    expect(fields.last().element(by.css('.control-label')).getText()).toEqual('Script');    

    automators.first().element(by.css('.btn-danger')).click();
    automators = element.all(by.repeater('(category, action) in model.provisioner.actions'));
    expect(automators.count()).toEqual(0);

    element(by.partialButtonText('Create')).click();

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/services$/);
  });

  it('should verify a service', function () {
    browser.get('/services');
    services = element.all(by.repeater('item in list'));
    expect(services.count()).toEqual(1);

    browser.get('/services/edit/foo');

    expect(element(by.css('#inputServiceName')).getAttribute('value')).toBe('foo');
    expect(element(by.css('#inputServiceDescription')).getAttribute('value')).toBe('bar');
    var provides = element.all(by.repeater('name in model'));
    expect(provides.count()).toEqual(2);
  });

  it('should delete hardwaretype', function () {
    browser.get('/services');
    services = element.all(by.repeater('item in list'));
    services.first().element(by.cssContainingText('.btn', 'Delete')).click();
    element(by.css('.modal-dialog .modal-footer .btn-primary')).click();
    services = element.all(by.repeater('item in list'));
    expect(services.count()).toEqual(0);
  });


  it('should logout', function () {
    helper.logout();
  });

});


