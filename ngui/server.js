var pkg = require('./package.json'),
    uiPort = parseInt(process.env.COOPR_UI_PORT || 8080, 10),
    corsPort = parseInt(process.env.COOPR_CORS_PORT || 8081, 10),
    httpLabel = '\x1B[40m\x1B[32mhttp-server\x1B[39m\x1B[49m',
    corsLabel = '\x1B[40m\x1B[35mcors-anywhere\x1B[39m\x1B[49m',
    httpLogger = mkLog(httpLabel);

console.log('\x1B[7m' + pkg.name + '\x1B[27m v' + pkg.version + ' starting up...');

/**
 * HTTP server
 */
require('http-server')
  .createServer({
    root: __dirname + '/dist',
    logFn: httpLogger,
    before: [function (req, res) {
      if(req.url !== '/config.js') {
        // all other paths are passed to ecstatic
        return res.emit('next');
      }
      httpLogger(req, res);
      res.writeHead(200, { 
        'Content-Type': 'application/javascript',
        'Cache-Control': 'no-store, must-revalidate'
      });
      res.end('angular.module("' + pkg.name + '.config", [])'
        + '.constant("MY_CONFIG", ' + JSON.stringify({
          COOPR_SERVER_URI: process.env.COOPR_SERVER_URI || 'http://127.0.0.1:55054',
          COOPR_CORS_PORT: corsPort,
          COOPR_UI_PORT: uiPort
        }) +');');
    }]
  })
  .listen(uiPort, '0.0.0.0', function () {
    console.log(httpLabel+' listening on port %s', uiPort);
  });


/**
 * CORS proxy
 */
require('cors-anywhere')
  .createServer({
    requireHeader: ['x-requested-with'],
    removeHeaders: ['cookie', 'cookie2']
  })
  .listen(corsPort, '0.0.0.0', function() {
    console.log(corsLabel+' listening on port %s', corsPort);
  })
  .on('request', mkLog(corsLabel));


function mkLog(label) {
  return function (req) {
    console.log(
      '\n%s %s\n\x1B[1m%s\x1B[22m \x1B[36m%s\x1B[39m',
      label,
      (new Date).toUTCString(),
      req.method,
      req.url
    );
  }
}

