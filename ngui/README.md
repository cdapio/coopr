Coopr Angular UI
================

### for development:

`cd standalone`

`mvn clean package assembly:single`

`open target` and unzip the _SNAPSHOT-standalone_ file

`COOPR_USE_DUMMY_PROVISIONER=true COOPR_DISABLE_UI=true target/(...)/bin/coopr.sh start -f 50`

`cd ../ngui`

`npm install && bower install` (assumes you have `bower` installed globally)

then, each in their own tab:

`gulp watch` (autobuild + livereload)

`npm start` (http-server + cors-anywhere)

`npm test` (run karma for unit tests)

`open http://localhost:8080`

* in dev mode, UI runs on port `8080` and connects to livereload on port `35729`
* cors-anywhere always runs on port `8081`
* coopr server is expected to be running on port `55054`

### for testing:

`cd ngui`

`npm run build` ( == `npm install && bower install && gulp build`)

`npm run protractor` (end-to-end tests)

`npm run test-single-run` (unit tests)

protractor spins up a server on port `9090`

### for production:

`cd ngui`

`npm run build` ( == `npm install && bower install && gulp build`)

`gulp minify`

the above generates the `ngui/dist/` folder, which contains all static assets.

now to run the server, possibly on a different host:

`cd ngui`

`npm install --production` (will skip devDependencies)

`export COOPR_UI_PORT=8100`

`export COOPR_SERVER_URI=http://hostname:port`

`npm start` or `node server.js`

