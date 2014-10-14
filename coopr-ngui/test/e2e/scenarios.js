'use strict';


describe('redirects', function() {

  it('should show a 404 when navigatin to /#/whatever', function() {
    browser.get('/#/whatever');

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/#\/whatever$/);

    expect(
      element(by.css('main .jumbotron')).getText()
    ).toMatch("Page not found");

  });

  it('should go to login when navigated to /#/signin', function() {
    browser.get('/#/signin');

    // Already signed in.
    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/#\/$/);

  });
});

