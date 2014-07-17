package com.continuuity.loom.http;

import com.continuuity.loom.common.conf.Constants;
import com.continuuity.loom.provisioner.PluginResourceMeta;
import com.continuuity.loom.provisioner.PluginResourceType;
import com.continuuity.loom.store.provisioner.PluginType;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Set;

/**
 *
 */
public class LoomPluginHandlerTest extends LoomServiceTestBase {

  @Test
  public void testNonAdminGetsForbidden() throws Exception {

  }

  private void assertSendContents(String contents, PluginType type, String pluginName, String resourceType,
                                  String resourceName, String version) throws Exception {
    assertSendContents(contents, new PluginResourceType(type, pluginName, resourceType),
                       new PluginResourceMeta(resourceName, version));
  }

  private void assertSendContents(String contents, PluginResourceType type, PluginResourceMeta meta) throws Exception {
    String path = getVersionedPath(type, meta);
    assertResponseStatus(doPut(path, contents, ADMIN_HEADERS), HttpResponseStatus.OK);
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

  private void testPutAndGet(PluginType type, String pluginName, String resourceType) throws Exception {
    String contents = RandomStringUtils.randomAlphanumeric(8 * Constants.PLUGIN_RESOURCE_CHUNK_SIZE);
    PluginResourceType pluginResourceType = new PluginResourceType(type, pluginName, resourceType);
    PluginResourceMeta meta = new PluginResourceMeta("hello", "1", false);
    assertSendContents(contents, type, pluginName, resourceType, "hello", "1");
    // get metadata
    HttpResponse response = doGet(getUnVersionedPath(pluginResourceType, meta), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta), bodyToMetaSet(response));
    // get actual contents
    response = doGet(getVersionedPath(pluginResourceType, meta), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(contents, bodyToString(response));
  }

  private void testVersions(PluginType type, String pluginName, String resourceType) throws Exception{
    String contents = "some contents";
    PluginResourceType pluginResourceType = new PluginResourceType(type, pluginName, resourceType);
    PluginResourceMeta meta1 = new PluginResourceMeta("name", "1", false);
    PluginResourceMeta meta2 = new PluginResourceMeta("name", "2", true);
    assertSendContents(contents, pluginResourceType, meta1);
    assertSendContents(contents, pluginResourceType, meta2);

    // activate version2
    assertResponseStatus(doPost(getVersionedPath(pluginResourceType, meta2) + "/activate", "", ADMIN_HEADERS),
                         HttpResponseStatus.OK);
    // should still see both versions when getting all versions of the resource name
    HttpResponse response = doGet(getUnVersionedPath(pluginResourceType, meta2), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta1, meta2), bodyToMetaSet(response));
    // check get active versions of the resources
    response = doGet(getPath(pluginResourceType) + "?active=true", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta2), bodyToMetaSet(response));
    // check get active version of the specific resource
    response = doGet(getUnVersionedPath(pluginResourceType, meta2) + "?active=true", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(meta2, bodyToMeta(response));

    // activate version1
    meta1 = new PluginResourceMeta("name", "1", true);
    meta2 = new PluginResourceMeta("name", "2", false);
    assertResponseStatus(doPost(getVersionedPath(pluginResourceType, meta1) + "/activate", "", ADMIN_HEADERS),
                         HttpResponseStatus.OK);
    // should still see both versions when getting all versions of the resource name
    response = doGet(getUnVersionedPath(pluginResourceType, meta1), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta1, meta2), bodyToMetaSet(response));
    // check get active versions of the resources
    response = doGet(getPath(pluginResourceType) + "?active=true", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta1), bodyToMetaSet(response));
    // check get active version of the specific resource
    response = doGet(getUnVersionedPath(pluginResourceType, meta1) + "?active=true", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(meta1, bodyToMeta(response));

    // deactivate
    meta1 = new PluginResourceMeta("name", "1", false);
    meta2 = new PluginResourceMeta("name", "2", false);
    assertResponseStatus(doPost(getUnVersionedPath(pluginResourceType, meta1) + "/deactivate", "", ADMIN_HEADERS),
                         HttpResponseStatus.OK);
    // should still see both versions when getting all versions of the resource name
    response = doGet(getUnVersionedPath(pluginResourceType, meta1), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta1, meta2), bodyToMetaSet(response));
    // active flag should return an empty list
    response = doGet(getPath(pluginResourceType) + "?active=true", ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertTrue(bodyToMetaSet(response).isEmpty());
    // no active versions
    assertResponseStatus(doGet(getUnVersionedPath(pluginResourceType, meta1) + "?active=true", ADMIN_HEADERS),
                         HttpResponseStatus.NOT_FOUND);
  }

  private void testGetAndDelete(PluginResourceType type) throws Exception {
    String contents = "some contents";
    PluginResourceMeta meta1 = new PluginResourceMeta("name1", "1", false);
    PluginResourceMeta meta2 = new PluginResourceMeta("name1", "2", false);
    PluginResourceMeta meta3 = new PluginResourceMeta("name2", "1", false);
    PluginResourceMeta meta4 = new PluginResourceMeta("name3", "2", false);
    assertSendContents(contents, type, meta1);
    assertSendContents(contents, type, meta2);
    assertSendContents(contents, type, meta3);
    assertSendContents(contents, type, meta4);

    HttpResponse response = doGet(getPath(type), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta1, meta2, meta3, meta4), bodyToMetaSet(response));

    // delete one
    assertResponseStatus(doDelete(getVersionedPath(type, meta3), ADMIN_HEADERS), HttpResponseStatus.OK);
    response = doGet(getPath(type), ADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Assert.assertEquals(Sets.newHashSet(meta1, meta2, meta4), bodyToMetaSet(response));
  }

  private Set<PluginResourceMeta> bodyToMetaSet(HttpResponse response) throws IOException {
    Reader reader = new InputStreamReader(response.getEntity().getContent());
    return gson.fromJson(reader, new TypeToken<Set<PluginResourceMeta>>() {}.getType());
  }

  private PluginResourceMeta bodyToMeta(HttpResponse response) throws IOException {
    Reader reader = new InputStreamReader(response.getEntity().getContent());
    return gson.fromJson(reader, PluginResourceMeta.class);
  }

  private String bodyToString(HttpResponse response) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
    StringBuilder sb = new StringBuilder();
    String line = reader.readLine();
    while (line != null) {
      sb.append(line);
      line = reader.readLine();
    }
    return sb.toString();
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
}
