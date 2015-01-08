Coopr Angular UI
================

### for development:

first you need a server

* `cd coopr-standalone`
* `mvn clean package assembly:single`
* `open target` and unzip the _SNAPSHOT-standalone_ file
* `COOPR_USE_DUMMY_PROVISIONER=true COOPR_DISABLE_UI=true target/(...)/bin/coopr.sh start -f 50`

## once that is running, to work on the frontend:

Global dependencies:

* `npm install -g bower gulp`

Local dependencies:

* `cd ../ui`
* `npm install && bower install`

then, each in their own tab:

* `gulp watch` (autobuild + livereload)
* `npm start` (http-server)
* `npm test` (run karma for unit tests)
* `open http://localhost:8080`

It's possible to run 'watch' and 'start' task by one command:

* `gulp serve` (autobuild + livereload + http-server)



* in dev mode, UI runs on port `8080` and connects to livereload on port `35729`
* coopr server is expected to be running on port `55054`.

### for testing:

* `cd coopr-ui`
* `npm run build` ( == `npm install && bower install && gulp build`)
* `npm run test-single-run` (unit tests)
* `cd ../coopr-e2e && npm install && npm run protractor` (end-to-end tests, needs `npm start` running in parallel)

### for production:

generate a minified build of static assets, in the `coopr-ui/dist/` folder

* `cd coopr-ui`
* `npm run build` ( == `npm install && bower install && gulp build`)
* `gulp distribute` (makes minified and revision-tagged static assets)

now to run the server, possibly on a different host:

* `cd coopr-ui`
* `npm install --production` (will skip devDependencies)
* `export COOPR_UI_PORT=8100`
* `export COOPR_SERVER_URI=http://hostname:port/`
* `npm start` or `node server.js`

