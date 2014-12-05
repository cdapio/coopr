/**
 * Spins up a web server which will:
 *   - send the static assets in dist
 *   - handles the /config.json endpoint
 *   - implement proxy at /proxy endpoint
 */

var COOPR_HOME = process.env.COOPR_HOME || (__dirname + '/../'),
    COOPR_SSL = ('true' === process.env.COOPR_SSL),
    COOPR_UI_PORT = ( COOPR_SSL
      ? parseInt(process.env.COOPR_UI_SSL_PORT || 8443, 10)
      : parseInt(process.env.COOPR_UI_PORT || 8080, 10)
    ),
    COOPR_UI_KEY_FILE = process.env.COOPR_UI_KEY_FILE || COOPR_HOME + 'cert/server.key',
    COOPR_UI_CERT_FILE = process.env.COOPR_UI_CERT_FILE || COOPR_HOME + 'cert/server.crt',
    COOPR_SERVER_URI = process.env.COOPR_SERVER_URI || 'http://127.0.0.1:55054';

var pkg = require('./package.json'),
    fs = require('fs'),
    morgan = require('morgan'),
    http = require('http'),
    https = require('https'),
    proxy = require('http-proxy').createProxyServer({
      target: COOPR_SERVER_URI
    }),
    express = require('express'),
    finalhandler = require('finalhandler'),
    serveFavicon = require('serve-favicon'),
    color = {
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
    httpStaticLogger = morgan(httpLabel + ' :method :url :status'),
    httpIndexLogger = morgan(httpLabel + ' :method ' + color.hilite(':url') + ' :status'),
    httpProxyLogger = morgan(color.pink('prox') + ' :method :url :cooprcred :status');

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


// survive in case of proxy error
proxy.on('error', function (err, req, res) {
  res.status(500).send('Proxy ' + (err.message || 'fail'));
});

// proxy requests to the backend
app.all('/proxy/*', [
    httpProxyLogger,
    function (req, res) {
        req.url = req.url.substr(6);
        proxy.web(req, res);
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


