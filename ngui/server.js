/**
 * Spins up two web servers
 *   - http-server sends the static assets in dist and handles the /config.json endpoint
 *   - cors-proxy adds the necessary headers for xdomain access to the REST API
 */

var pkg = require('./package.json'),
    morgan = require('morgan'),

    COOPR_UI_PORT = parseInt(process.env.COOPR_UI_PORT || 8080, 10),
    COOPR_CORS_PORT = parseInt(process.env.COOPR_CORS_PORT || 8081, 10),

    color = {
      hilite: function (v) { return '\x1B[7m' + v + '\x1B[27m'; },
      green: function (v) { return '\x1B[40m\x1B[32m' + v + '\x1B[39m\x1B[49m'; },
      pink: function (v) { return '\x1B[40m\x1B[35m' + v + '\x1B[39m\x1B[49m'; }
    };


morgan.token('loomcred', function(req, res){ 
  return color.pink(req.headers['x-loom-userid'] + '/' + req.headers['x-loom-tenantid']); 
});

var httpLabel = color.green('http-server'),
    corsLabel = color.pink('cors-proxy'),
    httpLogger = morgan(httpLabel+' :method :url', {immediate: true}),
    corsLogger = morgan(corsLabel+' :method :url :loomcred :status', {
      skip: function(req, res) { return req.method === 'OPTIONS' }
    });

console.log(color.hilite(pkg.name) + ' v' + pkg.version + ' starting up...');

/**
 * HTTP server
 */
require('http-server')
  .createServer({
    root: __dirname + '/dist',
    before: [
      httpLogger,
      function (req, res) {
        if(req.url !== '/config.json') {
          // all other paths are passed to ecstatic
          return res.emit('next');
        }
        res.writeHead(200, { 
          'Content-Type': 'application/json',
          'Cache-Control': 'no-store, must-revalidate'
        });
        var cooprServerURI = process.env.COOPR_SERVER_URI || 'http://localhost:55044';
        cooprServerURI += '/v2/';
        res.end(JSON.stringify({
          // the following will be available in angular via the "MY_CONFIG" injectable

          COOPR_SERVER_URI: cooprServerURI,
          COOPR_CORS_PORT: COOPR_CORS_PORT,
          authorization: req.headers.authorization

        }));
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
