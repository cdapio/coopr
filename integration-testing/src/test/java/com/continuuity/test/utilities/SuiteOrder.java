/**
 * Copyright 2012-2014, Continuuity, Inc.
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
package com.continuuity.test.utilities;
/**
 * SuiteOrder is class managing order of execution of tests.
 *
 */

import com.continuuity.test.Constants;
import com.continuuity.test.pagetest.ClustersInstanceTest;
import com.continuuity.test.pagetest.ClustersTest;
import com.continuuity.test.pagetest.ClustertemplatesInstanceTest;
import com.continuuity.test.pagetest.ClustertemplatesTest;
import com.continuuity.test.pagetest.CreateClusterTest;
import com.continuuity.test.pagetest.CreateClustertemplateTest;
import com.continuuity.test.pagetest.CreateHardwaretypeTest;
import com.continuuity.test.pagetest.CreateImagetypeTest;
import com.continuuity.test.pagetest.CreateProviderTest;
import com.continuuity.test.pagetest.CreateServiceTest;
import com.continuuity.test.pagetest.HardwaretypesInstanceTest;
import com.continuuity.test.pagetest.HardwaretypesTest;
import com.continuuity.test.pagetest.ImagetypesInstanceTest;
import com.continuuity.test.pagetest.ImagetypesTest;
import com.continuuity.test.pagetest.ProvidersInstanceTest;
import com.continuuity.test.pagetest.ProvidersTest;
import com.continuuity.test.pagetest.ServicesInstanceTest;
import com.continuuity.test.pagetest.ServicesTest;
import com.continuuity.test.resetTests.CloseDriverTest;
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
    ClustersInstanceTest.class, ClustersTest.class, ClustertemplatesInstanceTest.class, ClustertemplatesTest.class,
    CreateClustertemplateTest.class, CreateClusterTest.class, CreateHardwaretypeTest.class, CreateImagetypeTest.class,
    CreateProviderTest.class, CreateServiceTest.class, HardwaretypesInstanceTest.class, HardwaretypesTest.class,
    ImagetypesInstanceTest.class, ImagetypesTest.class, ProvidersInstanceTest.class, ProvidersTest.class,
    ServicesInstanceTest.class, ServicesTest.class, CloseDriverTest.class
})
public class SuiteOrder {

  static Process process;

  @BeforeClass
  public static void setUpClass() throws Exception {
    ProcessBuilder builder = new ProcessBuilder();
    builder.directory(new File("../ui"));
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
