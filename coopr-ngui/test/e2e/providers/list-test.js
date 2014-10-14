'use strict';

describe('providers list page tests login', function () {
  var ptor = protractor.getInstance();

  describe('shows provider list page', function() {
    var providers;
    var providersCount = 7;
    
    it('should show a list of providers', function() {
      browser.get('/#/providers');

      expect(
        browser.getLocationAbsUrl()
      ).toMatch(/\/#\/providers$/);

      providers = element.all(by.repeater('item in list'));
      expect(providers.count()).toEqual(providersCount);
      expect(providers.first().getText()).toMatch('aws-us-east-1 Amazon Web Services - US East 1 region Edit\nDelete');
    });

    describe('buttons', function () {
      
      it('should link to the right pages', function () {
        var edithref = providers.first().element(by.partialButtonText('Edit')).getAttribute('href');
        expect(edithref).toMatch(/\/#\/providers\/edit\/aws-us-east-1$/);
      });
    
    });

  });
});


