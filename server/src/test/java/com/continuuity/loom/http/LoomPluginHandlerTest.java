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

import com.continuuity.loom.Entities;
import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.provisioner.plugin.PluginType;
import com.continuuity.loom.provisioner.plugin.ResourceMeta;
import com.continuuity.loom.provisioner.plugin.ResourceStatus;
import com.continuuity.loom.provisioner.plugin.ResourceType;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class LoomPluginHandlerTest extends LoomServiceTestBase {

  @Before
  public void setupPluginHandlerTest() throws Exception {
    entityStoreService.getView(SUPERADMIN_ACCOUNT).writeAutomatorType(Entities.AutomatorTypeExample.SHELL);
    entityStoreService.getView(SUPERADMIN_ACCOUNT).writeAutomatorType(Entities.AutomatorTypeExample.CHEF);
    entityStoreService.getView(SUPERADMIN_ACCOUNT).writeProviderType(Entities.ProviderTypeExample.JOYENT);
  }

  @Test
  public void testNonAdminGetsForbidden() throws Exception {
    ResourceType type1 = new ResourceType(PluginType.PROVIDER, "joyent", "keys");
    ResourceType type2 = new ResourceType(PluginType.AUTOMATOR, "shell", "script");
    ResourceMeta meta = new ResourceMeta("name", 1);
    assertResponseStatus(doPost(getNamePath(type1, "name"), "contents", USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doPost(getNamePath(type2, "name"), "contents", USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doDelete(getVersionedPath(type1, meta), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doDelete(getVersionedPath(type2, meta), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet(getNamePath(type1, "name"), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet(getNamePath(type2, "name"), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet(getTypePath(type1), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doGet(getTypePath(type2), USER1_HEADERS), HttpResponseStatus.FORBIDDEN);
  }

  @Test
  public void testCallNonexistentResourceReturns404() throws Exception {
    List<String> getPaths = ImmutableList.of(
      // test nonexistent plugin
      "/v1/loom/providertypes/nonexistent/keys",
      "/v1/loom/automatortypes/nonexistent/cookbooks",
      // test nonexistent plugin resource type
      "/v1/loom/providertypes/joyent/cookbooks",
      "/v1/loom/automatortypes/chef-solo/keys"
    );
    for (String getPath : getPaths) {
      assertResponseStatus(doGet(getPath, ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);
    }
  }

  @Test
  public void testPutAndGetAutomatorTypeModule() throws Exception {
    testPutAndGet(PluginType.AUTOMATOR, "shell", "scripts");
  }

  @Test
  public void testPutAndGetProviderTypeModule() throws Exception {
    testPutAndGet(PluginType.PROVIDER, "joyent", "keys");
  }

  @Test
  public void testActivateDeactivateAutomatorTypeModule() throws Exception {
    testVersions(PluginType.AUTOMATOR, "shell", "scripts");
  }

  @Test
  public void testActivateDeactivateProviderTypeModule() throws Exception {
    testVersions(PluginType.PROVIDER, "joyent", "keys");
  }

  @Test
  public void testGetAndDeleteAutomatorTypeResources() throws Exception {
    testGetAndDelete(new ResourceType(PluginType.AUTOMATOR, "shell", "scripts"));
  }

  @Test
  public void testGetAndDeleteProviderTypeResources() throws Exception {
    testGetAndDelete(new ResourceType(PluginType.PROVIDER, "joyent", "keys"));
  }

  @Test
  public void testStageUnstageOnNonexistentReturns404() throws Exception {
    ResourceType cookbooks = new ResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    ResourceType keys = new ResourceType(PluginType.PROVIDER, "joyent", "keys");

    assertSendContents("hadoop contents 1", cookbooks, "hadoop");
    assertSendContents("dev keys 1", keys, "dev");

    List<String> paths = ImmutableList.of(
      getVersionedPath(cookbooks, "hadoop", 2) + "/stage",
      getVersionedPath(cookbooks, "hadoop", 2) + "/unstage",
      getVersionedPath(keys, "dev", 2) + "/stage",
      getVersionedPath(keys, "dev", 2) + "/unstage"
    );
    for (String path : paths) {
      assertResponseStatus(doPost(path, "", ADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);
    }
  }

  @Test
  public void testSync() throws Exception {
    ResourceType cookbooks = new ResourceType(PluginType.AUTOMATOR, "chef-solo", "cookbooks");
    ResourceType keys = new ResourceType(PluginType.PROVIDER, "joyent", "keys");
    ResourceMeta hadoop1 = new ResourceMeta("hadoop", 1, ResourceStatus.INACTIVE);
    ResourceMeta hadoop2 = new ResourceMeta("hadoop", 2, ResourceStatus.ACTIVE);
    ResourceMeta hadoop3 = new ResourceMeta("hadoop", 3, ResourceStatus.INACTIVE);
    ResourceMeta mysql1 = new ResourceMeta("mysql", 1, ResourceStatus.ACTIVE);
    ResourceMeta mysql2 = new ResourceMeta("mysql", 2, ResourceStatus.INACTIVE);
    ResourceMeta dev1 = new ResourceMeta("dev", 1, ResourceStatus.INACTIVE);
    ResourceMeta dev2 = new ResourceMeta("dev", 2, ResourceStatus.ACTIVE);
    ResourceMeta research1 = new ResourceMeta("research", 1, ResourceStatus.INACTIVE);

    // upload 3 versions of hadoop cookbook
    assertSendContents("hadoop contents 1", cookbooks, "hadoop");
    assertSendContents("hadoop contents 2", cookbooks, "hadoop");
    assertSendContents("hadoop contents 3", cookbooks, "hadoop");
    // upload 2 versions of mysql cookbook
    assertSendContents("mysql contents 1", cookbooks, "mysql");
    assertSendContents("mysql contents 2", cookbooks, "mysql");
    // upload 2 versions of dev keys
    assertSendContents("dev keys 1", keys, "dev");
    assertSendContents("dev keys 2", keys, "dev");
    // upload 1 version of research keys
    assertSendContents("research keys 1", keys, "research");

    // stage version 2 of hadoop
    assertResponseStatus(doPost(getVersionedPath(cookbooks, "hadoop", 2) + "/stage", "", ADMIN_HEADERS),
                         HttpResponseStatus.OK);
    // stage version 1 of mysql
    assertResponseStatus(doPost(getVersionedPath(cookbooks, "mysql", 1) + "/stage", "", ADMIN_HEADERS),
                         HttpResponseStatus.OK);
    // stage version 2 of dev
    assertResponseStatus(doPost(getVersionedPath(keys, "dev", 2) + "/stage", "", ADMIN_HEADERS),
                         HttpResponseStatus.OK);

    // sync
    assertResponseStatus(doPost("/v1/loom/sync", "", ADMIN_HEADERS), HttpResponseStatus.OK);

    // check cookbooks
    HttpResponse response = doGet(getTypePath(cookbooks), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Map<String, Set<ResourceMeta>> actual = bodyToMetaMap(response);
    Map<String, Set<ResourceMeta>> expected = ImmutableMap.<String, Set<ResourceMeta>>of(
      "hadoop", ImmutableSet.<ResourceMeta>of(hadoop1, hadoop2, hadoop3),
      "mysql", ImmutableSet.<ResourceMeta>of(mysql1, mysql2)
    );
    Assert.assertEquals(expected, actual);
    // check keys
    response = doGet(getTypePath(keys), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    actual = bodyToMetaMap(response);
    expected = ImmutableMap.<String, Set<ResourceMeta>>of(
      "dev", ImmutableSet.<ResourceMeta>of(dev1, dev2),
      "research", ImmutableSet.<ResourceMeta>of(research1)
    );
    Assert.assertEquals(expected, actual);

    // stage version3 of hadoop
    assertResponseStatus(doPost(getVersionedPath(cookbooks, "hadoop", 3) + "/stage", "", ADMIN_HEADERS),
                         HttpResponseStatus.OK);
    // unstage version1 of mysql
    assertResponseStatus(doPost(getVersionedPath(cookbooks, "mysql", 1) + "/unstage", "", ADMIN_HEADERS),
                         HttpResponseStatus.OK);
    response = doGet(getTypePath(cookbooks), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    actual = bodyToMetaMap(response);

    // sync
    assertResponseStatus(doPost("/v1/loom/sync", "", ADMIN_HEADERS), HttpResponseStatus.OK);

    // check cookbooks
    response = doGet(getTypePath(cookbooks), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    actual = bodyToMetaMap(response);
    hadoop2 = new ResourceMeta("hadoop", 2, ResourceStatus.INACTIVE);
    hadoop3 = new ResourceMeta("hadoop", 3, ResourceStatus.ACTIVE);
    mysql1 = new ResourceMeta("mysql", 1, ResourceStatus.INACTIVE);
    expected = ImmutableMap.<String, Set<ResourceMeta>>of(
      "hadoop", ImmutableSet.<ResourceMeta>of(hadoop1, hadoop2, hadoop3),
      "mysql", ImmutableSet.<ResourceMeta>of(mysql1, mysql2)
    );
    Assert.assertEquals(expected, actual);
  }

  private void assertSendContents(String contents, PluginType type, String pluginName, String resourceType,
                                  String resourceName) throws Exception {
    assertSendContents(contents, new ResourceType(type, pluginName, resourceType), resourceName);
  }

  private void assertSendContents(String contents, ResourceType type, String name) throws Exception {
    String path = getNamePath(type, name);
    HttpResponse response = doPost(path, contents, ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent());
    ResourceMeta responseMeta = gson.fromJson(reader, ResourceMeta.class);
    Assert.assertEquals(name, responseMeta.getName());
    Assert.assertEquals(ResourceStatus.INACTIVE, responseMeta.getStatus());
  }

  private void testPutAndGet(PluginType type, String pluginName, String resourceType) throws Exception {
    String contents = RandomStringUtils.randomAlphanumeric(8 * Constants.PLUGIN_RESOURCE_CHUNK_SIZE);
    ResourceType pluginResourceType = new ResourceType(type, pluginName, resourceType);
    ResourceMeta meta = new ResourceMeta("hello", 1, ResourceStatus.INACTIVE);
    assertSendContents(contents, type, pluginName, resourceType, "hello");
    // get metadata
    HttpResponse response = doGet(getNamePath(pluginResourceType, meta.getName()), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(ImmutableSet.of(meta), bodyToMetaSet(response));

    // get actual contents
    String typeStr = type.name().toLowerCase() + "types";
    String path = Joiner.on('/').join(
      "/v1/tenants",
      ADMIN_ACCOUNT.getTenantId(),
      typeStr,
      pluginName,
      resourceType,
      meta.getName(),
      "versions",
      meta.getVersion()
    );
    response = doGet(path);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(contents, bodyToString(response));
  }

  private void testVersions(PluginType type, String pluginName, String resourceType) throws Exception{
    String contents = "some contents";
    ResourceType pluginResourceType = new ResourceType(type, pluginName, resourceType);
    ResourceMeta meta1 = new ResourceMeta("name", 1, ResourceStatus.INACTIVE);
    ResourceMeta meta2 = new ResourceMeta("name", 2, ResourceStatus.INACTIVE);
    assertSendContents(contents, pluginResourceType, meta1.getName());
    assertSendContents(contents, pluginResourceType, meta2.getName());

    // stage version2
    meta2 = new ResourceMeta(meta2.getName(), meta2.getVersion(), ResourceStatus.STAGED);
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
      ImmutableMap.<String, Set<ResourceMeta>>of("name", ImmutableSet.<ResourceMeta>of(meta2)),
      bodyToMetaMap(response)
    );
    // check get staged version of the specific resource
    response = doGet(getNamePath(pluginResourceType, meta2.getName()) + "?status=staged", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta2), bodyToMetaSet(response));

    // stage version1
    meta1 = new ResourceMeta(meta1.getName(), meta1.getVersion(), ResourceStatus.STAGED);
    meta2 = new ResourceMeta(meta2.getName(), meta2.getVersion(), ResourceStatus.INACTIVE);
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
      ImmutableMap.<String, Set<ResourceMeta>>of("name", ImmutableSet.<ResourceMeta>of(meta1)),
      bodyToMetaMap(response)
    );
    // check get staged versions of the specific resource
    response = doGet(getNamePath(pluginResourceType, meta1.getName()) + "?status=staged", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta1), bodyToMetaSet(response));

    // unstage
    meta1 = new ResourceMeta(meta1.getName(), meta1.getVersion(), ResourceStatus.INACTIVE);
    assertResponseStatus(doPost(getVersionedPath(pluginResourceType, meta1) + "/unstage", "", ADMIN_HEADERS),
                         HttpResponseStatus.OK);
    // should still see both versions when getting all versions of the resource name
    response = doGet(getNamePath(pluginResourceType, meta1.getName()), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(ImmutableSet.<ResourceMeta>of(meta1, meta2), bodyToMetaSet(response));
    // staged filter should return an empty map
    response = doGet(getTypePath(pluginResourceType) + "?status=staged", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertTrue(bodyToMetaMap(response).isEmpty());
    // no staged versions
    response = doGet(getNamePath(pluginResourceType, meta1.getName()) + "?status=staged", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertTrue(bodyToMetaSet(response).isEmpty());
  }

  private void testGetAndDelete(ResourceType type) throws Exception {
    String contents = "some contents";
    ResourceMeta meta1 = new ResourceMeta("name1", 1, ResourceStatus.INACTIVE);
    ResourceMeta meta2 = new ResourceMeta("name1", 2, ResourceStatus.INACTIVE);
    ResourceMeta meta3 = new ResourceMeta("name2", 1, ResourceStatus.INACTIVE);
    ResourceMeta meta4 = new ResourceMeta("name3", 1, ResourceStatus.INACTIVE);
    assertSendContents(contents, type, meta1.getName());
    assertSendContents(contents, type, meta2.getName());
    assertSendContents(contents, type, meta3.getName());
    assertSendContents(contents, type, meta4.getName());

    HttpResponse response = doGet(getTypePath(type), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(
      ImmutableMap.<String, Set<ResourceMeta>>of(
        "name1", ImmutableSet.<ResourceMeta>of(meta1, meta2),
        "name2", ImmutableSet.<ResourceMeta>of(meta3),
        "name3", ImmutableSet.<ResourceMeta>of(meta4)),
      bodyToMetaMap(response)
    );

    // delete one
    assertResponseStatus(doDelete(getVersionedPath(type, meta3), ADMIN_HEADERS), HttpResponseStatus.OK);
    response = doGet(getTypePath(type), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(
      ImmutableMap.<String, Set<ResourceMeta>>of(
        "name1", ImmutableSet.<ResourceMeta>of(meta1, meta2),
        "name3", ImmutableSet.<ResourceMeta>of(meta4)),
      bodyToMetaMap(response)
    );
  }

  private Map<String, Set<ResourceMeta>> bodyToMetaMap(HttpResponse response) throws IOException {
    Reader reader = new InputStreamReader(response.getEntity().getContent());
    return gson.fromJson(reader, new TypeToken<Map<String, Set<ResourceMeta>>>() {}.getType());
  }

  private Set<ResourceMeta> bodyToMetaSet(HttpResponse response) throws IOException {
    Reader reader = new InputStreamReader(response.getEntity().getContent());
    return gson.fromJson(reader, new TypeToken<Set<ResourceMeta>>() {}.getType());
  }

  private String bodyToString(HttpResponse response) throws IOException {
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    try {
      return CharStreams.toString(reader);
    } finally {
      reader.close();
    }
  }

  private String getTypePath(ResourceType type) {
    return Joiner.on("/").join("/v1/loom", type.getPluginType().name().toLowerCase() + "types",
                               type.getPluginName(), type.getTypeName());
  }

  private String getNamePath(ResourceType type, String name) {
    return Joiner.on("/").join(getTypePath(type), name);
  }

  private String getVersionedPath(ResourceType type, String name, int version) {
    return Joiner.on("/").join(getTypePath(type), name, "versions", version);
  }

  private String getVersionedPath(ResourceType type, ResourceMeta meta) {
    return getVersionedPath(type, meta.getName(), meta.getVersion());
  }

}
