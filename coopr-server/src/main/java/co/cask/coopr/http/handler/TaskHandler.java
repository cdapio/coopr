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

import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.http.HttpHelper;
import co.cask.coopr.http.request.FinishTaskRequest;
import co.cask.coopr.http.request.TakeTaskRequest;
import co.cask.coopr.scheduler.task.MissingEntityException;
import co.cask.coopr.scheduler.task.TaskQueueService;
import co.cask.http.AbstractHttpHandler;
import co.cask.http.HttpResponder;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

/**
 * Handles requests to take and finish tasks from provisioners.
 */
@Path(Constants.API_BASE + "/tasks")
public final class TaskHandler extends AbstractHttpHandler {
  private static final Logger LOG = LoggerFactory.getLogger(TaskHandler.class);

  private final Gson gson;
  private final TaskQueueService taskQueueService;

  @Inject
  private TaskHandler(TaskQueueService taskQueueService, Gson gson) {
    this.taskQueueService = taskQueueService;
    this.gson = gson;
  }

  /**
   * Take a task from the queue to execute. Post body must contain a workerId key, which must also be passed back
   * when finishing a task. Tasks are returned as a json object with taskId, jobId, clusterId, taskName and nodeId
   * as key value pairs, and with a config key whose value is a json object with all the configuration settings.
   *
   * @param request The request to take a task.
   * @param responder Responder to send the response.
   */
  @POST
  @Path("/take")
  public void handleTakeTask(HttpRequest request, HttpResponder responder) {
    TakeTaskRequest takeRequest = HttpHelper.decodeRequestBody(request, responder, TakeTaskRequest.class, gson);

    try {
      String taskJson = taskQueueService.takeNextClusterTask(takeRequest);
      if (taskJson == null) {
        responder.sendStatus(HttpResponseStatus.NO_CONTENT);
        return;
      }
      responder.sendString(HttpResponseStatus.OK, taskJson);
    } catch (IOException e) {
      LOG.error("Exception while taking task.", e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error taking task.");
    } catch (MissingEntityException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "Provisioner " + takeRequest.getProvisionerId()
        + " is not registered.");
    }
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
   */
  @POST
  @Path("/finish")
  public void handleFinishTask(HttpRequest request, HttpResponder responder) {
    FinishTaskRequest finishRequest = HttpHelper.decodeRequestBody(request, responder, FinishTaskRequest.class, gson);
    if (finishRequest == null) {
      return;
    }

    LOG.trace("Got task finish {}", finishRequest);

    try {
      taskQueueService.finishClusterTask(finishRequest);
      responder.sendStatus(HttpResponseStatus.OK);
    } catch (IllegalStateException e) {
      responder.sendError(HttpResponseStatus.EXPECTATION_FAILED, e.getMessage());
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, e.getMessage());
    } catch (IOException e) {
      LOG.error("Exception finishing task {}.", finishRequest.getTaskId(), e);
      responder.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                          "Error finishing task " + finishRequest.getTaskId());
    } catch (MissingEntityException e) {
      responder.sendError(HttpResponseStatus.FORBIDDEN, "Provisioner " + finishRequest.getProvisionerId()
        + " is not registered.");
    }
  }
}
