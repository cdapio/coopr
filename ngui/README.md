Coopr Angular UI
================

### for development:

`cd standalone`

`mvn clean package assembly:single`

`open target` and unzip the _SNAPSHOT-standalone_ file

`LOOM_USE_DUMMY_PROVISIONER=true LOOM_DISABLE_UI=true target/(...)/bin/loom.sh start -f 50`

`cd ../ngui`

`npm install && bower install` (assumes you have `bower` installed globally)

then, each in their own tab:

`gulp watch` (autobuild + livereload)

`npm start` (http-server + cors-anywhere)

`npm test` (run karma for unit tests)

`open http://localhost:8080`

* in dev mode, UI runs on port `8080` and connects to livereload on port `35729`
* cors-anywhere runs on port `8081`
* loom server is expected to be running on port `55054`

### for testing:

`npm run protractor` (end-to-end tests)

`npm run test-single-run` (unit tests)

protractor spins up a server on port `9090`

### for production:

first make a clean build into `./dist` folder:

`cd ngui`

`npm install && bower install`

`gulp clean`

`gulp build minify`

then to run the server, possibly on a different host:

`cd ngui`

`npm install --production`

`export COOPR_UI_PORT=8100`

`export COOPR_SERVER_URI=http://hostname:port`

`npm start` or `node server.js`

