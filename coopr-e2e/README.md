Coopr E2E Tests
================

### Running tests:

From home directory `/coopr`

First set up the server (go through the README for coopr-server if you haven't done so already):

* `cd coopr-server`
* `mvn clean package assembly:single`
* `cd ..` (back to main directory)

* `java -cp coopr-server/target/*:coopr-e2e/config co.cask.coopr.runtime.ServerMain`
(in a separate tab)

* `java -cp coopr-server/target/*:coopr-e2e/config co.cask.coopr.runtime.
MockProvisionerMain -p 55054`
(in a separate tab)

wait for server to start
* `./coopr-server/templates/mock/load-mock.sh`

#### Start UI (go through the README for coopr-ngui if you haven't done so already):

* `cd coopr-ngui`
* `npm run build`
* `npm start` (in a separate tab)

#### Start tests:

* `cd coopr-e2e`
* `npm run build`
* `npm run protractor`
