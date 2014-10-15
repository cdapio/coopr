/**
 * e2e tests for the login functionality
 */

var helper = require('../protractor-help');

describe('login', function() {

  it('should show a form', function() {
    browser.get('/#/login');

    expect( 
      element.all(by.css('main form input')).count()
    ).toBe(4); // tenant, username, password, remember

  });



  describe('when logged in', function() {
    beforeEach(helper.loginAsAdmin);
    afterEach(helper.logout);

    it('should redirect home', function() {
      browser.get('/#/login');

      expect(
        $('body').getAttribute('class')
      ).toContain('state-home');

      expect(
        browser.getLocationAbsUrl()
      ).toMatch(/\/#\/$/);
    });

  });
});

