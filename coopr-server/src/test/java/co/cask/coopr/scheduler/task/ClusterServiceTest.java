package co.cask.coopr.scheduler.task;

import co.cask.coopr.BaseTest;
import co.cask.coopr.Entities;
import co.cask.coopr.account.Account;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.http.request.AddServicesRequest;
import co.cask.coopr.http.request.ClusterCreateRequest;
import co.cask.coopr.http.request.ProviderOperationRequest;
import co.cask.coopr.provisioner.Provisioner;
import co.cask.coopr.provisioner.TenantProvisionerService;
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
import co.cask.coopr.spec.template.Administration;
import co.cask.coopr.spec.template.ClusterDefaults;
import co.cask.coopr.spec.template.ClusterTemplate;
import co.cask.coopr.spec.template.Compatibilities;
import co.cask.coopr.spec.template.Constraints;
import co.cask.coopr.store.entity.EntityStoreView;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
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
    providerType = new ProviderType(
      "providertype",
      null,
      "some description",
      ImmutableMap.of(
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
      ),
      null
    );
    entityStoreService.getView(Account.SUPERADMIN).writeProviderType(providerType);
    provider = new Provider("provider", "description", providerType.getName(),
                            ImmutableMap.<String, Object>of("region", "iad", "url", "http://abc.com/api"));
    entityStoreView.writeProvider(provider);
    entityStoreView.writeHardwareType(hardwareType);
    entityStoreView.writeImageType(imageType);
    entityStoreView.writeService(service1);
    entityStoreView.writeService(service2);
    basicTemplate = new ClusterTemplate(
      "basic",
      "description",
      new ClusterDefaults(
        ImmutableSet.of(service1.getName()),
        provider.getName(),
        hardwareType.getName(),
        imageType.getName(),
        null,
        null
      ),
      new Compatibilities(
        ImmutableSet.of(hardwareType.getName()),
        ImmutableSet.of(imageType.getName()),
        ImmutableSet.of(service1.getName(), service2.getName())
      ),
      Constraints.EMPTY_CONSTRAINTS,
      Administration.EMPTY_ADMINISTRATION
    );
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
    Map<String, String> providerFields = Maps.newHashMap();
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
    Cluster cluster = clusterService.createEmptyCluster(createRequest, account);
    clusterService.addAndValidateProviderFields(createRequest, cluster);
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
    Assert.assertEquals(expectedSensitiveFields, credentialStore.get(account.getTenantId(), cluster.getId()));
  }

  @Test(expected = MissingEntityException.class)
  public void testInvalidProviderClusterCreate() throws Exception {
    String name = "clusty";
    Map<String, String> providerFields = ImmutableMap.of("keyname", "ec2", "key", "keycontents");
    ClusterCreateRequest createRequest = ClusterCreateRequest.builder()
      .setName(name)
      .setClusterTemplateName(basicTemplate.getName())
      .setProviderName("not" + provider.getName())
      .setNumMachines(1)
      .setProviderFields(providerFields)
      .build();
    Cluster cluster = clusterService.createEmptyCluster(createRequest, account);
    clusterService.addAndValidateProviderFields(createRequest, cluster);
  }

  @Test(expected = MissingEntityException.class)
  public void testInvalidTemplateClusterCreate() throws Exception {
    String name = "clusty";
    Map<String, String> providerFields = ImmutableMap.of("keyname", "ec2", "key", "keycontents");
    ClusterCreateRequest createRequest = ClusterCreateRequest.builder()
      .setName(name)
      .setClusterTemplateName("not" + basicTemplate.getName())
      .setNumMachines(1)
      .setProviderFields(providerFields)
      .build();
    clusterService.createEmptyCluster(createRequest, account);
  }

  @Test
  public void testRequiredUserFields() throws Exception {
    Cluster cluster = createActiveCluster();

    // the "key" user field is required. Should throw an except if its not set.
    Map<String, String> providerFields = Maps.newHashMap();
    ProviderOperationRequest request = new ProviderOperationRequest(providerFields);
    List<Map<String, FieldSchema>> missingFields = clusterService.addAndValidateProviderFields(request, cluster);
    Assert.assertEquals(
      ImmutableList.of(ImmutableMap.of(
        "key", providerType.getParametersSpecification(ParameterType.USER).getFields().get("key"))),
      missingFields);

    // now try with required user field set
    providerFields.put("key", "keycontents");
    request = new ProviderOperationRequest(providerFields);
    clusterService.addAndValidateProviderFields(request, cluster);

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
    Map<String, String> sensitiveFields = Maps.newHashMap();
    sensitiveFields.put("key", "keycontents");
    ProviderOperationRequest request = new ProviderOperationRequest(sensitiveFields);

    Cluster cluster = createActiveCluster();
    // nonsensitive fields should be everything currently in the provider before we get the updated cluster
    Map<String, Object> expectedNonsensitiveFields = cluster.getProvider().getProvisionerFields();
    clusterService.addAndValidateProviderFields(request, cluster);

    // nonsensitive fields should be everything currently in the provider plus the nonsensitive user fields
    // given in the request
    Assert.assertEquals(expectedNonsensitiveFields, cluster.getProvider().getProvisionerFields());
    Assert.assertEquals(sensitiveFields, credentialStore.get(account.getTenantId(), cluster.getId()));
  }

  @Test
  public void testUsesExistingCredentials() throws Exception {
    Cluster cluster = createActiveCluster();
    // add required sensitive user field
    Map<String, String> sensitiveFields = Maps.newHashMap();
    sensitiveFields.put("key", "keycontents");
    credentialStore.set(account.getTenantId(), cluster.getId(), sensitiveFields);

    // request doesn't contain the required key field, but it should be picked up from the credential store
    // so this should go through without throwing an exception.
    ProviderOperationRequest request = new ProviderOperationRequest(null);
    clusterService.addAndValidateProviderFields(request, cluster);
  }

  private Cluster createActiveCluster() throws IOException, IllegalAccessException {
    // Simulates an active cluster. The existing cluster will have
    // some user fields already specified from the initial cluster create operation, but another user field will
    // not exist because it is a sensitive field and was thus never persisted.
    // create a provider that already has the 'keyname' user field specified.
    Provider provider1 = new Provider("provider", "description", providerType.getName(),
                                      ImmutableMap.<String, Object>of(
                                        "region", "iad",
                                        "url", "http://abc.com/api",
                                        "keyname", "mykey"));
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
