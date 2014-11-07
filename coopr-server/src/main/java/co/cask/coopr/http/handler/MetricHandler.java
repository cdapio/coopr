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

package co.cask.coopr.http.handler;

import co.cask.coopr.account.Account;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.scheduler.task.ClusterTask;
import co.cask.coopr.store.cluster.ClusterStore;
import co.cask.coopr.store.cluster.ClusterStoreService;
import co.cask.coopr.store.cluster.ClusterTaskQuery;
import co.cask.coopr.store.tenant.TenantStore;
import co.cask.http.HttpResponder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.apache.commons.lang3.time.DateUtils;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handler for performing metric operations.
 */
@Path(Constants.API_BASE + "/metrics")
public class MetricHandler extends AbstractAuthHandler {

  private enum Periodicity {
    hour,
    day,
    week,
    month,
    year
  }

  private static final Logger LOG = LoggerFactory.getLogger(MetricHandler.class);
  private static final long WEEK = DateUtils.MILLIS_PER_DAY * 7;
  private static final long MONTH = DateUtils.MILLIS_PER_DAY * 30;
  private static final long YEAR = MONTH * 12;
  private static final Comparator<ClusterTask> COMPARATOR = new Comparator<ClusterTask>() {
    @Override
    public int compare(ClusterTask o1, ClusterTask o2) {
      return o1.getSubmitTime() > o2.getSubmitTime() ? 1 : -1;
    }
  };

  private final ClusterStore clusterStore;

  /**
   * Initializes a new instance of a MetricHandler.
   */
  @Inject
  private MetricHandler(TenantStore tenantStore, ClusterStoreService clusterStoreService) {
    super(tenantStore);
    this.clusterStore = clusterStoreService.getSystemView();
  }

  @GET
  @Path("/nodes/usage")
  public void getNodesUsage(HttpRequest request, HttpResponder responder,
                            @QueryParam("tenant") String tenant, @QueryParam("user") String user,
                            @QueryParam("cluster") String cluster, @QueryParam("clustertemplate") String template,
                            @QueryParam("start") long start, @QueryParam("end") long end,
                            @QueryParam("groupby") Periodicity periodicity) {
    Account account = getAndAuthenticateAccount(request, responder);
    if (account == null) {
      return;
    }
    if (!account.isSuperadmin()) {
      if (tenant == null) {
        tenant = account.getTenantId();
      } else if (!tenant.equals(account.getTenantId())) {
        responder.sendString(HttpResponseStatus.METHOD_NOT_ALLOWED,
                             String.format("Getting information about tenant %s not allowed for tenant %s",
                                           tenant, account.getTenantId()));
        return;
      }
    }
    ClusterTaskQuery query = new ClusterTaskQuery(tenant, user, cluster, template, start, end);
    try {
      List<ClusterTask> tasks = clusterStore.getClusterTasks(query);
      long startDate = start != 0 ? start : Collections.min(tasks, COMPARATOR).getSubmitTime();
      long endDate = end != 0 ? end : Collections.max(tasks, COMPARATOR).getSubmitTime();
      if (periodicity == null) {
        long seconds = 0;
        for (ClusterTask task : tasks) {
          seconds += task.getStatusTime() - task.getSubmitTime();
        }
        Map<Key, Long> map = new LinkedHashMap<Key, Long>();
        map.put(new Key(startDate, endDate), seconds);
        responder.sendJson(HttpResponseStatus.OK, toJson(map));
      } else {
        long period = getTimeStamp(periodicity);


        Map<Key, Long> periods = getPeriodMap(startDate, endDate, period);
        for (ClusterTask task : tasks) {
          Key current = getNearest(periods.keySet(), task.getSubmitTime());
          while (current.getStart() + period < task.getStatusTime()) {
            periods.put(current, periods.get(current) + period);
            current = getNext(current, periods.keySet());
          }
          periods.put(current, periods.get(current) + task.getStatusTime() - current.getStart());
        }
        responder.sendJson(HttpResponseStatus.OK, toJson(periods));
      }
    } catch (IOException e) {
      responder.sendString(HttpResponseStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  /**
   * Retrieves next {@link Key} from {@code entrySet} for {@code current}.
   *
   * @param current the current {@link Key}
   * @param entrySet the entry set of {@link Key}s
   * @return next {@link Key}
   */
  private Key getNext(Key current, Set<Key> entrySet) {
    for (Key value : entrySet) {
      if (value.getStart() == current.getEnd() + 1) {
        return value;
      }
    }
    return null;
  }

  /**
   * Retrieves nearest smaller {@link Key} from {@code entrySet} for {@code key}.
   *
   * @param entrySet the entry set of {@link Key}s
   * @param key the key
   * @return nearest smaller {@link Key}
   */
  private Key getNearest(Set<Key> entrySet, long key) {
    Key nearest = new Key(-1, -1);
    for (Key value : entrySet) {
      if (value.getStart() <= key && nearest.getStart() < value.getStart()) {
        nearest = value;
      }
    }
    return nearest;
  }

  /**
   * Creates {@link Map} with specified keys, each key presents some date period.
   *
   * @param start start of date period
   * @param end end of date period
   * @param period date period length in milliseconds
   * @return {@link Map} with specified keys, each key presents some date period.
   */
  private Map<Key, Long> getPeriodMap(long start, long end, long period) {
    Map<Key, Long> periods = new LinkedHashMap<Key, Long>();
    long currentStart = start;
    long nextStart = start - start%period + period;
    nextStart = nextStart < end ? nextStart : end;
    periods.put(new Key(currentStart, nextStart - 1), (long) 0);
    while (nextStart < end) {
      currentStart = nextStart;
      nextStart = currentStart + period < end ? currentStart + period : end;
      periods.put(new Key(currentStart, nextStart - 1), (long) 0);
    }
    return periods;
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

  private JsonObject toJson(Map<Key, Long> periods) {
    JsonArray array = new JsonArray();
    for (Key key : periods.keySet()) {
      JsonObject element = new JsonObject();
      element.addProperty("start", key.getStart());
      element.addProperty("end", key.getEnd());
      element.addProperty("seconds", periods.get(key));
      array.add(element);
    }
    JsonObject object = new JsonObject();
    object.add("usage", array);
    return object;
  }

  /**
   * Class for presenting start and end date of time period.
   */
  private class Key {

    private final long start;
    private final long end;

    public Key(long start, long end) {
      this.start = start;
      this.end = end;
    }

    public long getStart() {
      return start;
    }

    public long getEnd() {
      return end;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Key key = (Key) o;

      return start == key.start;

    }

    @Override
    public int hashCode() {
      return (int) (start ^ (start >>> 32));
    }
  }
}
