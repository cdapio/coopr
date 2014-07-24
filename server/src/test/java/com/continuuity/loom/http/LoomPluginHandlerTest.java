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
import com.continuuity.loom.provisioner.plugin.PluginResourceMeta;
import com.continuuity.loom.provisioner.plugin.PluginResourceStatus;
import com.continuuity.loom.provisioner.plugin.PluginResourceType;
import com.continuuity.loom.provisioner.plugin.PluginType;
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
    PluginResourceMeta meta = new PluginResourceMeta("name", 1);
    assertResponseStatus(doPost(getNamePath(type1, "name"), "contents", USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doPost(getNamePath(type2, "name"), "contents", USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doDelete(getVersionedPath(type1, meta), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doDelete(getVersionedPath(type2, meta), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet(getVersionedPath(type1, meta), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet(getVersionedPath(type2, meta), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet(getNamePath(type1, "name"), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet(getNamePath(type2, "name"), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet(getTypePath(type1), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet(getTypePath(type2), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
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
                                  String resourceName) throws Exception {
    assertSendContents(contents, new PluginResourceType(type, pluginName, resourceType), resourceName);
  }

  private void assertSendContents(String contents, PluginResourceType type, String name) throws Exception {
    String path = getNamePath(type, name);
    assertResponseStatus(doPost(path, contents, ADMIN_HEADERS), HttpResponseStatus.OK);
  }

  private void testPutAndGet(PluginType type, String pluginName, String resourceType) throws Exception {
    String contents = RandomStringUtils.randomAlphanumeric(8 * Constants.PLUGIN_RESOURCE_CHUNK_SIZE);
    PluginResourceType pluginResourceType = new PluginResourceType(type, pluginName, resourceType);
    PluginResourceMeta meta = new PluginResourceMeta("hello", 1, PluginResourceStatus.INACTIVE);
    assertSendContents(contents, type, pluginName, resourceType, "hello");
    // get metadata
    HttpResponse response = doGet(getNamePath(pluginResourceType, meta.getName()), ADMIN_HEADERS);
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
    PluginResourceMeta meta1 = new PluginResourceMeta("name", 1, PluginResourceStatus.INACTIVE);
    PluginResourceMeta meta2 = new PluginResourceMeta("name", 2, PluginResourceStatus.INACTIVE);
    assertSendContents(contents, pluginResourceType, meta1.getName());
    assertSendContents(contents, pluginResourceType, meta2.getName());

    // stage version2
    meta2 = new PluginResourceMeta(meta2.getName(), meta2.getVersion(), PluginResourceStatus.STAGED);
    assertResponseStatus(doPost(getVersionedPath(pluginResourceType, meta2) + "/stage", "", ADMIN_HEADERS),
                         HttpResponseStatus.OK);
    // should still see both versions when getting all versions of the resource name
    HttpResponse response = doGet(getNamePath(pluginResourceType, meta2.getName()), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta1, meta2), bodyToMetaSet(response));
    // check get staged versions of the resources
    response = doGet(getTypePath(pluginResourceType) + "?status=staged", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(
      ImmutableMap.<String, Set<PluginResourceMeta>>of("name", ImmutableSet.<PluginResourceMeta>of(meta2)),
      bodyToMetaMap(response)
    );
    // check get staged version of the specific resource
    response = doGet(getNamePath(pluginResourceType, meta2.getName()) + "?status=staged", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta2), bodyToMetaSet(response));

    // stage version1
    meta1 = new PluginResourceMeta(meta1.getName(), meta1.getVersion(), PluginResourceStatus.STAGED);
    meta2 = new PluginResourceMeta(meta2.getName(), meta2.getVersion(), PluginResourceStatus.INACTIVE);
    assertResponseStatus(doPost(getVersionedPath(pluginResourceType, meta1) + "/stage", "", ADMIN_HEADERS),
                         HttpResponseStatus.OK);
    // should still see both versions when getting all versions of the resource name
    response = doGet(getNamePath(pluginResourceType, meta1.getName()), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta1, meta2), bodyToMetaSet(response));
    // check get staged versions of the resources
    response = doGet(getTypePath(pluginResourceType) + "?status=staged", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(
      ImmutableMap.<String, Set<PluginResourceMeta>>of("name", ImmutableSet.<PluginResourceMeta>of(meta1)),
      bodyToMetaMap(response)
    );
    // check get staged versions of the specific resource
    response = doGet(getNamePath(pluginResourceType, meta1.getName()) + "?status=staged", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta1), bodyToMetaSet(response));

    // unstage
    meta1 = new PluginResourceMeta(meta1.getName(), meta1.getVersion(), PluginResourceStatus.INACTIVE);
    assertResponseStatus(doPost(getVersionedPath(pluginResourceType, meta1) + "/unstage", "", ADMIN_HEADERS),
                         HttpResponseStatus.OK);
    // should still see both versions when getting all versions of the resource name
    response = doGet(getNamePath(pluginResourceType, meta1.getName()), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(ImmutableSet.<PluginResourceMeta>of(meta1, meta2), bodyToMetaSet(response));
    // staged filter should return an empty map
    response = doGet(getTypePath(pluginResourceType) + "?status=staged", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertTrue(bodyToMetaMap(response).isEmpty());
    // no staged versions
    response = doGet(getNamePath(pluginResourceType, meta1.getName()) + "?status=staged", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertTrue(bodyToMetaSet(response).isEmpty());
  }

  private void testGetAndDelete(PluginResourceType type) throws Exception {
    String contents = "some contents";
    PluginResourceMeta meta1 = new PluginResourceMeta("name1", 1, PluginResourceStatus.INACTIVE);
    PluginResourceMeta meta2 = new PluginResourceMeta("name1", 2, PluginResourceStatus.INACTIVE);
    PluginResourceMeta meta3 = new PluginResourceMeta("name2", 1, PluginResourceStatus.INACTIVE);
    PluginResourceMeta meta4 = new PluginResourceMeta("name3", 1, PluginResourceStatus.INACTIVE);
    assertSendContents(contents, type, meta1.getName());
    assertSendContents(contents, type, meta2.getName());
    assertSendContents(contents, type, meta3.getName());
    assertSendContents(contents, type, meta4.getName());

    HttpResponse response = doGet(getTypePath(type), ADMIN_HEADERS);
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
    response = doGet(getTypePath(type), ADMIN_HEADERS);
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

  private String getTypePath(PluginResourceType type) {
    return Joiner.on("/").join("/v1/loom", type.getPluginType().name().toLowerCase() + "types",
                               type.getPluginName(), type.getResourceType());
  }

  private String getNamePath(PluginResourceType type, String name) {
    return Joiner.on("/").join(getTypePath(type), name);
  }

  private String getVersionedPath(PluginResourceType type, PluginResourceMeta meta) {
    return Joiner.on("/").join(getTypePath(type), meta.getName(), "versions", meta.getVersion());
  }

}
