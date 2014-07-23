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

import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.provisioner.PluginResourceMeta;
import com.continuuity.loom.provisioner.PluginResourceStatus;
import com.continuuity.loom.provisioner.PluginResourceType;
import com.continuuity.loom.store.provisioner.PluginType;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class LoomPluginHandlerTest extends LoomServiceTestBase {

  @Test
  public void testNonAdminGetsForbidden() throws Exception {
    PluginResourceType type1 = new PluginResourceType(PluginType.PROVIDER, "openstack", "keys");
    PluginResourceType type2 = new PluginResourceType(PluginType.AUTOMATOR, "shell", "script");
    PluginResourceMeta meta = PluginResourceMeta.createNew("name", "1");
    assertResponseStatus(doPut(getVersionedPath(type1, meta), "contents", USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doPut(getVersionedPath(type2, meta), "contents", USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doDelete(getVersionedPath(type1, meta), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doDelete(getVersionedPath(type2, meta), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet(getVersionedPath(type1, meta), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet(getVersionedPath(type2, meta), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet(getUnVersionedPath(type1, meta), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet(getUnVersionedPath(type2, meta), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet(getPath(type1), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet(getPath(type2), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
  }

  @Test
  public void testPutAndGetAutomatorTypeModule() throws Exception {
    testPutAndGet(PluginType.AUTOMATOR, "shell", "scripts");
  }

  @Test
  public void testPutAndGetProviderTypeModule() throws Exception {
    testPutAndGet(PluginType.PROVIDER, "openstack", "cookbooks");
  }

  @Test
  public void testActivateDeactivateAutomatorTypeModule() throws Exception {
    testVersions(PluginType.AUTOMATOR, "shell", "scripts");
  }

  @Test
  public void testActivateDeactivateProviderTypeModule() throws Exception {
    testVersions(PluginType.PROVIDER, "openstack", "keys");
  }

  @Test
  public void testGetAndDeleteAutomatorTypeResources() throws Exception {
    testGetAndDelete(new PluginResourceType(PluginType.AUTOMATOR, "shell", "scripts"));
  }

  @Test
  public void testGetAndDeleteProviderTypeResources() throws Exception {
    testGetAndDelete(new PluginResourceType(PluginType.PROVIDER, "openstack", "keys"));
  }

  private void assertSendContents(String contents, PluginType type, String pluginName, String resourceType,
                                  String resourceName, String version) throws Exception {
    assertSendContents(contents, new PluginResourceType(type, pluginName, resourceType),
                       PluginResourceMeta.createNew(resourceName, version));
  }

  private void assertSendContents(String contents, PluginResourceType type, PluginResourceMeta meta) throws Exception {
    String path = getVersionedPath(type, meta);
    assertResponseStatus(doPut(path, contents, ADMIN_HEADERS), HttpResponseStatus.OK);
  }

  private void testPutAndGet(PluginType type, String pluginName, String resourceType) throws Exception {
    String contents = RandomStringUtils.randomAlphanumeric(8 * Constants.PLUGIN_RESOURCE_CHUNK_SIZE);
    PluginResourceType pluginResourceType = new PluginResourceType(type, pluginName, resourceType);
    PluginResourceMeta meta = PluginResourceMeta.fromExisting(null, "hello", "1", PluginResourceStatus.INACTIVE);
    assertSendContents(contents, type, pluginName, resourceType, "hello", "1");
    // get metadata
    HttpResponse response = doGet(getUnVersionedPath(pluginResourceType, meta), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(ImmutableSet.of(meta), bodyToMetaSet(response));
    // get actual contents
    response = doGet(getVersionedPath(pluginResourceType, meta), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(contents, bodyToString(response));
  }

  private void testVersions(PluginType type, String pluginName, String resourceType) throws Exception{
    String contents = "some contents";
    PluginResourceType pluginResourceType = new PluginResourceType(type, pluginName, resourceType);
    PluginResourceMeta meta1 = PluginResourceMeta.fromExisting(null, "name", "1", PluginResourceStatus.INACTIVE);
    PluginResourceMeta meta2 = PluginResourceMeta.fromExisting(null, "name", "2", PluginResourceStatus.INACTIVE);
    assertSendContents(contents, pluginResourceType, meta1);
    assertSendContents(contents, pluginResourceType, meta2);

    // stage version2
    meta2 = PluginResourceMeta.fromExisting(
      meta2.getResourceId(), meta2.getName(), meta2.getVersion(), PluginResourceStatus.STAGED);
    assertResponseStatus(doPost(getVersionedPath(pluginResourceType, meta2) + "/stage", "", ADMIN_HEADERS),
                         HttpResponseStatus.OK);
    // should still see both versions when getting all versions of the resource name
    HttpResponse response = doGet(getUnVersionedPath(pluginResourceType, meta2), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta1, meta2), bodyToMetaSet(response));
    // check get staged versions of the resources
    response = doGet(getPath(pluginResourceType) + "?status=staged", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(
      ImmutableMap.<String, Set<PluginResourceMeta>>of("name", ImmutableSet.<PluginResourceMeta>of(meta2)),
      bodyToMetaMap(response)
    );
    // check get staged version of the specific resource
    response = doGet(getUnVersionedPath(pluginResourceType, meta2) + "?status=staged", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta2), bodyToMetaSet(response));

    // stage version1
    meta1 = PluginResourceMeta.fromExisting(
      meta1.getResourceId(), meta1.getName(), meta1.getVersion(), PluginResourceStatus.STAGED);
    meta2 = PluginResourceMeta.fromExisting(
      meta2.getResourceId(), meta2.getName(), meta2.getVersion(), PluginResourceStatus.INACTIVE);
    assertResponseStatus(doPost(getVersionedPath(pluginResourceType, meta1) + "/stage", "", ADMIN_HEADERS),
                         HttpResponseStatus.OK);
    // should still see both versions when getting all versions of the resource name
    response = doGet(getUnVersionedPath(pluginResourceType, meta1), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta1, meta2), bodyToMetaSet(response));
    // check get staged versions of the resources
    response = doGet(getPath(pluginResourceType) + "?status=staged", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(
      ImmutableMap.<String, Set<PluginResourceMeta>>of("name", ImmutableSet.<PluginResourceMeta>of(meta1)),
      bodyToMetaMap(response)
    );
    // check get staged versions of the specific resource
    response = doGet(getUnVersionedPath(pluginResourceType, meta1) + "?status=staged", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta1), bodyToMetaSet(response));

    // unstage
    meta1 = PluginResourceMeta.fromExisting(
      meta1.getResourceId(), meta1.getName(), meta1.getVersion(), PluginResourceStatus.INACTIVE);
    assertResponseStatus(doPost(getVersionedPath(pluginResourceType, meta1) + "/unstage", "", ADMIN_HEADERS),
                         HttpResponseStatus.OK);
    // should still see both versions when getting all versions of the resource name
    response = doGet(getUnVersionedPath(pluginResourceType, meta1), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(ImmutableSet.<PluginResourceMeta>of(meta1, meta2), bodyToMetaSet(response));
    // staged filter should return an empty map
    response = doGet(getPath(pluginResourceType) + "?status=staged", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertTrue(bodyToMetaMap(response).isEmpty());
    // no staged versions
    response = doGet(getUnVersionedPath(pluginResourceType, meta1) + "?status=staged", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertTrue(bodyToMetaSet(response).isEmpty());
  }

  private void testGetAndDelete(PluginResourceType type) throws Exception {
    String contents = "some contents";
    PluginResourceMeta meta1 = PluginResourceMeta.fromExisting(null, "name1", "1", PluginResourceStatus.INACTIVE);
    PluginResourceMeta meta2 = PluginResourceMeta.fromExisting(null, "name1", "2", PluginResourceStatus.INACTIVE);
    PluginResourceMeta meta3 = PluginResourceMeta.fromExisting(null, "name2", "1", PluginResourceStatus.INACTIVE);
    PluginResourceMeta meta4 = PluginResourceMeta.fromExisting(null, "name3", "2", PluginResourceStatus.INACTIVE);
    assertSendContents(contents, type, meta1);
    assertSendContents(contents, type, meta2);
    assertSendContents(contents, type, meta3);
    assertSendContents(contents, type, meta4);

    HttpResponse response = doGet(getPath(type), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(
      ImmutableMap.<String, Set<PluginResourceMeta>>of(
        "name1", ImmutableSet.<PluginResourceMeta>of(meta1, meta2),
        "name2", ImmutableSet.<PluginResourceMeta>of(meta3),
        "name3", ImmutableSet.<PluginResourceMeta>of(meta4)),
      bodyToMetaMap(response)
    );

    // delete one
    assertResponseStatus(doDelete(getVersionedPath(type, meta3), ADMIN_HEADERS), HttpResponseStatus.OK);
    response = doGet(getPath(type), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(
      ImmutableMap.<String, Set<PluginResourceMeta>>of(
        "name1", ImmutableSet.<PluginResourceMeta>of(meta1, meta2),
        "name3", ImmutableSet.<PluginResourceMeta>of(meta4)),
      bodyToMetaMap(response)
    );
  }

  private Map<String, Set<PluginResourceMeta>> bodyToMetaMap(HttpResponse response) throws IOException {
    Reader reader = new InputStreamReader(response.getEntity().getContent());
    return gson.fromJson(reader, new TypeToken<Map<String, Set<PluginResourceMeta>>>() {}.getType());
  }

  private Set<PluginResourceMeta> bodyToMetaSet(HttpResponse response) throws IOException {
    Reader reader = new InputStreamReader(response.getEntity().getContent());
    return gson.fromJson(reader, new TypeToken<Set<PluginResourceMeta>>() {}.getType());
  }

  private String bodyToString(HttpResponse response) throws IOException {
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    try {
      return CharStreams.toString(reader);
    } finally {
      reader.close();
    }
  }

  private String getPath(PluginResourceType type) {
    return Joiner.on("/").join("/v1/loom", type.getPluginType().name().toLowerCase() + "types",
                               type.getPluginName(), type.getResourceType());
  }

  private String getUnVersionedPath(PluginResourceType type, PluginResourceMeta meta) {
    return Joiner.on("/").join(getPath(type), meta.getName());
  }

  private String getVersionedPath(PluginResourceType type, PluginResourceMeta meta) {
    return Joiner.on("/").join(getPath(type), meta.getName(), "versions", meta.getVersion());
  }

  // do it manually since the ids will not be the same
  private void assertResourceMapsEqual(Map<String, Set<PluginResourceMeta>> map1,
                                       Map<String, Set<PluginResourceMeta>> map2) {
    Assert.assertEquals(map1.size(), map2.size());
    Assert.assertEquals(map1.keySet(), map2.keySet());

  }
}
