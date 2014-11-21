var config = {
  allScriptsTimeout: 60000,

  specs: [
    'e2e/**/*.js'
  ],

  capabilities: {
    'browserstack.user': process.env.BROWSER_STACK_USERNAME,
    'browserstack.key': process.env.BROWSER_STACK_ACCESS_KEY,
    'browserstack.local': 'true',
    'browserstack.debug': 'true',
    'browserstack.tunnel': 'true',
    'os' : 'OS X',
    'os_version' : 'Mavericks',
    'resolution' : '1024x768',
    'browserName': 'chrome'
  },



  baseUrl: 'http://localhost:8080/',

  framework: 'jasmine',

  jasmineNodeOpts: {
    defaultTimeoutInterval: 60000,
    silent: true
  },

  onPrepare: function() {
    browser.driver.manage().window().maximize();
    var SpecReporter = require('jasmine-spec-reporter');
    // add jasmine spec reporter
    jasmine.getEnv().addReporter(new SpecReporter({
      displayStacktrace: true,
      displayFailuresSummary: true
    }));
  }
};

if (process.env.TRAVIS) {

  config.seleniumAddress =  'http://hub.browserstack.com/wd/hub';

  if('BS_AUTOMATE_PROJECT' in process.env) {
    config.capabilities['project'] = process.env['BS_AUTOMATE_PROJECT'];
  }

  if('BS_AUTOMATE_BUILD' in process.env) {
    config.capabilities['build'] = process.env['BS_AUTOMATE_BUILD'];
  }
}


module.exports.config = config;
