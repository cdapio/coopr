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
package co.cask.coopr.http;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStreamReader;
import java.io.Reader;

/**
 *
 */
public class UserHandlerTest extends ServiceTestBase {

  @Test
  public void testWriteGetDelete() throws Exception {
    // build some arbitrary object to use as the profile
    JsonObject profile = new JsonObject();
    profile.addProperty("email", "user@company.com");
    JsonObject attributes = new JsonObject();
    attributes.addProperty("key1", "val1");
    attributes.addProperty("key2", "val2");
    JsonArray list = new JsonArray();
    list.add(new JsonPrimitive("item1"));
    list.add(new JsonPrimitive("item2"));
    list.add(new JsonPrimitive("item3"));
    attributes.add("key3", list);
    profile.add("attributes", attributes);

    // no profile yet, should 404
    assertResponseStatus(doGetExternalAPI("/profile", USER1_HEADERS), HttpResponseStatus.NOT_FOUND);

    // write profile
    assertResponseStatus(doPutExternalAPI("/profile", profile.toString(), USER1_HEADERS), HttpResponseStatus.OK);

    // get the profile
    HttpResponse response = doGetExternalAPI("/profile", USER1_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonObject result = new Gson().fromJson(reader, JsonObject.class);
    Assert.assertEquals(profile, result);
    reader.close();

    // delete the profile
    assertResponseStatus(doDeleteExternalAPI("/profile", USER1_HEADERS), HttpResponseStatus.OK);

    // no profile anymore, should 404
    assertResponseStatus(doGetExternalAPI("/profile", USER1_HEADERS), HttpResponseStatus.NOT_FOUND);
  }
}
