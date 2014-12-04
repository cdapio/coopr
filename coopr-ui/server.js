/**
 * Spins up two web servers
 *   - http-server sends the static assets in dist and handles the /config.json endpoint
 *   - cors-proxy adds the necessary headers for xdomain access to the REST API
 */

var pkg = require('./package.json'),
    fs = require('fs'),
    morgan = require('morgan'),
    http = require('http'),
    https = require('https'),
    express = require('express'),
    finalhandler = require('finalhandler'),
    serveFavicon = require('serve-favicon'),
    corsAnywhere = require('cors-anywhere');

var COOPR_HOME = process.env.COOPR_HOME || (__dirname + '/../'),
    COOPR_SSL = ('true' === process.env.COOPR_SSL),
    COOPR_UI_PORT = ( COOPR_SSL
      ? parseInt(process.env.COOPR_UI_SSL_PORT || 8443, 10)
      : parseInt(process.env.COOPR_UI_PORT || 8080, 10)
    ),
    COOPR_UI_KEY_FILE = process.env.COOPR_UI_KEY_FILE || COOPR_HOME + 'cert/server.key',
    COOPR_UI_CERT_FILE = process.env.COOPR_UI_CERT_FILE || COOPR_HOME + 'cert/server.crt',
    COOPR_CORS_PORT = parseInt(process.env.COOPR_CORS_PORT || 8081, 10),
    COOPR_SERVER_URI = process.env.COOPR_SERVER_URI || 'http://127.0.0.1:55054';

COOPR_SERVER_URI = COOPR_SERVER_URI.replace('/localhost:', '/127.0.0.1:');

var color = {
        hilite: function (v) {
            return '\x1B[7m' + v + '\x1B[27m';
        },
        green: function (v) {
            return '\x1B[40m\x1B[32m' + v + '\x1B[39m\x1B[49m';
        },
        pink: function (v) {
            return '\x1B[40m\x1B[35m' + v + '\x1B[39m\x1B[49m';
        }
    };


morgan.token('cooprcred', function (req, res) {
    return color.pink(req.headers['coopr-userid'] + '/' + req.headers['coopr-tenantid']);
});

var httpLabel = color.green('http'),
    corsLabel = color.pink('cors'),
    httpStaticLogger = morgan(httpLabel + ' :method :url :status'),
    httpIndexLogger = morgan(httpLabel + ' :method ' + color.hilite(':url') + ' :status'),
    corsLogger = morgan(corsLabel + ' :method :url :cooprcred :status', {
        skip: function (req, res) {
            return req.method === 'OPTIONS'
        }
    });

console.log(color.hilite(pkg.name) + ' v' + pkg.version + ' starting up...');

/**
 * Express application
 */

var app = express();

try { app.use(serveFavicon(__dirname + '/dist/assets/img/favicon.png')); }
catch(e) { console.error("Favicon missing! Did you run `gulp build`?"); }

// serve the config file
app.get('/config.js', function (req, res) {
    var data = JSON.stringify({
        // the following will be available in angular via the "MY_CONFIG" injectable

        COOPR_SERVER_URI: COOPR_SERVER_URI,
        COOPR_CORS_PORT: COOPR_CORS_PORT,
        authorization: req.headers.authorization

    });
    res.header({
        'Content-Type': 'text/javascript',
        'Cache-Control': 'no-store, must-revalidate'
    });
    res.send('angular.module("' + pkg.name + '.config", [])' +
        '.constant("MY_CONFIG",' + data + ');');
});

// serve static assets
app.use('/assets', [
  httpStaticLogger,
  express.static(__dirname + '/dist/assets', {
    index: false
  }),
  function(req, res) {
    finalhandler(req, res)(false); // 404
  }
]);

app.get('/robots.txt', [
    httpStaticLogger,
    function (req, res) {
        res.type('text/plain');
        res.send('User-agent: *\nDisallow: /');
    }
]);

// any other path, serve index.html
app.all('*', [
    httpIndexLogger,
    function (req, res) {
        res.sendFile(__dirname + '/dist/index.html');
    }
]);


/**
 * HTTP(S) server
 */
var server;
if (COOPR_SSL) {
    server = https.createServer({
        key: fs.readFileSync(COOPR_UI_KEY_FILE, 'utf-8'),
        cert: fs.readFileSync(COOPR_UI_CERT_FILE, 'utf-8')
    }, app);
}
else {
    server = http.createServer(app);
}

server.listen(COOPR_UI_PORT, null, null, function () {
    console.info(httpLabel + ' listening on port %s', COOPR_UI_PORT);
});


/**
 * CORS proxy
 */
corsAnywhere.createServer({
    requireHeader: ['x-requested-with'],
    removeHeaders: ['cookie', 'cookie2']
})
    .on('request', function (req, res) {
        corsLogger(req, res, function noop() {});
    })
    .listen(COOPR_CORS_PORT, '0.0.0.0', function () {
        console.info(corsLabel + ' listening on port %s', COOPR_CORS_PORT);
    });
