var port = process.env.COOPR_UI_PORT || 8080,
    httpLabel = '\x1B[40m\x1B[32mhttp-server\x1B[39m\x1B[49m',
    corsLabel = '\x1B[40m\x1B[35mcors-anywhere\x1B[39m\x1B[49m';

require('http-server')
  .createServer({
    root: __dirname + '/dist',
    logFn: mkLog(httpLabel)
  })
  .listen(port, '0.0.0.0', function () {
    console.log(httpLabel+' listening on port %s', port);
  });


require('cors-anywhere')
  .createServer({
    requireHeader: ['x-requested-with'],
    removeHeaders: ['cookie', 'cookie2']
  })
  .listen(8081, '0.0.0.0', function() {
    console.log(corsLabel+' listening on port 8081');
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

