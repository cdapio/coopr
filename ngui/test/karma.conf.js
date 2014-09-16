module.exports = function(config){
  var karma = {

    basePath : '../',

    files : [
      'dist/bundle/lib.js',
      'dist/bundle/app.js',
      'dist/bundle/tpl.js',
      'bower_components/angular-mocks/angular-mocks.js',
      'app/js/**/*.spec.js',
      'test/unit/**/*.js'
    ],

    autoWatch : true,

    frameworks: ['jasmine'],

    browsers : ['Chrome'],

    plugins : [
            'karma-chrome-launcher',
            'karma-jasmine'
            ],

    customLaunchers: {
      Chrome_travis_ci: {
        base: 'Chrome',
        flags: ['--no-sandbox']
      }
    },

    reporters: ['progress']

  };

  if(process.env.TRAVIS){
    karma.browsers = ['Chrome_travis_ci'];
  }

  config.set(karma);
};
