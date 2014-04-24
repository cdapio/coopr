# Selenium web testing for Loom

Runs with the test environment of loom UI.

### Dependencies:
   * Nodejs: http://nodejs.org/
   * Chrome: https://www.google.com/intl/en/chrome/browser/
   * Phantomjs: http://phantomjs.org/ (for headless testing)

### Usage:
##### Full Test
From the project root directory, run:

```
$ mvn test
```

This will build the server module first, then run the SuiteOrder test suite.

##### Just Integration Tests
Alternatively, you can first install the project to your local maven repository:

```
$ mvn install
```

And then run just the integration tests after that:

```
$ cd integration-testing
$ mvn test
```

If the server code changes, you may need to install it again.

##### Location of stdout
stdout of tests is captured by the surefire plugin and placed in target/surefire-reports
