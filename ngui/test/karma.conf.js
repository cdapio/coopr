module.exports = function(config){
  config.set({

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

    reporters: ['progress']

  });
};
