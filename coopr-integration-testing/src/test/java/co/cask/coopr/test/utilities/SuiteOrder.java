/**
 * Copyright © 2012-2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.cask.coopr.test.utilities;
/**
 * SuiteOrder is class managing order of execution of tests.
 *
 */

import co.cask.coopr.test.Constants;
import co.cask.coopr.test.pagetest.ClustersInstanceTest;
import co.cask.coopr.test.pagetest.ClustersTest;
import co.cask.coopr.test.pagetest.ClustertemplatesInstanceTest;
import co.cask.coopr.test.pagetest.ClustertemplatesTest;
import co.cask.coopr.test.pagetest.CreateClusterTest;
import co.cask.coopr.test.pagetest.CreateClustertemplateTest;
import co.cask.coopr.test.pagetest.CreateHardwaretypeTest;
import co.cask.coopr.test.pagetest.CreateImagetypeTest;
import co.cask.coopr.test.pagetest.HardwaretypesInstanceTest;
import co.cask.coopr.test.pagetest.HardwaretypesTest;
import co.cask.coopr.test.pagetest.ImagetypesInstanceTest;
import co.cask.coopr.test.pagetest.ImagetypesTest;
import co.cask.coopr.test.pagetest.ProvidersInstanceTest;
import co.cask.coopr.test.pagetest.ProvidersTest;
import co.cask.coopr.test.pagetest.ServicesInstanceTest;
import co.cask.coopr.test.pagetest.ServicesTest;
import co.cask.coopr.test.resetTests.CloseDriverTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
 
@RunWith(Suite.class)
@Suite.SuiteClasses({
    ClustersInstanceTest.class,
    ClustersTest.class,
    ClustertemplatesInstanceTest.class,
    ClustertemplatesTest.class,
    CreateClustertemplateTest.class,
    CreateClusterTest.class,
    CreateHardwaretypeTest.class,
    CreateImagetypeTest.class,
//  CreateProviderTest.class,
//  CreateServiceTest.class,
    HardwaretypesInstanceTest.class,
    HardwaretypesTest.class,
    ImagetypesInstanceTest.class,
    ImagetypesTest.class,
    ProvidersInstanceTest.class,
    ProvidersTest.class,
    ServicesInstanceTest.class,
    ServicesTest.class,
    CloseDriverTest.class
})
public class SuiteOrder {

  static Process process;

  @BeforeClass
  public static void setUpClass() throws Exception {
    ProcessBuilder builder = new ProcessBuilder();
    builder.directory(new File("../coopr-ui"));
    builder.command("/bin/sh","-c","node server.js --env=test --port=" + Constants.PORT);

    builder.redirectErrorStream(true);

    process = builder.start();
    System.out.println("Starting nodejs on port " + Constants.PORT);
    final BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    Thread loggingThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          String line;
          StringBuffer buffer = new StringBuffer(2048);
          while ((line = bufferedreader.readLine()) != null) {
            buffer.append(line);
            System.out.println(line);
          }
        } catch (final IOException ioe) {
          ioe.printStackTrace();
        }
      }
    });
    loggingThread.setDaemon(true);
    loggingThread.start();
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    System.out.println("Stopping nodejs");
    process.destroy();
    process.waitFor();
  }

}
