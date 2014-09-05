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
package co.cask.coopr.common.queue;

import com.google.common.base.Objects;

/**
 * A snapshot of metrics related to a {@link TrackingQueue}.
 */
public class QueueMetrics {
  private final int queued;
  private final int inProgress;
  private final int total;

  public QueueMetrics(int queued, int inProgress) {
    this.queued = queued;
    this.inProgress = inProgress;
    this.total = queued + inProgress;
  }

  public int getQueued() {
    return queued;
  }

  public int getInProgress() {
    return inProgress;
  }

  public int getTotal() {
    return total;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    QueueMetrics that = (QueueMetrics) o;

    return total == that.total && inProgress == that.inProgress && queued == that.queued;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(total, queued, inProgress);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("total", total)
      .add("inProgress", inProgress)
      .add("queued", queued)
      .toString();
  }
}
