'use strict';

describe('providers list page tests login', function () {
  var ptor = protractor.getInstance();

  describe('shows provider create page', function() {
    var formfields;
    var providersList;
    
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
      formfields = element.all(by.repeater('(name,fieldData) in config.fields'));
      formfields.first().element(by.css('input')).sendKeys('Test resource');
      formfields.get(1).element(by.css('input')).sendKeys('Test email');
      formfields.get(3).element(by.css('input')).sendKeys('Test project id');
      formfields.get(4).element(by.css('input')).sendKeys('Test keyname');
      formfields.get(5).element(by.css('input')).sendKeys('Test username');
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

  });
});


