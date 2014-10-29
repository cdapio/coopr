// /**
//  * E2E tests for /providers.
//  */
var helper = require('../../protractor-help');

describe('providers test', function () {
  var ptor = protractor.getInstance();
  var formfields;
  var providersList;
  
  it('should log in', function () {
    browser.get('/login');
    element(by.id('loginTenant')).clear().sendKeys('superadmin');
    element(by.id('loginUsername')).clear().sendKeys('admin');
    element(by.id('loginPassword')).clear().sendKeys('admin');
    element(by.partialButtonText('Submit')).click();
    expect(element(by.css('header .dropdown-toggle .fa-user')).isPresent()).toBe(true);
  });

  it('should show the correct fields for provider type', function() {
    browser.get('/providers/create');

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/providers\/create$/);

    browser.waitForAngular();

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

  it('should create, store and delete a provider', function () {
    // Create
    element(by.name('name')).sendKeys('Testprovider');
    element(by.css('#inputProviderDescription')).sendKeys('Test description');
    element(by.cssContainingText('option', 'google')).click();
    formfields = element.all(by.repeater('(name,fieldData) in config.fields'));
    formfields.first().element(by.css('input')).sendKeys('Test resource');
    formfields.get(5).element(by.css('input')).sendKeys('Test username');
    formfields.get(1).element(by.css('input')).sendKeys('Test email');
    formfields.get(3).element(by.css('input')).sendKeys('Test project id');
    formfields.get(4).element(by.css('input')).sendKeys('Test keyname');
    browser.waitForAngular();
    element(by.css('.action-btn')).click();
    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/providers$/);


    // List
    providersList = element.all(by.repeater('item in list'));
    expect(providersList.count()).toEqual(1);
    var providerText = providersList.first().getText();
    expect(providerText).toMatch('Testprovider Test description Edit\nDelete');
    providersList.first().element(by.cssContainingText('.btn', 'Edit')).click();

    // Edit
    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/providers\/edit\/Testprovider$/);
    expect(element(by.css('#inputProviderName')).getAttribute('value')).toBe('Testprovider');
    expect(
      element(by.css('#inputProviderDescription')).getAttribute('value')).toBe('Test description');
    expect(element(by.css('#inputProviderType')).getAttribute('value')).toBe('');
    formfields = element.all(by.repeater('(name,fieldData) in config.fields'));
    var size = formfields.get(2).element(by.css('input')).getAttribute('value');
    expect(size).toEqual('10');
    var zone = formfields.get(6).element(by.css('select')).getAttribute('value');
    expect(zone).toEqual('5');

    // Delete
    browser.get('/providers');
    providersList = element.all(by.repeater('item in list'));
    providersList.first().element(by.cssContainingText('.btn', 'Delete')).click();
    element(by.css('.modal-dialog .modal-footer .btn-primary')).click();
    browser.waitForAngular();
    providersList = element.all(by.repeater('item in list'));
    expect(providersList.count()).toEqual(0);
  });
  
  it('should show a list of providers', function () {
    browser.get('/providers');

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/providers$/);

    providersList = element.all(by.repeater('item in list'));
    expect(providersList.count()).toEqual(0);
  });

  it('should logout', function () {
    element(by.css('header .navbar-right .dropdown-toggle')).click();
    element(by.css('.dropdown-menu a[ng-click^="logout"]')).click();
  });

});

