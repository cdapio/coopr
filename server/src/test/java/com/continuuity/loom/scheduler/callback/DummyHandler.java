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

import com.continuuity.http.AbstractHttpHandler;
import com.continuuity.http.HttpResponder;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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
 *
 */
public class DummyHandler extends AbstractHttpHandler {
  private static final Logger LOG  = LoggerFactory.getLogger(DummyHandler.class);
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private int startCount = 0;
  private int successCount = 0;
  private int failureCount = 0;

  public void clear() {
    startCount = 0;
    successCount = 0;
    failureCount = 0;
  }

  public int getStartCount() {
    return startCount;
  }

  public int getSuccessCount() {
    return successCount;
  }

  public int getFailureCount() {
    return failureCount;
  }

  @POST
  @Path("/start/endpoint")
  public void start(HttpRequest request, HttpResponder responder) throws Exception {
    startCount++;
    responder.sendStatus(HttpResponseStatus.OK);
  }

  @POST
  @Path("/success/endpoint")
  public void success(HttpRequest request, HttpResponder responder) throws Exception {
    successCount++;
    responder.sendStatus(HttpResponseStatus.OK);
  }

  @POST
  @Path("/failure/endpoint")
  public void failure(HttpRequest request, HttpResponder responder) throws Exception {
    failureCount++;
    responder.sendStatus(HttpResponseStatus.OK);
  }
}
