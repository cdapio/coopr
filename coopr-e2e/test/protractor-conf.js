var config = {
  allScriptsTimeout: 11000,

  specs: [
    'e2e/providers/provider-spec.js'
  ],

  capabilities: {
    'browserstack.user': process.env.BROWSER_STACK_USERNAME,
    'browserstack.key': process.env.BROWSER_STACK_ACCESS_KEY,
    'browserstack.local': 'true',
    'browserstack.tunnel': 'true',
    'os' : 'OS X',
    'os_version' : 'Mavericks',
    'resolution' : '1024x768',
    'browserName': 'Chrome'
  },

  

  baseUrl: 'http://localhost:8080/',

  framework: 'jasmine',

  jasmineNodeOpts: {
    defaultTimeoutInterval: 30000
  },

  onPrepare: function() {
    browser.driver.manage().window().maximize();
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
