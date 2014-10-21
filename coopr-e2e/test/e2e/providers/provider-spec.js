/**
 * E2E tests for /providers.
 */
var helper = require('../../protractor-help');

'use strict';

describe('providers test', function () {
  var ptor = protractor.getInstance();
  var formfields;
  var providersList;
  
  it('should log in', function () {
    helper.loginAsAdmin();
  });

  it('should show the correct fields for provider type', function() {
    browser.get('/#/providers/create');

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/#\/providers\/create$/);

    element(by.cssContainingText('option', 'google')).click();
    formfields = element.all(by.repeater('(name,fieldData) in config.fields'));
    expect(formfields.count()).toEqual(7);

    element(by.cssContainingText('option', 'aws')).click();
    formfields = element.all(by.repeater('(name,fieldData) in config.fields'));
    expect(formfields.count()).toEqual(15);
  });

  it('should fill in default values', function () {
    element(by.cssContainingText('option', 'google')).click();
    formfields = element.all(by.repeater('(name,fieldData) in config.fields'));
    var size = formfields.get(2).element(by.css('input')).getAttribute('value');
    expect(size).toEqual('10');
    var zone = formfields.get(6).element(by.css('select')).getAttribute('value');
    expect(zone).toEqual('5');
  });

  it('should create a provider', function () {
    element(by.name('name')).sendKeys('Testprovider');
    element(by.css('#inputProviderDescription')).sendKeys('Test description');
    element(by.cssContainingText('option', 'google')).click();
    formfields = element.all(by.repeater('(name,fieldData) in config.fields'));
    formfields.first().element(by.css('input')).sendKeys('Test resource');
    formfields.get(5).element(by.css('input')).sendKeys('Test username');
    formfields.get(1).element(by.css('input')).sendKeys('Test email');
    formfields.get(3).element(by.css('input')).sendKeys('Test project id');
    formfields.get(4).element(by.css('input')).sendKeys('Test keyname');
    element(by.partialButtonText('Create')).click();

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/#\/providers$/);

    providersList = element.all(by.repeater('item in list'));
    expect(providersList.count()).toEqual(8);
    var providerText = providersList.last().getText();
    expect(providerText).toMatch('Testprovider Test description Edit\nDelete');
  });


  it('should delete provider upon clicking delete', function () {
    providersList.last().element(by.css('.delete-btn')).click();
    ptor.switchTo().alert().accept();
    providersList = element.all(by.repeater('item in list'));
    expect(providersList.count()).toEqual(7);
  });

  it('should navigate to provider edit page and show fields.', function() {
    browser.get('/#/providers/edit/google');

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/#\/providers\/edit\/google$/);

    expect(element(by.css('#inputProviderName')).getAttribute('value')).toBe('google');
    expect(element(by.css('#inputProviderDescription')).getAttribute('value')).toBe('Google Compute Engine');
    expect(element(by.css('#inputProviderType')).getAttribute('value')).toBe('1');
    formfields = element.all(by.repeater('(name,fieldData) in config.fields'));
    var size = formfields.get(2).element(by.css('input')).getAttribute('value');
    expect(size).toEqual('10');
    var zone = formfields.get(6).element(by.css('select')).getAttribute('value');
    expect(zone).toEqual('5'); 
  });

  // !TODO Add these tests once reset endpoint becomes available.

  // it('should submit provider update form', function () {
  //   formfields.first().element(by.css('input')).clear().sendKeys('apifoo');
  //   formfields.get(1).element(by.css('input')).clear().sendKeys('emailfoo');
  //   formfields.get(2).element(by.css('input')).clear().sendKeys(12);
  //   formfields.get(3).element(by.css('input')).clear().sendKeys('idfoo');
  //   formfields.get(4).element(by.css('input')).clear().sendKeys('sshfoo');
  //   formfields.get(5).element(by.css('input')).clear().sendKeys('resourcefoo');
  //   element(by.partialButtonText('Update')).click();
    
  //   expect(
  //     browser.getLocationAbsUrl()
  //   ).toMatch(/\/#\/providers$/);
  // });

  // it('should show new provider data', function () {
  //   browser.get('/#/providers/edit/google');
    
  //   expect(
  //     browser.getLocationAbsUrl()
  //   ).toMatch(/\/#\/providers\/edit\/google$/);

  //   formfields = element.all(by.repeater('(name,fieldData) in config.fields'));
  //   expect(formfields.first().element(by.css('input')).getAttribute('value')).toBe('apifoo');
  //   expect(formfields.get(1).element(by.css('input')).getAttribute('value')).toBe('emailfoo');
  //   expect(formfields.get(2).element(by.css('input')).getAttribute('value')).toBe('12');
  //   expect(formfields.get(3).element(by.css('input')).getAttribute('value')).toBe('idfoo');
  //   expect(formfields.get(4).element(by.css('input')).getAttribute('value')).toBe('sshfoo');
  //   expect(formfields.get(5).element(by.css('input')).getAttribute('value')).toBe('resourcefoo');

  //   // !TODO call /reset to get default data back to normal once default data has been implemented.
  //   // browser.get('/#/reset');
  // });
  
  it('should show a list of providers', function() {
    browser.get('/#/providers');

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/#\/providers$/);

    providersList = element.all(by.repeater('item in list'));
    expect(providersList.count()).toEqual(7);
    expect(providersList.first().getText()).toMatch('aws-us-east-1 Amazon Web Services - US East 1 region Edit\nDelete');
  });

  it('should link to the right pages', function () {
    var edithref = providersList.first().element(by.css('.edit-btn')).getAttribute('href');
    expect(edithref).toMatch(/\/#\/providers\/edit\/aws-us-east-1$/);
  });

  it('should logout', function () {
    helper.logout();
  });

});


