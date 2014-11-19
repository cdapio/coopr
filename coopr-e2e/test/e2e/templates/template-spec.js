/**
 * E2E tests for /templates.
 */
var helper = require('../../protractor-help');

'use strict';

describe('templates test', function () {

  var templates;

  it('should log in', function () {
    helper.loginAsAdmin();
  });
  
  it('should show no templates', function () {
    templates = element.all(by.repeater('item in list'));
    expect(templates.count()).toEqual(0);
  });

  it('should create a template', function () {
    // First create provider
    browser.get('/providers/create');
    element(by.name('name')).sendKeys('Testprovider');
    element(by.css('#inputProviderDescription')).sendKeys('Test description');
    element(by.cssContainingText('option', 'google')).click();
    var formfields = element.all(by.repeater('(name,fieldData) in config.fields'));
    formfields.first().element(by.css('input')).sendKeys('Test resource');
    formfields.get(5).element(by.css('input')).sendKeys('Test username');
    formfields.get(1).element(by.css('input')).sendKeys('Test email');
    formfields.get(3).element(by.css('input')).sendKeys('Test project id');
    formfields.get(4).element(by.css('input')).sendKeys('Test keyname');
    element(by.partialButtonText('Create')).click();
    browser.waitForAngular();

    browser.get('/templates/create');
    
    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/templates\/create\/tab\/0$/);

    element(by.css('#inputTemplateName')).sendKeys('foo');
    element(by.css('#inputTemplateDescription')).sendKeys('bar');
    element(by.cssContainingText('fieldset.active button.btn.btn-sm.btn-default', 'Next')).click();

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/templates\/create\/tab\/1$/);

    element(by.cssContainingText('fieldset.active button.btn.btn-sm.btn-default', 'Next')).click();

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/templates\/create\/tab\/2$/);

    element(by.css('#inputTemplateDefaultProvider option:nth-child(2)')).click();
    element(by.cssContainingText('fieldset.active button.btn.btn-sm.btn-default', 'Next')).click();

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/templates\/create\/tab\/3$/);

    element(by.cssContainingText('button.action-btn:not(.ng-hide)', 'Create')).click();

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/templates$/);

    // Delete provider created for test
    browser.get('/providers');
    providersList = element.all(by.repeater('item in list'));
    providersList.first().element(by.cssContainingText('.btn', 'Delete')).click();
    element(by.css('.modal-dialog .modal-footer .btn-primary')).click();
  });

  it('should verify a template', function () {
    browser.get('/templates');
    templates = element.all(by.repeater('item in list'));
    expect(templates.count()).toEqual(1);

    browser.get('/templates/edit/foo');

    expect(element(by.css('#inputTemplateName')).getAttribute('value')).toBe('foo');
    expect(element(by.css('#inputTemplateDescription')).getAttribute('value')).toBe('bar');
  });

  it('should delete a template', function () {
    browser.get('/templates');
    templates = element.all(by.repeater('item in list'));
    templates.first().element(by.cssContainingText('.btn', 'Delete')).click();
    element(by.css('.modal-dialog .modal-footer .btn-primary')).click();
    templates = element.all(by.repeater('item in list'));
    expect(templates.count()).toEqual(0);
  });


  it('should logout', function () {
    helper.logout();
  });

});


