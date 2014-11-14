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

package co.cask.coopr.metrics;

/**
 * Class for presenting time - start date of time period, and some data point.
 */
public class Interval {

  private final long time;
  private long value = 0;

  public Interval(long time) {
    this.time = time;
  }

  public long getTime() {
    return time;
  }

  public long getValue() {
    return value;
  }

  public void increaseValue(long value) {
    this.value += value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Interval interval = (Interval) o;

    return time == interval.time && value == interval.value;
  }

  @Override
  public int hashCode() {
    int result = (int) (time ^ (time >>> 32));
    result = 31 * result + (int) (value ^ (value >>> 32));
    return result;
  }
}
