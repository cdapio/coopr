/**
 * e2e tests for generic redirects
 */

describe('redirects', function() {

  it('should show a 404 when navigatin to /#/whatever', function() {
    browser.get('/whatever');

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/whatever$/);

    expect(
      element(by.css('main .jumbotron')).getText()
    ).toContain("Page not found");

  });

  it('should go to login when navigated to /#/signin', function() {
    browser.get('/signin');

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/login$/);

  });
});

