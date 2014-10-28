package co.cask.coopr.test.cli;


import co.cask.common.cli.exception.InvalidCommandException;
import co.cask.coopr.Entities;
import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.provisioner.plugin.ResourceStatus;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.coopr.test.Constants;
import co.cask.coopr.test.client.ClientTestEntities;
import com.google.gson.reflect.TypeToken;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PluginCommandsTest extends AbstractTest{

  @BeforeClass
  public static void beforeClass()
    throws URISyntaxException, NoSuchFieldException, IllegalAccessException, IOException {
    createCli(ADMIN_ACCOUNT);
  }

  @Test
  public void testListAllAutomators() throws InvalidCommandException, UnsupportedEncodingException {
    execute(Constants.LIST_ALL_AUTOMATORS_COMMAND);
    Set<AutomatorType> resultSet =  getSetFromOutput(new TypeToken<List<AutomatorType>>() { }.getType());
    Assert.assertEquals(3, resultSet.size());
    Assert.assertTrue(resultSet.contains(Entities.AutomatorTypeExample.CHEF));
    Assert.assertTrue(resultSet.contains(Entities.AutomatorTypeExample.PUPPET));
    Assert.assertTrue(resultSet.contains(Entities.AutomatorTypeExample.SHELL));
    Assert.assertEquals(resultSet, resultSet);
  }

  @Test
  public void testListAllProviders() throws InvalidCommandException, UnsupportedEncodingException {
    execute(Constants.LIST_ALL_PROVIDERS_COMMAND);
    Set<ProviderType> resultSet =  getSetFromOutput(new TypeToken<List<ProviderType>> () { }.getType());
    Assert.assertEquals(2, resultSet.size());
    Assert.assertTrue(resultSet.contains(Entities.ProviderTypeExample.JOYENT));
    Assert.assertTrue(resultSet.contains(Entities.ProviderTypeExample.RACKSPACE));
    Assert.assertEquals(resultSet, resultSet);
  }

  @Test
  public void testListAutomatorTypeResourcesCommand() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.LIST_ALL_AUTOMATOR_TYPE_RESOURSES_COMMAND,
                                   Constants.AUTOMATOR_TYPE_CHEF_NAME,
                                   ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                                   ResourceStatus.ACTIVE.toString());
    execute(command);
    Map<String, Set<ResourceMeta>> resultMap =  getMapFromOutput(new TypeToken<Map<String, Set<ResourceMeta>>>() { }.getType());
    Assert.assertEquals(3, resultMap.size());
    Assert.assertTrue(resultMap.containsKey(Entities.AutomatorTypeExample.CHEF));
    Assert.assertTrue(resultMap.containsKey(Entities.AutomatorTypeExample.PUPPET));
    Assert.assertTrue(resultMap.containsKey(Entities.AutomatorTypeExample.SHELL));
    Assert.assertEquals(resultMap, resultMap);
  }

  @Test
  public void testListAllTypeResourcesProviders() throws InvalidCommandException, UnsupportedEncodingException {
    execute(Constants.LIST_ALL_PROVIDERS_COMMAND);
    Map<String, Set<ResourceMeta>> resultMap =  getMapFromOutput(new TypeToken<List<AutomatorType>>() { }.getType());
    Assert.assertEquals(2, resultMap.size());
    Assert.assertTrue(resultMap.containsKey(Entities.ProviderTypeExample.JOYENT));
    Assert.assertTrue(resultMap.containsKey(Entities.ProviderTypeExample.RACKSPACE));
    Assert.assertEquals(resultMap, resultMap);
  }

}
