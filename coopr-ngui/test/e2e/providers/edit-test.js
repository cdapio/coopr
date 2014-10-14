'use strict';

describe('shows provider update page', function() {
  var ptor = protractor.getInstance();
  var formfields;
  
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

  it('should submit provider update form', function () {
    formfields.first().element(by.css('input')).clear().sendKeys('apifoo');
    formfields.get(1).element(by.css('input')).clear().sendKeys('emailfoo');
    formfields.get(2).element(by.css('input')).clear().sendKeys(12);
    formfields.get(3).element(by.css('input')).clear().sendKeys('idfoo');
    formfields.get(4).element(by.css('input')).clear().sendKeys('sshfoo');
    formfields.get(5).element(by.css('input')).clear().sendKeys('resourcefoo');
    element(by.partialButtonText('Update')).click();
    
    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/#\/providers$/);    
  });

  // it('should show new provider data', function () {
  //   browser.get('/#/providers/edit/google');
    
  //   expect(
  //     browser.getLocationAbsUrl()
  //   ).toMatch(/\/#\/providers\/edit\/google$/);

  //   formfields = element.all(by.repeater('(name,fieldData) in config.fields'));
  //   expect(formfields.first().element(by.css('input'))).toBe('apifoo');
  //   expect(formfields.get(1).element(by.css('input'))).toBe('12');
  //   expect(formfields.get(2).element(by.css('input'))).toBe('idfoo');
  //   expect(formfields.get(3).element(by.css('input'))).toBe('sshfoo');
  //   expect(formfields.get(4).element(by.css('input'))).toBe('resourcefoo');

  //   // !TODO call /reset to get default data back to normal once default data has been implemented.
  //   // browser.get('/#/reset');
  // });

});