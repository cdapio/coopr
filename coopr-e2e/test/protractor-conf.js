var config = {
  allScriptsTimeout: 11000,

  specs: [
    'e2e/providers/provider-spec.js'
  ],

  capabilities: {

    // Settings for the browser you want to test
  },

  seleniumAddress: 'http://hub.browserstack.com/wd/hub',

  baseUrl: 'http://localhost:8080/',

  framework: 'jasmine',

  jasmineNodeOpts: {
    defaultTimeoutInterval: 30000
  },

  onPrepare: function() {
    browser.driver.manage().window().maximize();
  }
};

if(process.env.TRAVIS) {
  if('BS_AUTOMATE_PROJECT' in process.env) {
    config.capabilities['project'] = process.env['BS_AUTOMATE_PROJECT'];  
  }

  if('BS_AUTOMATE_BUILD' in process.env) {
    config.capabilities['build'] = process.env['BS_AUTOMATE_BUILD'];  
  }

  if('SELENIUM_VERSION' in process.env) {
    config.capabilities['browser_version'] = process.env['SELENIUM_VERSION'];  
  }
  
  config.capabilities['platform'] = process.env['SELENIUM_PLATFORM'] ? process.env['SELENIUM_PLATFORM'] : 'ANY';
  config.capabilities['browser'] = process.env['SELENIUM_BROWSER'] ? process.env['SELENIUM_BROWSER'] : 'chrome';
  config.capabilities['browserstack.user'] = process.env.BROWSER_STACK_USERNAME;
  config.capabilities['browserstack.key'] = process.env.BROWSER_STACK_ACCESS_KEY;
  // config.sauceUser = process.env.SAUCE_USERNAME;
  // config.sauceKey = process.env.SAUCE_ACCESS_KEY;
  // config.capabilities['tunnel-identifier'] = process.env.TRAVIS_JOB_NUMBER;
  // config.capabilities['build'] = process.env.TRAVIS_BUILD_NUMBER;
  // config.capabilities['name'] = "coopr-ngui build#"+process.env.TRAVIS_BUILD_NUMBER;
  // 'browser' : 'Chrome'
}


module.exports.config = config;
