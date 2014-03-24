# Selenium web testing for Loom

Runs with the test environment of loom UI.

### Dependencies:
Nodejs: http://nodejs.org/

Chrome: https://www.google.com/intl/en/chrome/browser/

Phantomjs: http://phantomjs.org/ (for headless testing)

###Steps
First build server:

$ cd server

$ mvn clean package

Add loom server as a dependency:

mvn org.apache.maven.plugins:maven-install-plugin:2.5.1:install-file \

-Dfile=../server/target/loom-<version>.jar \

-DgroupId=com.continuuity \

-DartifactId=loom \

-Dpackaging=jar \

-Dversion=<version>

Run tests:

mvn -Dtest=SuiteOrder test

