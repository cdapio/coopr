/**
 * e2e tests for the login functionality
 */

var helper = require('../protractor-help');

describe('login', function() {

  it('should show a form that works', function() {
    helper.logout();

    browser.get('/login');

    expect( 
      element.all(by.css('main form input')).count()
    ).toBe(4); // tenant, username, password, remember

    helper.loginAsAdmin();

    expect( 
      element(
        by.css('header .dropdown-toggle .fa-user')
      ).isPresent()
    ).toBe(true);

  });


  describe('once logged in', function() {

    it('/login should redirect home', function() {
      browser.get('/login');

      expect(
        element(by.css('body')).getAttribute('class')
      ).toContain('state-home');

      expect(
        browser.getLocationAbsUrl()
      ).not.toMatch(/\/login$/);
    });



    it('can log out', function() {
      helper.logout();

      expect( 
        element(
          by.css('header .dropdown-toggle .fa-user')
        ).isPresent()
      ).toBe(false);

    });

  });
});

