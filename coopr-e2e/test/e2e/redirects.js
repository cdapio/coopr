/**
 * e2e tests for generic redirects
 */

describe('redirects', function() {

  it('should show a soft 404 when navigatin to /whatever', function() {
    browser.get('/whatever');

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/whatever$/);

    expect(
      element(by.css('main .jumbotron')).getText()
    ).toContain("Page not found");

  });

  it('should show a real 404 for static paths', function() {
    // there is no angular here, so we have to use the driver directly
    browser.driver.get(browser.baseUrl + 'assets/bundle/foo.bar');

    expect(
      browser.driver.getCurrentUrl()
    ).toMatch(/\/assets\/bundle\/foo.bar$/);

    expect(
      browser.driver.getPageSource()
    ).toContain('Cannot GET /assets/bundle/foo.bar');

  });

  it('should go to login when navigated to /signin', function() {
    browser.get('/signin');

    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/login$/);

  });
});
