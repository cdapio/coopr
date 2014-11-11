/**
 * e2e tests for the /tenants section
 */

var helper = require('../../protractor-help');


describe('tenants section', function() {
  
  it('should log in', function () {
    helper.loginAsAdmin();
  });


  it('should show tenant info', function() {

    browser.get('/tenants');

    expect(
      element(by.css('body')).getAttribute('class')
    ).toContain('state-tenants-list');


    // navigating via hashbang should work as well
    browser.get('/#/tenants/edit/superadmin');

    expect(
      element(by.css('body')).getAttribute('class')
    ).toContain('state-tenants-edit');

    expect(
      element(by.model('model.name')).getAttribute('value')
    ).toBe('superadmin');

  });

  it('should logout', function () {
    helper.logout();
  });

});

