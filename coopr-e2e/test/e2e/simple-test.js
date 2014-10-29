/**
 * Simple test case for sauce.
 */


describe('Simple test cases', function() {

  it('should pass', function() {
    browser.get('/');

    expect(true).toBe(true);
  });

});

