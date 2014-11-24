/**
 * E2E tests for /clusters.
 */
var helper = require('../../protractor-help');

'use strict';

describe('cluster types test', function () {

  var clusters;

  it('should log in', function () {
    helper.loginAsAdmin();
  });
  
  it('should go to cluster create page', function () {

    browser.get('/clusters');
    // clusters = element.all(by.repeater('item in list'));
    // expect(clusters.count()).toEqual(0);

    element(by.cssContainingText('a.btn.btn-primary', 'Create a cluster')).click();
    
    expect(
      browser.getLocationAbsUrl()
    ).toMatch(/\/clusters\/create$/);
  });

  it('should logout', function () {
    helper.logout();
  });

});


