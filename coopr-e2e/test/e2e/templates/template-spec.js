/**
 * E2E tests for /templates.
 */
var helper = require('../../protractor-help');

'use strict';

describe('templates test', function () {

  var templates,
      template,
      provider;

  it('should log in', function () {
    helper.loginAsAdmin();
  });

  it('should show no templates', function () {
    templates = element.all(by.repeater('item in list'));
    expect(templates.count()).toEqual(0);
  });

  it('should create a template', function () {
    var providerNames;
    provider = Date.now() + '-provider';
    // First create provider
    browser.get('/providers/create');
    element(by.name('name')).sendKeys(provider);
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

    template = Date.now() + '-template';
    element(by.css('#inputTemplateName')).sendKeys(template);
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
    providerNames = element.all(by.repeater('item in list').column('item.name'));
    helper.deleteAssetFromList(providerNames, provider)
  });

  it('should verify a template', function () {
    browser.get('/templates');
    var templateNames = element.all(by.repeater('item in list').column('item.name'));
    helper.editAssetFromList(templateNames, template);

    expect(element(by.css('#inputTemplateName')).getAttribute('value')).toBe(template);
    expect(element(by.css('#inputTemplateDescription')).getAttribute('value')).toBe('bar');
  });

  it('should delete a template', function () {
    browser.get('/templates');
    var templateNames = element.all(by.repeater('item in list').column('item.name'));
    var templateCount = templateNames.count();
    helper.deleteAssetFromList(templateNames, template);

    expect(templateCount.then(function(i) {
      return i - 1;
    })).toBe(
      templateNames.count()
    ); //Lame..
  });


  it('should logout', function () {
    helper.logout();
  });

});
