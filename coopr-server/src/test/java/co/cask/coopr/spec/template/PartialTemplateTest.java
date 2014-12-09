package co.cask.coopr.spec.template;

import co.cask.coopr.BaseTest;
import co.cask.coopr.account.Account;
import co.cask.coopr.common.conf.Constants;
import co.cask.coopr.provisioner.Provisioner;
import co.cask.coopr.provisioner.TenantProvisionerService;
import co.cask.coopr.spec.Tenant;
import co.cask.coopr.spec.TenantSpecification;
import co.cask.coopr.store.entity.EntityStoreView;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.beanutils.expression.Resolver;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class PartialTemplateTest extends BaseTest {

  private static ClusterTemplate insecureTemplate;
  private static ClusterTemplate secureTemplate;
  private static ClusterTemplate distributedTemplate;
  private static PartialTemplate sensuPartial;
  private static PartialTemplate ldapPartial;

  private static EntityStoreView entityStoreView;

  private Set<String> immutables = Sets.newHashSet();

  @Override
  protected boolean shouldClearDataBetweenTests() {
    return false;
  }

  @BeforeClass
  public static void setupClusterServiceTests() throws Exception {
    gson = injector.getInstance(Gson.class);
    TenantProvisionerService tenantProvisionerService = injector.getInstance(TenantProvisionerService.class);
    // setup data
    tenantProvisionerService.writeProvisioner(new Provisioner("p1", "host", 50056, 100, null, null));
    tenantProvisionerService.writeTenantSpecification(new TenantSpecification("tenantX", 10, 1, 10));
    Tenant tenant = tenantStore.getTenantByName("tenantX");
    entityStoreView = entityStoreService.getView(new Account(Constants.ADMIN_USER, tenant.getId()));
  }

  @Test
  public void testTemplateLifecycle() throws Exception {
    parse();
    persist();
    persistentTemplatesConsistence();
    resolveTemplateTest();
    deleteTemplate();
  }

  public void parse() throws Exception {
    //load json templates
    ClassLoader classLoader = PartialTemplateTest.class.getClassLoader();
    InputStream insecureIn = classLoader.getResourceAsStream("partials/cdap-distributed­insecure.json");
    InputStream distributedIn = classLoader.getResourceAsStream("partials/cdap­distributed.json");
    InputStream secureIn = classLoader.getResourceAsStream("partials/cdap­distributed-secure­hadoop.json");
    InputStream sensuIn = classLoader.getResourceAsStream("partials/sensu-partial.json");
    InputStream ldapIn = classLoader.getResourceAsStream("partials/ldap-partial.json");

    insecureTemplate = gson.fromJson(IOUtils.toString(insecureIn), ClusterTemplate.class);
    distributedTemplate = gson.fromJson(IOUtils.toString(distributedIn), ClusterTemplate.class);
    secureTemplate = gson.fromJson(IOUtils.toString(secureIn), ClusterTemplate.class);
    sensuPartial = gson.fromJson(IOUtils.toString(sensuIn), PartialTemplate.class);
    ldapPartial = gson.fromJson(IOUtils.toString(ldapIn), PartialTemplate.class);

    Assert.assertNotNull(insecureTemplate);
    Assert.assertNotNull(distributedTemplate);
    Assert.assertNotNull(secureTemplate);
    Assert.assertNotNull(sensuPartial);
    Assert.assertNotNull(ldapPartial);
  }

  public void persist() throws Exception {

    int beforePartialsCount = entityStoreView.getAllPartialTemplates().size();
    int beforeClusterTemplatesCount = entityStoreView.getAllClusterTemplates().size();

    entityStoreView.writeClusterTemplate(insecureTemplate);
    entityStoreView.writeClusterTemplate(distributedTemplate);
    entityStoreView.writeClusterTemplate(secureTemplate);

    entityStoreView.writePartialTemplate(sensuPartial);
    entityStoreView.writePartialTemplate(ldapPartial);

    int afterPartialsCount = entityStoreView.getAllPartialTemplates().size();
    int afterClusterTemplatesCount = entityStoreView.getAllClusterTemplates().size();

    Assert.assertEquals(afterPartialsCount, beforePartialsCount + 2);
    Assert.assertEquals(afterClusterTemplatesCount, beforeClusterTemplatesCount + 3);
  }

  public void persistentTemplatesConsistence() throws Exception {
    PartialTemplate ldapInternal = entityStoreView.getPartialTemplate("LDAPInternal");
    PartialTemplate sensuInternal = entityStoreView.getPartialTemplate("sensuInternal");

    ClusterTemplate cdapDistributedSecureHadoop = entityStoreView.getClusterTemplate("cdapDistributedSecureHadoop");
    ClusterTemplate cdapDistributedInsecure = entityStoreView.getClusterTemplate("cdapDistributedInsecure");
    ClusterTemplate cdapDistributed = entityStoreView.getClusterTemplate("cdapDistributed");

    Assert.assertNotNull(ldapInternal);
    Assert.assertNotNull(sensuInternal);

    Assert.assertNotNull(cdapDistributedSecureHadoop);
    Assert.assertNotNull(cdapDistributedInsecure);
    Assert.assertNotNull(cdapDistributed);

    Assert.assertEquals("Configure Example, Inc. LDAP services", ldapInternal.getDescription());
    Assert.assertEquals(true, sensuInternal.isImmutable());
    Assert.assertEquals("ldap­internal", ldapInternal.clusterDefaults.getServices().iterator().next());
    Assert.assertEquals("ldap­internal", ldapInternal.compatibilities.getServices().iterator().next());
    Assert.assertNotNull(ldapInternal.clusterDefaults.getConfig().get("ldap"));
    Assert.assertEquals("ldap.wrong.com", ldapInternal.clusterDefaults.getConfig().get("ldap")
      .getAsJsonObject().get("endpoint").getAsString());

    Assert.assertEquals("Cask DAP (CDAP) with Security and Secure Hadoop cluster with single master",
                        cdapDistributedSecureHadoop.getDescription());
    Assert.assertNotNull(cdapDistributedSecureHadoop.getParent());
    Assert.assertEquals("cdapDistributed", cdapDistributedSecureHadoop.getParent().getName());
    Assert.assertNotNull(cdapDistributedSecureHadoop.getIncludes());
    Assert.assertEquals("LDAPInternal", cdapDistributedSecureHadoop.getIncludes().iterator().next().getName());
    Assert.assertEquals(3, cdapDistributedSecureHadoop.clusterDefaults.getServices().size());
    Assert.assertNotNull(cdapDistributedSecureHadoop.getClusterDefaults().getConfig().get("hive"));
    Assert.assertEquals("kerberos­client",
                        cdapDistributedSecureHadoop.getCompatibilities().getServices().iterator().next());
  }

  public void deleteTemplate() throws Exception {
    int beforePartialsCount = entityStoreView.getAllPartialTemplates().size();
    int beforeClusterTemplatesCount = entityStoreView.getAllClusterTemplates().size();

    entityStoreView.deleteClusterTemplate(insecureTemplate.getName());
    entityStoreView.deletePartialTemplate(sensuPartial.getName());

    int afterPartialsCount = entityStoreView.getAllPartialTemplates().size();
    int afterClusterTemplatesCount = entityStoreView.getAllClusterTemplates().size();

    Assert.assertEquals(afterPartialsCount, beforePartialsCount - 1);
    Assert.assertEquals(afterClusterTemplatesCount, beforeClusterTemplatesCount - 1);
  }

  public void resolveTemplateTest() throws Exception {
    immutables.clear();
    resolveParentTemplate(secureTemplate);
    secureTemplate.getParent();
  }

  private ClusterTemplate resolveParentTemplate(ClusterTemplate clusterTemplate) throws Exception {
    ClusterTemplate dummyParent = clusterTemplate.getParent();
    if (dummyParent != null) {
      ClusterTemplate persistedParent = entityStoreView.getClusterTemplate(dummyParent.getName());
      if (persistedParent == null) {
        throw new Exception(dummyParent.getName() + " parent template not found.");
      }
      resolveParentTemplate(persistedParent);

      //merge all from parent to child
      copyFullProps(clusterTemplate, persistedParent);
    }

    resolvePartialIncludes(clusterTemplate);
    //check overrides in child template
    checkImmutability(clusterTemplate.getClusterDefaults().getConfig(), false);

    return clusterTemplate;
  }


  private ClusterTemplate resolvePartialIncludes(ClusterTemplate clusterTemplate) throws Exception {
    Set<PartialTemplate> partialTemplates = clusterTemplate.getIncludes();
    if (partialTemplates != null) {
      for (PartialTemplate dummyPartial : partialTemplates) {
        PartialTemplate persistedPartial = entityStoreView.getPartialTemplate(dummyPartial.getName());
        if (persistedPartial == null) {
          throw new Exception(dummyPartial.getName() + " partial not found.");
        }
        copyMainProps(clusterTemplate, persistedPartial);

        if (persistedPartial.isImmutable()) {
          JsonObject config = persistedPartial.getClusterDefaults().getConfig();
          //check overrides in partial template
          checkImmutability(config, true);
          for (Map.Entry<String, JsonElement> configItem : config.entrySet()) {
            immutables.add(configItem.getKey());
          }
        }
      }
    }
    return clusterTemplate;
  }

  //for partial includes
  private void copyMainProps(AbstractTemplate dest, AbstractTemplate from) throws Exception {
    //merge defaults
    JsonObject defaultsConfig = from.getClusterDefaults().getConfig();
    mergeSet(dest.getClusterDefaults().getServices(), from.getClusterDefaults().getServices());

    //merge defaults config
    for (Map.Entry<String, JsonElement> configItem : defaultsConfig.entrySet()) {
      dest.getClusterDefaults().getConfig().add(configItem.getKey(), configItem.getValue());
    }
    //merge compatibilities services
    mergeSet(dest.getCompatibilities().getServices(), from.getCompatibilities().getServices());
  }

  private void checkImmutability(JsonObject config, boolean isPartial) throws Exception {
      for (String immutable : immutables) {
        if (config.has(immutable)) {
          throw new Exception(immutable + " can't be overridden due immutability.");
        }
      }
  }

  //for parent extension
  private void copyFullProps(AbstractTemplate dest, AbstractTemplate from) throws Exception {
    copyMainProps(dest, from);

    //merge defaults options
    BeanUtils.copyProperties(dest.getClusterDefaults(), from.getClusterDefaults());

    //merge compatibilities services
    mergeSet(dest.getCompatibilities().getServices(), from.getCompatibilities().getServices());
    BeanUtils.copyProperties(dest.getCompatibilities(), from.getCompatibilities());

    //merge constraints
    mergeMap(dest.getConstraints().getServiceConstraints(), from.getConstraints().getServiceConstraints());
    mergeSet(dest.getConstraints().getLayoutConstraint().getServicesThatMustCoexist(),
             from.getConstraints().getLayoutConstraint().getServicesThatMustCoexist());
    mergeSet(dest.getConstraints().getLayoutConstraint().getServicesThatMustNotCoexist(),
             from.getConstraints().getLayoutConstraint().getServicesThatMustNotCoexist());
    BeanUtils.copyProperties(dest.getConstraints().getSizeConstraint(), from.getConstraints().getSizeConstraint());

    //merge compatibilities
    mergeSet(dest.getCompatibilities().getHardwaretypes(), from.getCompatibilities().getHardwaretypes());
    mergeSet(dest.getCompatibilities().getImagetypes(), from.getCompatibilities().getImagetypes());
//    dest.getCompatibilities().getServices().addAll(from.getCompatibilities().getServices());
    BeanUtils.copyProperties(dest.getAdministration().getLeaseDuration(), from.getAdministration().getLeaseDuration());
  }

  @SuppressWarnings("unchecked")
  private void mergeSet(Set dest, Set from) {
    if (dest != null && from != null) {
      dest.addAll(from);
    }
  }

  @SuppressWarnings("unchecked")
  private void mergeMap(Map dest, Map from) {
    if (dest != null && from != null) {
      dest.putAll(from);
    }
  }
}
