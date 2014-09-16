/**
 * Spins up two web servers
 *   - http-server sends the static assets in dist and handles the /config.js endpoint
 *   - cors-proxy adds the necessary headers for xdomain access to the REST API
 */

var pkg = require('./package.json'),
    morgan = require('morgan'),
    COOPR_UI_PORT = parseInt(process.env.COOPR_UI_PORT || 8080, 10),
    COOPR_CORS_PORT = parseInt(process.env.COOPR_CORS_PORT || 8081, 10),

    configStr = JSON.stringify({
      // the following will be available in angular via the "MY_CONFIG" injectable

      COOPR_SERVER_URI: process.env.COOPR_SERVER_URI || 'http://127.0.0.1:55054',
      COOPR_CORS_PORT: COOPR_CORS_PORT,
      COOPR_UI_PORT: COOPR_UI_PORT

    });


var hilite = function (v) { return '\x1B[7m' + v + '\x1B[27m'; },
    httpLabel = '\x1B[40m\x1B[32mhttp-server\x1B[39m\x1B[49m',
    corsLabel = '\x1B[40m\x1B[35mcors-proxy\x1B[39m\x1B[49m',
    httpLogger = morgan(httpLabel+' :method :url', {immediate: true}),
    corsLogger = morgan(corsLabel+' :req[X-Loom-UserID]/:req[X-Loom-TenantID] :method :url '+hilite(':status'));

console.log(hilite(pkg.name) + ' v' + pkg.version + ' starting up...');

/**
 * HTTP server
 */
require('http-server')
  .createServer({
    root: __dirname + '/dist',
    before: [
      httpLogger,
      function (req, res) {
        if(req.url !== '/config.js') {
          // all other paths are passed to ecstatic
          return res.emit('next');
        }
        res.writeHead(200, { 
          'Content-Type': 'application/javascript',
          'Cache-Control': 'no-store, must-revalidate'
        });
        res.end('angular.module("' + pkg.name + '.config", [])'
          + '.constant("MY_CONFIG", ' + configStr +');'); 
      }
    ]
  })
  .listen(COOPR_UI_PORT, '0.0.0.0', function () {
    console.log(httpLabel+' listening on port %s', COOPR_UI_PORT);
  });


/**
 * CORS proxy
 */
require('cors-anywhere')
  .createServer({
    requireHeader: ['x-requested-with'],
    removeHeaders: ['cookie', 'cookie2']
  })
  .on('request', function (req, res) {
    corsLogger(req, res, function noop() {} );
  })
  .listen(COOPR_CORS_PORT, '0.0.0.0', function() {
    console.log(corsLabel+' listening on port %s', COOPR_CORS_PORT);
  });
