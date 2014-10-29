/*
 * Copyright © 2012-2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.cask.coopr.shell.command;

import co.cask.common.cli.exception.InvalidCommandException;
import co.cask.coopr.http.request.ClusterOperationRequest;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

/**
 * {@link StartServiceOnClusterCommand} class test.
 */
public class StartServiceOnClusterCommandTest extends AbstractTest {

  private static final String INPUT1 = "start service \"service1\" on cluster \"cluster1\"";
  private static final String INPUT2 = "start service \"service2\" on cluster \"cluster2\" with provider fields '{}'";

  @Test
  public void executeTestNoOptional() throws IOException, InvalidCommandException {
    CLI.execute(INPUT1, System.out);

    Mockito.verify(CLUSTER_CLIENT).startServiceOnCluster(Mockito.eq("cluster1"), Mockito.eq("service1"));
  }

  @Test
  public void executeTestWithOptional() throws IOException, InvalidCommandException {
    CLI.execute(INPUT2, System.out);

    Mockito.verify(CLUSTER_CLIENT).startServiceOnCluster(Mockito.eq("cluster2"), Mockito.eq("service2"),
                                                         Mockito.any(ClusterOperationRequest.class));
  }
}
