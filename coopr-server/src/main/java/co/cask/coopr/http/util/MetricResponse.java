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

package co.cask.coopr.http.util;

import com.google.common.base.Objects;

import java.util.List;

/**
 * Class for presenting metric response.
 */
public class MetricResponse {

  private final List<Interval> usage;

  public MetricResponse(List<Interval> usage) {
    this.usage = usage;
  }

  public List<Interval> getUsage() {
    return usage;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("usage", usage)
      .toString();
  }
}
