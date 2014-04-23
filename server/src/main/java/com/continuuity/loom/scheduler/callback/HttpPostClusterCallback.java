/*
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
package com.continuuity.loom.scheduler.callback;

import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.conf.Configuration;
import com.continuuity.loom.conf.Constants;
import com.continuuity.loom.scheduler.ClusterAction;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Set;

/**
 * Executes before and after hooks by sending an HTTP POST request to some configurable endpoints, with the post body
 * containing the cluster and job objects. By default the requests are sent before and after cluster creation and
 * cluster deletion,
 */
public class HttpPostClusterCallback extends ClusterCallback {
  private static final Logger LOG = LoggerFactory.getLogger(HttpPostClusterCallback.class);
  private static final Gson GSON = new JsonSerde().getGson();
  private final String beforeUrl;
  private final String afterUrl;
  private final Set<ClusterAction> beforeTriggerActions;
  private final Set<ClusterAction> afterTriggerActions;
  private final DefaultHttpClient httpClient;

  @Inject
  public HttpPostClusterCallback(Configuration conf) {
    super(conf);
    this.beforeUrl = conf.get(Constants.HTTP_CALLBACK_BEFORE_URL);
    this.afterUrl = conf.get(Constants.HTTP_CALLBACK_AFTER_URL);
    this.beforeTriggerActions = parseActionsString(conf.get(Constants.HTTP_CALLBACK_BEFORE_TRIGGER_ACTIONS,
                                                            Constants.DEFAULT_HTTP_CALLBACK_BEFORE_TRIGGER_ACTIONS));
    this.afterTriggerActions = parseActionsString(conf.get(Constants.HTTP_CALLBACK_AFTER_TRIGGER_ACTIONS,
                                                           Constants.DEFAULT_HTTP_CALLBACK_AFTER_TRIGGER_ACTIONS));
    if (beforeUrl != null) {
      LOG.debug("before hook will be triggered on actions {}", Joiner.on(',').join(beforeTriggerActions));
    }
    if (afterUrl != null) {
      LOG.debug("after hook will be triggered on actions {}", Joiner.on(',').join(afterTriggerActions));
    }

    HttpParams httpParams = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(
      httpParams, conf.getInt(Constants.HTTP_CALLBACK_CONNECTION_TIMEOUT,
                              Constants.DEFAULT_HTTP_CALLBACK_CONNECTION_TIMEOUT));
    HttpConnectionParams.setSoTimeout(
      httpParams, conf.getInt(Constants.HTTP_CALLBACK_SOCKET_TIMEOUT,
                              Constants.DEFAULT_HTTP_CALLBACK_SOCKET_TIMEOUT));
    this.httpClient = new DefaultHttpClient(httpParams);
  }

  private Set<ClusterAction> parseActionsString(String actionsStr) {
    if (actionsStr == null) {
      return ImmutableSet.of();
    }

    Iterator<String> actionIter = Splitter.on(',').split(actionsStr).iterator();
    Set<ClusterAction> actions = Sets.newHashSet();
    while (actionIter.hasNext()) {
      String actionStr = actionIter.next();
      try {
        ClusterAction action = ClusterAction.valueOf(actionStr.toUpperCase());
        actions.add(action);
      } catch (IllegalArgumentException e) {
        LOG.warn("Unknown cluster action " + actionStr + ". Hooks will not be executed for that action");
      }
    }
    return actions;
  }

  public void executeBeforeCallback(CallbackData data) {
    ClusterAction jobAction = data.getJob().getClusterAction();
    if (beforeTriggerActions.contains(jobAction)) {
      LOG.debug("sending request to {} before performing {} on cluster {}",
                beforeUrl, jobAction, data.getCluster().getId());
      sendPost(beforeUrl, data);
    }
  }

  public void executeAfterCallback(CallbackData data) {
    ClusterAction jobAction = data.getJob().getClusterAction();
    if (afterTriggerActions.contains(data.getJob().getClusterAction())) {
      LOG.debug("sending request to {} after performing {} on cluster {}",
                afterUrl, jobAction, data.getCluster().getId());
      sendPost(afterUrl, data);
    }
  }

  private void sendPost(String url, CallbackData data) {
    if (url != null) {
      HttpPost post = new HttpPost(url);

      try {
        post.setEntity(new StringEntity(GSON.toJson(data)));
        HttpResponse response = httpClient.execute(post);
        // ignore what we get back, but make sure resources are released
        HttpEntity entity = response.getEntity();
        if (entity != null) {
          entity.getContent().close();
        }
      } catch (UnsupportedEncodingException e) {
        LOG.warn("Exception setting http post body", e);
      } catch (ClientProtocolException e) {
        LOG.warn("Exception executing http post callback to " + url, e);
      } catch (IOException e) {
        LOG.warn("Exception executing http post callback to " + url, e);
      }
    }
  }
}
