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

import co.cask.coopr.scheduler.task.ClusterTask;
import co.cask.coopr.spec.ProvisionerAction;
import org.apache.commons.lang3.time.DateUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Util for performing metric operations.
 */
public class MetricUtil {

  private enum Periodicity {
    hour,
    day,
    week,
    month,
    year
  }

  private static final long WEEK = DateUtils.MILLIS_PER_DAY * 7;
  private static final long MONTH = DateUtils.MILLIS_PER_DAY * 30;
  private static final long YEAR = MONTH * 12;
  private static final Comparator<ClusterTask> COMPARATOR = new Comparator<ClusterTask>() {
    @Override
    public int compare(ClusterTask o1, ClusterTask o2) {
      return o1.getStatusTime() > o2.getStatusTime() ? 1 : -1;
    }
  };

  /**
   * Calculate statistics of nodes usage for given filter.
   * The start and end times are inclusive.
   *
   * @param tasks the tasks
   * @param filters the filters
   * @return {@link Object} that presents node live time usage
   */
  public static MetricResponse getNodesUsage(List<ClusterTask> tasks, Map<String, String> filters) {
    if (tasks.isEmpty()) {
      return new MetricResponse(Collections.<Interval>emptyList());
    }
    Long start = null;
    Long end = null;
    try {
      start = Long.parseLong(filters.get("start"));
    } catch (NumberFormatException ignored) {
    }
    try {
      end = Long.parseLong(filters.get("end"));
    } catch (NumberFormatException ignored) {
    }
    List<ClusterTask> createTasks = getTasksByName(tasks, ProvisionerAction.CREATE);
    List<ClusterTask> deleteTasks = getTasksByName(tasks, ProvisionerAction.DELETE);
    long startDate = start != null ? start : Collections.min(createTasks, COMPARATOR).getStatusTime();
    long endDate = end != null ? end : Collections.max(deleteTasks, COMPARATOR).getStatusTime();
    String periodicity = filters.get("groupby");
    long period;
    if (periodicity == null) {
      period = endDate;
    } else {
      period = getTimeStamp(Periodicity.valueOf(periodicity));
    }
    final List<Interval> intervals = getIntervalList(startDate, endDate, period);
    for (ClusterTask createTask : createTasks) {
      long deleteTaskTime = getDeleteTaskTime(deleteTasks, createTask);
      long localStart = createTask.getStatusTime() > startDate ? createTask.getStatusTime() : startDate;
      long localEnd = deleteTaskTime < endDate ? deleteTaskTime : endDate;
      int currentIndex = getNearestIndex(intervals, localStart);
      Interval current = intervals.get(currentIndex);
      while (current.getStart() + period < localEnd) {
        long increaseTime = localStart < current.getStart() ? period : period + current.getStart() - localStart;
        current.increaseSeconds(increaseTime);
        current = intervals.get(++currentIndex);
      }
      long increaseTime = localStart < current.getStart() ? localEnd - current.getStart() : localEnd - localStart;
      if (increaseTime > 0) {
        current.increaseSeconds(increaseTime);
      }
    }
    return new MetricResponse(intervals);
  }

  private static long getDeleteTaskTime(List<ClusterTask> deleteTasks, ClusterTask createTask) {
    ClusterTask result = null;
    for (ClusterTask deleteTask : deleteTasks) {
      if (createTask.getClusterId().equals(deleteTask.getClusterId()) &&
        createTask.getNodeId().equals(deleteTask.getNodeId())) {
        if (result == null) {
          result = deleteTask;
        } else if (deleteTask.getStatusTime() < result.getStatusTime()) {
          result = deleteTask;
        }
      }
    }
    if (result == null) {
      return System.currentTimeMillis();
    }
    return result.getStatusTime();
  }

  private static List<ClusterTask> getTasksByName(List<ClusterTask> tasks, ProvisionerAction name) {
    List<ClusterTask> result = new ArrayList<ClusterTask>();
    for (ClusterTask task : tasks) {
      if (task.getTaskName().equals(name)) {
        result.add(task);
      }
    }
    return result;
  }

  /**
   * Retrieves nearest smaller {@link Interval} from {@code intervals} for {@code key}.
   *
   * @param intervals the list of {@link Interval}s
   * @param key the key
   * @return nearest smaller {@link Interval}
   */
  private static int getNearestIndex(List<Interval> intervals, long key) {
    Interval nearest = new Interval(-1, -1);
    for (Interval value : intervals) {
      if (value.getStart() <= key && nearest.getStart() < value.getStart()) {
        nearest = value;
      }
    }
    return intervals.indexOf(nearest);
  }

  /**
   * Creates {@link List} of {@code Interval}s.
   *
   * @param start start of date period
   * @param end end of date period
   * @param period date period length in milliseconds
   * @return {@link List} of {@code Interval}s
   */
  private static List<Interval> getIntervalList(long start, long end, long period) {
    List<Interval> intervals = new ArrayList<Interval>();
    long currentStart = start;
    long nextStart = start - start%period + period;
    nextStart = nextStart < end ? nextStart : end;
    while (nextStart < end) {
      intervals.add(new Interval(currentStart, nextStart - 1));
      currentStart = nextStart;
      nextStart = currentStart + period < end ? currentStart + period : end;
    }
    intervals.add(new Interval(currentStart, nextStart));
    System.out.println(intervals.size());
    return intervals;
  }

  private static long getTimeStamp(Periodicity periodicity) {
    switch (periodicity) {
      case hour:
        return DateUtils.MILLIS_PER_HOUR;
      case day:
        return DateUtils.MILLIS_PER_DAY;
      case week:
        return WEEK;
      case month:
        return MONTH;
      default:
        return YEAR;
    }
  }
}
