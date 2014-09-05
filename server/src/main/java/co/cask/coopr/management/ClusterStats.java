/*
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
package co.cask.coopr.management;

import co.cask.coopr.scheduler.ClusterAction;

/**
 * Counters for different cluster actions used for JMX.
 */
@SuppressWarnings("UnusedDeclaration")
public class ClusterStats extends StatCounter<ClusterAction> {
  public long getSolve() {
    return getValue(ClusterAction.SOLVE_LAYOUT);
  }

  public long getCreate() {
    return getValue(ClusterAction.CLUSTER_CREATE);
  }

  public long getDelete() {
    return getValue(ClusterAction.CLUSTER_DELETE);
  }
}
