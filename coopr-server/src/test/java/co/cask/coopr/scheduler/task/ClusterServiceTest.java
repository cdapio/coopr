package co.cask.coopr.scheduler.task;

import co.cask.coopr.BaseTest;
import co.cask.coopr.Entities;
import co.cask.coopr.account.Account;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.cluster.ClusterService;
import co.cask.coopr.cluster.MissingFieldsException;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.http.request.AddServicesRequest;
import co.cask.coopr.http.request.ClusterConfigureRequest;
import co.cask.coopr.http.request.ClusterCreateRequest;
import co.cask.coopr.http.request.ClusterOperationRequest;
import co.cask.coopr.provisioner.Provisioner;
import co.cask.coopr.provisioner.TenantProvisionerService;
import co.cask.coopr.scheduler.ClusterAction;
import co.cask.coopr.spec.HardwareType;
import co.cask.coopr.spec.ImageType;
import co.cask.coopr.spec.Provider;
import co.cask.coopr.spec.Tenant;
import co.cask.coopr.spec.TenantSpecification;
import co.cask.coopr.spec.plugin.FieldSchema;
import co.cask.coopr.spec.plugin.ParameterType;
import co.cask.coopr.spec.plugin.ParametersSpecification;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.coopr.spec.service.Service;
import co.cask.coopr.spec.template.ClusterDefaults;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.Compatibilities;
import co.cask.coopr.store.entity.EntityStoreView;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class ClusterServiceTest extends BaseTest {
  private static ClusterService clusterService;
  private static Account account;
  private static Provider provider;
  private static ProviderType providerType;
  private static ClusterTemplate basicTemplate;
  private static Service service1 = Entities.ServiceExample.HOSTS;
  private static Service service2 = Entities.ServiceExample.NAMENODE;
  private static ImageType imageType = Entities.ImageTypeExample.UBUNTU_12;
  private static HardwareType hardwareType = Entities.HardwareTypeExample.LARGE;

  @Override
  protected boolean shouldClearDataBetweenTests() {
    return false;
  }

  @BeforeClass
  public static void setupClusterServiceTests() throws Exception {
    clusterService = injector.getInstance(ClusterService.class);
    TenantProvisionerService tenantProvisionerService = injector.getInstance(TenantProvisionerService.class);
    // setup data
    tenantProvisionerService.writeProvisioner(new Provisioner("p1", "host", 50056, 100, null, null));
    tenantProvisionerService.writeTenantSpecification(new TenantSpecification("tenantX", 10, 1, 10));
    Tenant tenant = tenantStore.getTenantByName("tenantX");
    account = new Account("user9", tenant.getId());
    // write relevant entities
    EntityStoreView entityStoreView = entityStoreService.getView(new Account(Constants.ADMIN_USER, tenant.getId()));
    providerType = ProviderType.builder()
      .setParameters(ImmutableMap.of(
        ParameterType.ADMIN, new ParametersSpecification(
        ImmutableMap.of(
          "region", FieldSchema.builder().setLabel("region").setType("text").setOverride(true).build(),
          "url", FieldSchema.builder().setLabel("url").setType("text").setOverride(true).setSensitive(true).build()),
        ImmutableSet.<Set<String>>of()
      ),
        ParameterType.USER, new ParametersSpecification(
        ImmutableMap.of(
          "keyname", FieldSchema.builder().setLabel("keyname").setType("text").setSensitive(false).build(),
          "key", FieldSchema.builder().setLabel("key").setType("text").setSensitive(true).build()),
        ImmutableSet.<Set<String>>of(ImmutableSet.of("keyname", "key"))
      )
      ))
      .setName("providertype")
      .build();
    entityStoreService.getView(Account.SUPERADMIN).writeProviderType(providerType);
    provider = Provider.builder()
      .setProviderType(providerType.getName())
      .setProvisionerFields(ImmutableMap.<String, Object>of("region", "iad", "url", "http://abc.com/api"))
      .setName("provider")
      .build();
    entityStoreView.writeProvider(provider);
    entityStoreView.writeHardwareType(hardwareType);
    entityStoreView.writeImageType(imageType);
    entityStoreView.writeService(service1);
    entityStoreView.writeService(service2);
    basicTemplate = ClusterTemplate.builder()
      .setName("basic")
      .setClusterDefaults(
        ClusterDefaults.builder()
          .setProvider(provider.getName())
          .setServices(service1.getName())
          .setHardwaretype(hardwareType.getName())
          .setImagetype(imageType.getName())
          .build())
      .setCompatibilities(
        Compatibilities.builder()
          .setHardwaretypes(hardwareType.getName())
          .setImagetypes(imageType.getName())
          .setServices(service1.getName(), service2.getName()).build())
      .build();
    entityStoreView.writeClusterTemplate(basicTemplate);
  }

  @After
  public void cleanupClusterServiceTest() throws Exception {
    credentialStore.wipe();
    clusterStoreService.clearData();
  }

  @Test
  public void testValidClusterCreate() throws Exception {
    String name = "clusty";
    Map<String, Object> providerFields = Maps.newHashMap();
    providerFields.put("keyname", "myname");
    providerFields.put("key", "keycontents");
    providerFields.put("region", "dfw");
    providerFields.put("url", "internal.net/api");
    ClusterCreateRequest createRequest = ClusterCreateRequest.builder()
      .setName(name)
      .setClusterTemplateName(basicTemplate.getName())
      .setNumMachines(1)
      .setProviderFields(providerFields)
      .build();
    String clusterId = clusterService.requestClusterCreate(createRequest, account);
    Cluster cluster = clusterStore.getCluster(clusterId);
    Assert.assertEquals(basicTemplate, cluster.getClusterTemplate());
    Assert.assertEquals(account, cluster.getAccount());
    Assert.assertEquals(name, cluster.getName());
    Assert.assertEquals(provider.getName(), cluster.getProvider().getName());
    // key is a sensitive field, should be in the credential store and not in the cluster object
    // url is a weird case. admin fields that are overridable and sensitive means the user provided override should
    // never be persisted, but the admin given default can be persisted.
    Map<String, String> expectedSensitiveFields = ImmutableMap.of(
      "key", "keycontents",
      "url", "internal.net/api"
    );
    Map<String, Object> expectedNonsensitiveFields = ImmutableMap.<String, Object>of(
      "keyname", "myname",
      "region", "dfw",
      "url", "http://abc.com/api"
    );
    Assert.assertEquals(expectedNonsensitiveFields, cluster.getProvider().getProvisionerFields());
    Assert.assertEquals(expectedSensitiveFields, credentialStore.get(account.getTenantId(), clusterId));
  }

  @Test(expected = MissingEntityException.class)
  public void testInvalidProviderClusterCreate() throws Exception {
    String name = "clusty";
    Map<String, Object> providerFields = ImmutableMap.<String, Object>of("keyname", "ec2", "key", "keycontents");
    ClusterCreateRequest createRequest = ClusterCreateRequest.builder()
      .setName(name)
      .setClusterTemplateName(basicTemplate.getName())
      .setProviderName("not" + provider.getName())
      .setNumMachines(1)
      .setProviderFields(providerFields)
      .build();
    clusterService.requestClusterCreate(createRequest, account);
  }

  @Test(expected = MissingEntityException.class)
  public void testInvalidTemplateClusterCreate() throws Exception {
    String name = "clusty";
    Map<String, Object> providerFields = ImmutableMap.<String, Object>of("keyname", "ec2", "key", "keycontents");
    ClusterCreateRequest createRequest = ClusterCreateRequest.builder()
      .setName(name)
      .setClusterTemplateName("not" + basicTemplate.getName())
      .setNumMachines(1)
      .setProviderFields(providerFields)
      .build();
    clusterService.requestClusterCreate(createRequest, account);
  }

  @Test
  public void testClusterConfigure() throws Exception {
    Cluster cluster = createActiveCluster();

    Map<String, Object> providerFields = Maps.newHashMap();
    providerFields.put("keyname", "somename");
    providerFields.put("key", "somecontents");
    providerFields.put("url", "internal.net/api");
    ClusterConfigureRequest configureRequest = new ClusterConfigureRequest(providerFields, new JsonObject(), false);
    clusterService.requestClusterReconfigure(cluster.getId(), account, configureRequest);

    cluster = clusterStore.getCluster(cluster.getId());
    // key and url are both sensitive fields
    Map<String, String> expectedSensitiveFields = ImmutableMap.of(
      "key", "somecontents",
      "url", "internal.net/api"
    );
    // nonsensitive fields should be everything currently in the provider plus the nonsensitive user fields
    // given in the request
    Map<String, Object> expectedNonsensitiveFields = Maps.newHashMap(provider.getProvisionerFields());
    expectedNonsensitiveFields.put("keyname", "somename");
    Assert.assertEquals(expectedNonsensitiveFields, cluster.getProvider().getProvisionerFields());
    Assert.assertEquals(expectedSensitiveFields, credentialStore.get(account.getTenantId(), cluster.getId()));
  }

  @Test
  public void testRequiredUserFields() throws Exception {
    Cluster cluster = createActiveCluster();

    // the "key" user field is required. Should throw an except if its not set.
    Map<String, Object> providerFields = Maps.newHashMap();
    ClusterConfigureRequest configureRequest = new ClusterConfigureRequest(providerFields, new JsonObject(), false);
    boolean failed = false;
    try {
      clusterService.requestClusterReconfigure(cluster.getId(), account, configureRequest);
    } catch (MissingFieldsException e) {
      // this is expected
      failed = true;
    }
    Assert.assertTrue(failed);

    // now try with required user field set
    providerFields.put("key", "keycontents");
    configureRequest = new ClusterConfigureRequest(providerFields, new JsonObject(), false);
    clusterService.requestClusterReconfigure(cluster.getId(), account, configureRequest);

    // nonsensitive fields should be everything currently in the provider before we get the updated cluster
    Map<String, Object> expectedNonsensitiveFields = cluster.getProvider().getProvisionerFields();
    // get the updated cluster
    cluster = clusterStore.getCluster(cluster.getId());
    // key and url are both sensitive fields
    Map<String, String> expectedSensitiveFields = ImmutableMap.of(
      "key", "keycontents"
    );
    Assert.assertEquals(expectedNonsensitiveFields, cluster.getProvider().getProvisionerFields());
    Assert.assertEquals(expectedSensitiveFields, credentialStore.get(account.getTenantId(), cluster.getId()));
  }

  @Test
  public void testSensitiveUserFields() throws Exception {
    Map<String, Object> sensitiveFields = Maps.newHashMap();
    sensitiveFields.put("key", "keycontents");
    AddServicesRequest addRequest = new AddServicesRequest(sensitiveFields, ImmutableSet.of(service2.getName()));
    ClusterOperationRequest opRequest = new ClusterOperationRequest(sensitiveFields);

    Cluster cluster = createActiveCluster();
    clusterService.requestAddServices(cluster.getId(), account, addRequest);
    testSensitiveFieldsAdded(cluster, sensitiveFields);
    clusterStore.deleteCluster(cluster.getId());

    cluster = createActiveCluster();
    clusterService.requestClusterDelete(cluster.getId(), account, opRequest);
    testSensitiveFieldsAdded(cluster, sensitiveFields);
    clusterStore.deleteCluster(cluster.getId());

    cluster = createActiveCluster();
    clusterService.requestServiceRuntimeAction(cluster.getId(), account, ClusterAction.RESTART_SERVICES,
                                               service1.getName(), opRequest);
    testSensitiveFieldsAdded(cluster, sensitiveFields);
    clusterStore.deleteCluster(cluster.getId());
  }

  @Test(expected = MissingFieldsException.class)
  public void testMissingRequestThrowsException() throws Exception {
    Cluster cluster = createActiveCluster();
    clusterService.requestClusterDelete(cluster.getId(), cluster.getAccount(), null);
  }

  // test that sensitive user fields were added to the credential store
  private void testSensitiveFieldsAdded(Cluster cluster, Map<String, Object> sensitiveFields) throws Exception {
    // nonsensitive fields should be everything currently in the provider before we get the updated cluster
    Map<String, Object> expectedNonsensitiveFields = cluster.getProvider().getProvisionerFields();
    // get the updated cluster
    cluster = clusterStore.getCluster(cluster.getId());
    // nonsensitive fields should be everything currently in the provider plus the nonsensitive user fields
    // given in the request
    Assert.assertEquals(expectedNonsensitiveFields, cluster.getProvider().getProvisionerFields());
    Assert.assertEquals(sensitiveFields, credentialStore.get(account.getTenantId(), cluster.getId()));
    credentialStore.wipe();
  }

  @Test
  public void testAddServices() throws Exception {
    Cluster cluster = createActiveCluster();
    // add required sensitive user field
    Map<String, Object> sensitiveFields = Maps.newHashMap();
    sensitiveFields.put("key", "keycontents");
    AddServicesRequest addServicesRequest =
      new AddServicesRequest(sensitiveFields, ImmutableSet.of(service2.getName()));
    clusterService.requestAddServices(cluster.getId(), account, addServicesRequest);

    // nonsensitive fields should be everything currently in the provider before we get the updated cluster
    Map<String, Object> expectedNonsensitiveFields = cluster.getProvider().getProvisionerFields();
    // get the updated cluster
    cluster = clusterStore.getCluster(cluster.getId());
    // nonsensitive fields should be everything currently in the provider plus the nonsensitive user fields
    // given in the request
    Assert.assertEquals(expectedNonsensitiveFields, cluster.getProvider().getProvisionerFields());
    Assert.assertEquals(sensitiveFields, credentialStore.get(account.getTenantId(), cluster.getId()));
  }

  @Test
  public void testUsesExistingCredentials() throws Exception {
    Cluster cluster = createActiveCluster();
    // add required sensitive user field
    Map<String, Object> sensitiveFields = Maps.newHashMap();
    sensitiveFields.put("key", "keycontents");
    credentialStore.set(account.getTenantId(), cluster.getId(), sensitiveFields);

    // request doesn't contain the required key field, but it should be picked up from the credential store
    // so this should go through without throwing an exception.
    AddServicesRequest addServicesRequest = new AddServicesRequest(null, ImmutableSet.of(service2.getName()));
    clusterService.requestAddServices(cluster.getId(), account, addServicesRequest);
  }

  private Cluster createActiveCluster() throws IOException, IllegalAccessException {
    // Simulates an active cluster. The existing cluster will have
    // some user fields already specified from the initial cluster create operation, but another user field will
    // not exist because it is a sensitive field and was thus never persisted.
    // create a provider that already has the 'keyname' user field specified.
    Provider provider1 = Provider.builder()
      .setProviderType(providerType.getName())
      .setProvisionerFields(ImmutableMap.<String, Object>of(
        "region", "iad",
        "url", "http://abc.com/api",
        "keyname", "mykey"))
      .setName("provider")
      .build();
    // write the cluster to the store
    String clusterId = "123";
    Cluster cluster = Cluster.builder()
      .setName("cluster1")
      .setID(clusterId)
      .setProvider(provider1)
      .setClusterTemplate(basicTemplate)
      .setServices(ImmutableSet.of(service1.getName()))
      .setNodes(ImmutableSet.of("node1"))
      .setAccount(account)
      .setStatus(Cluster.Status.ACTIVE)
      .setLatestJobID(new JobId(clusterId, 1).getId())
      .build();
    clusterStore.writeCluster(cluster);
    return cluster;
  }
}
