// /**
//  * E2E tests for /providers.
//  */
var helper = require('../../protractor-help');


describe('providers test', function () {
  var formfields;
  var providersList,
      provider;

  it('should log in', function () {
    helper.loginAsAdmin();
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

  it('should create a provider', function () {
    // Create
    provider = Date.now() + '-provider';
    element(by.name('name')).sendKeys(provider);
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
    ).toMatch(/\/providers$/);

  });

  it('should edit a provider', function() {
    var providerNames;

    // List
    providerNames = element.all(by.repeater('item in list').column('item.name'));
    helper.editAssetFromList(providerNames, provider);

    // Edit
    expect(
      browser.getLocationAbsUrl()
    ).toMatch(new RegExp('/providers/edit/'+ provider + '$' ));
    expect(element(by.css('#inputProviderName')).getAttribute('value')).toBe(provider);
    expect(
      element(by.css('#inputProviderDescription')).getAttribute('value')).toBe('Test description');
    formfields = element.all(by.repeater('(name,fieldData) in config.fields'));
    var size = formfields.get(2).element(by.css('input')).getAttribute('value');
    expect(size).toEqual('10');
    var zone = formfields.get(6).element(by.css('select')).getAttribute('value');
    expect(zone).toEqual('5');
  });

  it('should delete a provider', function() {
    var selectedService,
        servicesCount;
    browser.get('/providers');
      provider = "1416350481883-provider";
    var providerNames = element.all(by.repeater('item in list').column("item.name"));

    providersCount = providerNames.count();
    helper.deleteAssetFromList(providerNames, provider);
    expect(providersCount.then(function(i) {
      return i - 1;
    })).toBe(
      providerNames.count()
    ); //Lame..

  });

  it('should logout', function () {
    helper.logout();
  });

});
