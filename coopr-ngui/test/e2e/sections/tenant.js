/**
 * e2e tests for the /tenants section
 */

var helper = require('../../protractor-help');


describe('tenants section', function() {

  it('should redir to login when logged out', function() {
    browser.get('/tenants');

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/login$/);

    expect(
      element(by.css('body')).getAttribute('class')
    ).toContain('state-login');

    expect(
      element(by.css('body')).getAttribute('class')
    ).not.toContain('state-tenants-list');
  });


  it('while logged in....', function() {
    helper.loginAsAdmin();

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


    helper.logout();
  });
});

