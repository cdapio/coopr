Coopr Angular UI
================

### for development:

`npm install && bower install`

`COOPR_USE_DUMMY_PROVISIONER=true ../standalone/target/.../bin/coopr.sh start`

and then, each in their own tab:

`gulp watch` (autobuild + livereload)

`npm start` (http-server + cors-anywhere)

`npm test` (run karma for unit tests)

`open http://localhost:8080`

* in dev mode, UI runs on port `8080` and connects to livereload on port `35729`
* cors-anywhere always runs on port `8081`
* coopr server is expected to be running on port `55054`

### for testing:

`npm run protractor` (end-to-end tests)

`npm run test-single-run` (unit tests)

protractor spins up a server on port `9090`

### for production:

first make a clean build into `./dist` folder:

`npm install && bower install`

`gulp clean`

`gulp build minify`

then to run the server, possibly on a different host:

`npm install --production`

`COOPR_UI_PORT=8100 npm start` (http-server + cors-anywhere)
