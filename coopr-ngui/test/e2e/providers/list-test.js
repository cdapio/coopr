'use strict';

describe('providers list page tests login', function () {
  var ptor = protractor.getInstance();
  
  it('should log in', function () {
    browser.get('/#/login');
    var username = element(by.name('username'));
    var password = element(by.name('password'));
    var tenant = element(by.name('tenant'));
    username.clear();
    password.clear();
    tenant.clear();

    username.sendKeys('admin');
    password.sendKeys('admin');
    tenant.sendKeys('superadmin');
    element(by.css('#login-submit-button')).click();
    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/#\/$/);
  });

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
      expect(providers.first().getText()).toEqual('aws-us-east-1 Amazon Web Services - US East 1 region Edit\nDelete');
    });

    describe('buttons', function () {
      
      it('should link to the right pages', function () {
        var edithref = providers.first().element(by.css('.edit-btn')).getAttribute('href');
        expect(edithref).toBe('http://localhost:8080/#/providers/edit/aws-us-east-1');
      });

      it('should delete provider upon clicking delete', function () {
        providers.first().element(by.css('.delete-btn')).click();
        ptor.switchTo().alert().accept();
        providers = element.all(by.repeater('item in list'));
        expect(providers.count()).toEqual(providersCount - 1);
      });
    
    });

  });
});


