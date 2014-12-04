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

import co.cask.coopr.scheduler.task.ClusterTask;
import co.cask.coopr.spec.ProvisionerAction;
import co.cask.coopr.store.cluster.ClusterStore;
import co.cask.coopr.store.cluster.ClusterTaskFilter;
import org.apache.commons.lang3.time.DateUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Util for performing metric operations.
 */
public class MetricService {

  public enum Periodicity {
    hour,
    day,
    week,
    month,
    year
  }

  private static final long WEEK = DateUtils.MILLIS_PER_DAY * 7;
  private static final long MONTH = DateUtils.MILLIS_PER_DAY * 30;
  private static final long YEAR = MONTH * 12;

  private final ClusterStore clusterStore;

  public MetricService(ClusterStore clusterStore) {
    this.clusterStore = clusterStore;
  }

  /**
   * Calculate statistics of nodes usage for given {@link ClusterTaskFilter}.
   * The start and end times are inclusive.
   * Loads all tasks with CREATE or DELETE {@link ProvisionerAction}. Then, for each node, calculates node live time:
   * finished time of CREATE task to finished time of DELETE task or finished time of CREATE task to current time.
   * If required, then overlays {@code filter}'s start and end date.
   *
   * @param filter the filter
   * @return {@link TimeSeries} that presents node live time usage
   */
  public TimeSeries getNodesUsage(ClusterTaskFilter filter) throws IOException {
    //TODO: implement this functionality without keeping all tasks in memory.
    List<ClusterTask> tasks = clusterStore.getClusterTasks(filter);
    Long start = filter.getStart();
    Long end = filter.getEnd();
    TimeUnit timeUnit = filter.getTimeUnit();
    if (tasks.isEmpty()) {
      long startTime = start != null ? start : 0;
      return new TimeSeries(startTime, end != null ? end : TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()),
                            Arrays.asList(new Interval(startTime)));
    }
    List<ClusterTask> createTasks = getTasksByName(tasks, ProvisionerAction.CREATE);
    List<ClusterTask> deleteTasks = getTasksByName(tasks, ProvisionerAction.DELETE);
    long startDate = start != null ? TimeUnit.SECONDS.toMillis(start) : createTasks.isEmpty() ?
      0 : createTasks.get(0).getStatusTime();
    long endDate = end != null ? TimeUnit.SECONDS.toMillis(end) : deleteTasks.isEmpty() ?
      System.currentTimeMillis() : deleteTasks.get(deleteTasks.size() - 1).getStatusTime();
    Periodicity periodicity = filter.getPeriodicity();
    long period;
    if (periodicity == null) {
      period = endDate;
    } else {
      period = getTimeStamp(periodicity);
    }
    final List<Interval> intervals = getIntervalList(startDate, endDate, period);
    for (ClusterTask createTask : createTasks) {
      long deleteTaskTime = getDeleteTaskTime(deleteTasks, createTask);
      long localStart = Math.max(createTask.getStatusTime(), startDate);
      long localEnd = Math.min(deleteTaskTime, endDate);
      int currentIndex = getNearestIndex(intervals, localStart);
      Interval current = intervals.get(currentIndex);
      long currentTimeInMillis = TimeUnit.SECONDS.toMillis(current.getTime());
      while (currentTimeInMillis + period < localEnd) {
        long increaseTime = localStart < currentTimeInMillis ? period : period + currentTimeInMillis - localStart;
        current.increaseValue(timeUnit.convert(increaseTime, TimeUnit.MILLISECONDS));
        current = intervals.get(++currentIndex);
        currentTimeInMillis = TimeUnit.SECONDS.toMillis(current.getTime());
      }
      long increaseTime = localStart < currentTimeInMillis ? localEnd - currentTimeInMillis : localEnd - localStart;
      if (increaseTime > 0) {
        current.increaseValue(timeUnit.convert(increaseTime, TimeUnit.MILLISECONDS));
      }
    }
    return new TimeSeries(TimeUnit.MILLISECONDS.toSeconds(startDate),
                          TimeUnit.MILLISECONDS.toSeconds(endDate), intervals);
  }

  private long getDeleteTaskTime(List<ClusterTask> deleteTasks, ClusterTask createTask) {
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

  private List<ClusterTask> getTasksByName(List<ClusterTask> tasks, ProvisionerAction name) {
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
  private int getNearestIndex(List<Interval> intervals, long key) {
    int index = -1;
    for (Interval value : intervals) {
      if (TimeUnit.SECONDS.toMillis(value.getTime()) <= key) {
        index++;
      } else {
        break;
      }
    }
    return index;
  }

  /**
   * Creates {@link List} of {@code Interval}s.
   *
   * @param start start of date period
   * @param end end of date period
   * @param period date period length in milliseconds
   * @return {@link List} of {@code Interval}s
   */
  private List<Interval> getIntervalList(long start, long end, long period) {
    List<Interval> intervals = new ArrayList<Interval>();
    long currentStart = start;
    long nextStart = start - start%period + period;
    nextStart = nextStart < end ? nextStart : end;
    while (nextStart < end) {
      intervals.add(new Interval(TimeUnit.MILLISECONDS.toSeconds(currentStart)));
      currentStart = nextStart;
      nextStart = currentStart + period < end ? currentStart + period : end;
    }
    intervals.add(new Interval(TimeUnit.MILLISECONDS.toSeconds(currentStart)));
    return intervals;
  }

  private long getTimeStamp(Periodicity periodicity) {
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
