var config = {
  allScriptsTimeout: 11000,

  specs: [
    'e2e/**/*.js'
  ],

  capabilities: {
    'browserName': 'chrome'
  },

  baseUrl: 'http://localhost:8080/',

  framework: 'jasmine',

  jasmineNodeOpts: {
    defaultTimeoutInterval: 30000
  },

  onPrepare: function() {
    browser.driver.manage().window().maximize();
    browser.driver.executeScript("window.name='PROTRACTOR';");
  }
};

if (process.env.TRAVIS) {
  config.sauceUser = process.env.SAUCE_USERNAME;
  config.sauceKey = process.env.SAUCE_ACCESS_KEY;
  config.capabilities['platform'] = 'Linux';
  config.capabilities['tunnel-identifier'] = process.env.TRAVIS_JOB_NUMBER;
  config.capabilities['build'] = process.env.TRAVIS_BUILD_NUMBER;
  config.capabilities['name'] = "coopr-ngui build#"+process.env.TRAVIS_BUILD_NUMBER;
}


module.exports.config = config;
