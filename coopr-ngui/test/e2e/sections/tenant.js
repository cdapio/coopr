/**
 * e2e tests for the /tenants section
 */

var helper = require('../../protractor-help');


describe('tenants section', function() {

  it('should redir to login when logged out', function() {
    browser.get('/#/tenants');

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/#\/login/);

    expect(
      element(by.css('body')).getAttribute('class')
    ).toContain('state-login');

    expect(
      element(by.css('body')).getAttribute('class')
    ).not.toContain('state-tenants-list');
  });


  describe('when logged in', function() {

    beforeEach(helper.loginAsAdmin);
    afterEach(helper.logout);
    // beforeAll/afterAll not in Jasmine yet... but soon?
    // cf https://github.com/angular/protractor/issues/103


    it('/tenants renders list view', function() {
      browser.get('/#/tenants');

      expect(
        element(by.css('body')).getAttribute('class')
      ).toContain('state-tenants-list');

    });

    it('/tenants/edit/superadmin renders edit view', function() {
      browser.get('/#/tenants/edit/superadmin');

      expect(
        element(by.css('body')).getAttribute('class')
      ).toContain('state-tenants-edit');

      expect(
        element(by.model('model.name')).getAttribute('value')
      ).toBe('superadmin');

    });

  });
});

