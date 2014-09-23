'use strict';


describe('redirects', function() {

  it('should go to homepage when navigated to /#/whatever', function() {
    browser.get('/#/whatever');
    expect(browser.getLocationAbsUrl()).toMatch("/#/");
  });

});

