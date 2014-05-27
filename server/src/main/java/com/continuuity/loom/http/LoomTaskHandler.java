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
package com.continuuity.loom.http;

import com.continuuity.http.AbstractHttpHandler;
import com.continuuity.http.HttpResponder;
import com.continuuity.loom.codec.json.JsonSerde;
import com.continuuity.loom.scheduler.task.TaskQueueService;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Handles requests to take and finish tasks from provisioners.
 */
@Path("/v1/loom/tasks")
public final class LoomTaskHandler extends AbstractHttpHandler {
  private static final Logger LOG = LoggerFactory.getLogger(LoomTaskHandler.class);
  private static final Gson GSON = new JsonSerde().getGson();

  private TaskQueueService taskQueueService;

  @Inject
  private LoomTaskHandler(TaskQueueService taskQueueService) {
    this.taskQueueService = taskQueueService;
  }

  /**
   * Take a task from the queue to execute. Post body must contain a workerId key, which must also be passed back
   * when finishing a task. Tasks are returned as a json object with taskId, jobId, clusterId, taskName and nodeId
   * as key value pairs, and with a config key whose value is a json object with all the configuration settings.
   *
   * @param request The request to take a task.
   * @param responder Responder to send the response.
   * @throws Exception
   */
  @POST
  @Path("/take")
  public void handleTakeTask(HttpRequest request, HttpResponder responder) throws Exception {
    JsonObject body;

    try {
      body = getRequestBody(request);
    } catch (Exception e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid request.");
      return;
    }

    if (!body.has("workerId") || body.get("workerId") == null) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "workerId must be specified.");
      return;
    }

    String workerId = body.get("workerId").getAsString();
    String taskJson = taskQueueService.takeNextClusterTask(workerId);

    if (taskJson == null) {
      responder.sendError(HttpResponseStatus.NO_CONTENT, "no tasks to take for worker " + workerId);
      return;
    }

    responder.sendString(HttpResponseStatus.OK, taskJson);
  }

  /**
   * Finish a previously taken task by reporting a status code for execution of the task. Post body must contain
   * "workerId", "taskId", and "status" keys or a 400 is returned. A non-zero status indicates task failure.
   * If there was an error, logs can be stored by including "stdout" and "stderr" key values. Additional key values can
   * be returned as a json object assigned to the "result" key. These key values will be included in the config section
   * of future tasks that must take place on the node. If the worker id does not match the worker that took the task,
   * a 417 error is returned. This includes the case where the task is timed out by the server and the original
   * provisioner comes back and tries to finish the task.
   *
   * @param request The request to take a task.
   * @param responder Responder to send the response.
   * @throws Exception
   */
  @POST
  @Path("/finish")
  public void handleFinishTask(HttpRequest request, HttpResponder responder) throws Exception {
    JsonObject body;
    try {
      body = getRequestBody(request);
    } catch (Exception e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid request.");
      return;
    }

    if (!validateFinishBody(body, responder)) {
      // validateFinishBody sends error response.
      return;
    }

    LOG.trace("Got response {}", body);

    String workerId = body.get("workerId").getAsString();
    String taskId = body.get("taskId").getAsString();
    int status = body.get("status").getAsInt();
    JsonObject result = (body.has("result") && body.get("result") != null) ?
      body.get("result").getAsJsonObject() : new JsonObject();
    String stdout = (body.has("stdout") && body.get("stdout") != null) ? body.get("stdout").getAsString() : null;
    String stderr = (body.has("stderr") && body.get("stderr") != null) ? body.get("stderr").getAsString() : null;

    try {
      taskQueueService.finishClusterTask(taskId, workerId, status, result, stdout, stderr);
    } catch (IllegalStateException e) {
      responder.sendError(HttpResponseStatus.EXPECTATION_FAILED, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    }

    responder.sendStatus(HttpResponseStatus.OK);
  }

  private boolean validateFinishBody(JsonObject body, HttpResponder responder) {
    if (!body.has("workerId") || body.get("workerId") == null) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "workerId must be specified.");
      return false;
    }

    if (!body.has("taskId") || body.get("taskId") == null) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "taskId must be specified.");
      return false;
    }

    if (!body.has("status") || body.get("status") == null) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "status must be specified.");
      return false;
    }

    return true;
  }

  private JsonObject getRequestBody(HttpRequest request) {
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    return GSON.fromJson(reader, JsonObject.class);
  }
}
