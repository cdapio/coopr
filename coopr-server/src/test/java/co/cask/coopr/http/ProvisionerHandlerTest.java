package co.cask.coopr.http;

import co.cask.coopr.Entities;
import co.cask.coopr.account.Account;
import co.cask.coopr.provisioner.Provisioner;
import co.cask.coopr.provisioner.ProvisionerHeartbeat;
import co.cask.coopr.provisioner.plugin.ResourceType;
import co.cask.coopr.spec.Tenant;
import co.cask.coopr.spec.TenantSpecification;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.coopr.store.provisioner.SQLProvisionerStore;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.sql.SQLException;
import java.util.List;

/**
 *
 */
public class ProvisionerHandlerTest extends ServiceTestBase {
  private static final Gson GSON = new Gson();

  @Before
  public void setupProvisionerHandlerTest() throws SQLException {
    // base tests write a provisioner, need to clear it.
    injector.getInstance(SQLProvisionerStore.class).clearData();
  }

  @Test
  public void testPutAndGetProvisioner() throws Exception {
    Provisioner provisioner = new Provisioner("p1", "host", 12345, 100, null, null);
    String resource = "/provisioners/" + provisioner.getId();

    // haven't added it yet, should get a 404
    assertResponseStatus(doGetExternalAPI(resource, SUPERADMIN_HEADERS), HttpResponseStatus.NOT_FOUND);

    // put the provisioner
    assertResponseStatus(doPutInternalAPI(resource, GSON.toJson(provisioner)), HttpResponseStatus.OK);

    HttpResponse response = doGetExternalAPI(resource, SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    InputStreamReader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    Provisioner actualProvisioner = GSON.fromJson(reader, Provisioner.class);
    Assert.assertEquals(provisioner, actualProvisioner);
  }

  @Test
  public void testInvalidProvisionerInputReturns400() throws Exception {
    Provisioner provisioner = new Provisioner("p1", "host", 12345, 100, null, null);
    // test mismatching ids
    assertResponseStatus(doPutInternalAPI("/provisioners/not-" + provisioner.getId(), GSON.toJson(provisioner)),
                         HttpResponseStatus.BAD_REQUEST);

    // missing all fields
    JsonObject body = new JsonObject();
    assertResponseStatus(doPutInternalAPI("/provisioners/p1", body.toString()), HttpResponseStatus.BAD_REQUEST);
    // can't decode body
    assertResponseStatus(doPutInternalAPI("/provisioners/p1", "non-json object"), HttpResponseStatus.BAD_REQUEST);
    // missing capacity
    body.addProperty("id", "p1");
    body.addProperty("host", "hostname");
    body.addProperty("port", 12345);
    assertResponseStatus(doPutInternalAPI("/provisioners/p1", body.toString()), HttpResponseStatus.BAD_REQUEST);
    // missing host
    body.addProperty("capacityTotal", 16);
    body.remove("host");
    assertResponseStatus(doPutInternalAPI("/provisioners/p1", body.toString()), HttpResponseStatus.BAD_REQUEST);
    // missing port
    body.addProperty("host", "hostname");
    body.remove("port");
    assertResponseStatus(doPutInternalAPI("/provisioners/p1", body.toString()), HttpResponseStatus.BAD_REQUEST);
  }

  @Test
  public void testGetProvisioners() throws Exception {
    Provisioner provisioner1 = new Provisioner("p1", "host1", 12345, 100, null, null);
    Provisioner provisioner2 = new Provisioner("p2", "host2", 12345, 100, null, null);
    Provisioner provisioner3 = new Provisioner("p3", "host3", 12345, 100, null, null);

    // should return empty array if there are no provisioners
    HttpResponse response = doGetExternalAPI("/provisioners", SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    InputStreamReader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonArray output = GSON.fromJson(reader, JsonArray.class);
    Assert.assertTrue(output.size() == 0);

    // put all 3 provisioners
    List<Provisioner> provisioners = Lists.newArrayList(provisioner1, provisioner2, provisioner3);
    for (Provisioner provisioner : provisioners) {
      assertResponseStatus(doPutInternalAPI("/provisioners/" + provisioner.getId(), GSON.toJson(provisioner)),
                           HttpResponseStatus.OK);
    }

    response = doGetExternalAPI("/provisioners", SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    output = GSON.fromJson(reader, JsonArray.class);
    List<Provisioner> actual = Lists.newArrayList();
    for (JsonElement element : output) {
      actual.add(GSON.fromJson(element, Provisioner.class));
    }
    Assert.assertEquals(provisioners, actual);
  }

  @Test
  public void testHeartbeat() throws Exception {
    Provisioner provisioner = new Provisioner("p1", "host", 12345, 100, null, null);
    ProvisionerHeartbeat heartbeat = new ProvisionerHeartbeat(
      ImmutableMap.of("tenantX", 10, "tenantY", 20, "tenantZ", 10));
    String heartbeatUrl = "/provisioners/" + provisioner.getId() + "/heartbeat";
    String provisionerUrl = "/provisioners/" + provisioner.getId();

    // haven't registered the provisioner, heartbeat should return 404
    assertResponseStatus(doPostInternalAPI(heartbeatUrl, GSON.toJson(heartbeat)), HttpResponseStatus.NOT_FOUND);

    // register the provisioner
    assertResponseStatus(doPutInternalAPI(provisionerUrl, GSON.toJson(provisioner)), HttpResponseStatus.OK);
    // perform heartbeat
    assertResponseStatus(doPostInternalAPI(heartbeatUrl, GSON.toJson(heartbeat)), HttpResponseStatus.OK);

    // check usage info
    HttpResponse response = doGetExternalAPI(provisionerUrl, SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    InputStreamReader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    Provisioner actualProvisioner = GSON.fromJson(reader, Provisioner.class);
    Assert.assertEquals(heartbeat.getUsage(), actualProvisioner.getUsage());

    // perform another heartbeat
    heartbeat = new ProvisionerHeartbeat(
      ImmutableMap.of("tenantX", 10, "tenantY", 50, "tenantZ", 20));
    assertResponseStatus(doPostInternalAPI(heartbeatUrl, GSON.toJson(heartbeat)), HttpResponseStatus.OK);

    // check usage info
    response = doGetExternalAPI(provisionerUrl, SUPERADMIN_HEADERS);
    assertResponseStatus(response, HttpResponseStatus.OK);
    reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    actualProvisioner = GSON.fromJson(reader, Provisioner.class);
    Assert.assertEquals(heartbeat.getUsage(), actualProvisioner.getUsage());
  }

  @Test
  public void testProviderTypes() throws Exception {
    testNonPostRestAPIs("providertypes", gson.toJsonTree(Entities.ProviderTypeExample.JOYENT).getAsJsonObject(),
                        gson.toJsonTree(Entities.ProviderTypeExample.RACKSPACE).getAsJsonObject(), SUPERADMIN_HEADERS);
  }

  @Test
  public void testAutomatorTypes() throws Exception {
    testNonPostRestAPIs("automatortypes", gson.toJsonTree(Entities.AutomatorTypeExample.CHEF).getAsJsonObject(),
                        gson.toJsonTree(Entities.AutomatorTypeExample.SHELL).getAsJsonObject(), SUPERADMIN_HEADERS);
  }

  @Test
  public void testEditProviderTypesMustBeSuperadmin() throws Exception {
    tenantStore.writeTenant(
      new Tenant(ADMIN_ACCOUNT.getTenantId(), new TenantSpecification(TENANT, 500, 1000, 10000)));
    ProviderType type = Entities.ProviderTypeExample.RACKSPACE;
    assertResponseStatus(doPutInternalAPI("/plugins/providertypes/" + type.getName(), gson.toJson(type)),
                         HttpResponseStatus.OK);
    assertResponseStatus(doDeleteExternalAPI("/plugins/providertypes/" + type.getName(), ADMIN_HEADERS),
                         HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doDeleteExternalAPI("/plugins/providertypes/" + type.getName(), SUPERADMIN_HEADERS),
                         HttpResponseStatus.OK);
  }

  @Test
  public void testEditAutomatorTypesMustBeSuperadmin() throws Exception {
    tenantStore.writeTenant(
      new Tenant(ADMIN_ACCOUNT.getTenantId(), new TenantSpecification(TENANT, 500, 1000, 10000)));
    AutomatorType type = Entities.AutomatorTypeExample.CHEF;
    assertResponseStatus(doPutInternalAPI("/plugins/automatortypes/" + type.getName(), gson.toJson(type)),
                         HttpResponseStatus.OK);
    assertResponseStatus(doDeleteExternalAPI("/plugins/automatortypes/" + type.getName(), ADMIN_HEADERS),
                         HttpResponseStatus.FORBIDDEN);
    assertResponseStatus(doDeleteExternalAPI("/plugins/automatortypes/" + type.getName(), SUPERADMIN_HEADERS),
                         HttpResponseStatus.OK);
  }

  private void testNonPostRestAPIs(String entityType, JsonObject entity1, JsonObject entity2,
                                   Header[] headers) throws Exception {
    String base = "/plugins/" + entityType;
    String entity1Path = base + "/" + entity1.get("name").getAsString();
    String entity2Path = base + "/" + entity2.get("name").getAsString();
    // should start off with no entities
    assertResponseStatus(doGetExternalAPI(entity1Path, headers), HttpResponseStatus.NOT_FOUND);

    // add entity through PUT
    assertResponseStatus(doPutInternalAPI(entity1Path, entity1.toString(), headers), HttpResponseStatus.OK);
    // check we can get it
    HttpResponse response = doGetExternalAPI(entity1Path, headers);
    assertResponseStatus(response, HttpResponseStatus.OK);
    Reader reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonObject result = new Gson().fromJson(reader, JsonObject.class);
    Assert.assertEquals(entity1, result);

    // add second entity through PUT
    assertResponseStatus(doPutInternalAPI(entity2Path, entity2.toString(), headers), HttpResponseStatus.OK);
    // check we can get it
    response = doGetExternalAPI(entity2Path, headers);
    assertResponseStatus(response, HttpResponseStatus.OK);
    reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    result = new Gson().fromJson(reader, JsonObject.class);
    Assert.assertEquals(entity2, result);

    // get both entities
    response = doGetExternalAPI(base, headers);
    assertResponseStatus(response, HttpResponseStatus.OK);
    reader = new InputStreamReader(response.getEntity().getContent(), Charsets.UTF_8);
    JsonArray results = new Gson().fromJson(reader, JsonArray.class);

    Assert.assertEquals(2, results.size());
    JsonObject first = results.get(0).getAsJsonObject();
    JsonObject second = results.get(1).getAsJsonObject();
    if (first.get("name").getAsString().equals(entity1.get("name").getAsString())) {
      Assert.assertEquals(entity1, first);
      Assert.assertEquals(entity2, second);
    } else {
      Assert.assertEquals(entity2, first);
      Assert.assertEquals(entity1, second);
    }

    assertResponseStatus(doDeleteExternalAPI(entity1Path, headers), HttpResponseStatus.OK);
    assertResponseStatus(doDeleteExternalAPI(entity2Path, headers), HttpResponseStatus.OK);
    // check both were deleted
    assertResponseStatus(doGetExternalAPI(entity1Path, headers), HttpResponseStatus.NOT_FOUND);
    assertResponseStatus(doGetExternalAPI(entity2Path, headers), HttpResponseStatus.NOT_FOUND);
  }
}
