package co.cask.coopr.test.cli;


import co.cask.common.cli.exception.InvalidCommandException;
import co.cask.coopr.Entities;
import co.cask.coopr.provisioner.plugin.ResourceMeta;
import co.cask.coopr.provisioner.plugin.ResourceStatus;
import co.cask.coopr.spec.plugin.AutomatorType;
import co.cask.coopr.spec.plugin.ProviderType;
import co.cask.coopr.test.Constants;
import co.cask.coopr.test.client.ClientTestEntities;
import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

public class PluginCommandsTest extends AbstractTest{

  @BeforeClass
  public static void beforeClass()
    throws URISyntaxException, NoSuchFieldException, IllegalAccessException, IOException {
    createCli(SUPERADMIN_ACCOUNT);
  }

  @Test
  public void testListAllAutomators() throws InvalidCommandException, UnsupportedEncodingException {
    execute(Constants.LIST_ALL_AUTOMATORS_COMMAND);
    Set<AutomatorType> resultSet = getSetFromOutput(new TypeToken<Set<AutomatorType>>() {
    }.getType());
    Assert.assertEquals(3, resultSet.size());
    Assert.assertTrue(resultSet.contains(Entities.AutomatorTypeExample.CHEF));
    Assert.assertTrue(resultSet.contains(Entities.AutomatorTypeExample.PUPPET));
    Assert.assertTrue(resultSet.contains(Entities.AutomatorTypeExample.SHELL));
    Assert.assertEquals(resultSet, resultSet);
  }

  @Test
  public void testListAllProviders() throws InvalidCommandException, UnsupportedEncodingException {
    execute(Constants.LIST_ALL_PROVIDERS_COMMAND);
    Set<ProviderType> resultSet = getSetFromOutput(new TypeToken<Set<ProviderType>>() {
    }.getType());
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
    Map<String, Set<ResourceMeta>> resultMap =
      getMapFromOutput(new TypeToken<Map<String, Set<ResourceMeta>>>() {
      }.getType());
    Assert.assertEquals(2, resultMap.size());
    Assert.assertTrue(resultMap.containsKey("hadoop"));
    Assert.assertTrue(resultMap.containsKey("kafka"));
  }

  @Test
  public void testListAllTypeResourcesProviders() throws InvalidCommandException, UnsupportedEncodingException {
    String command = String.format(Constants.LIST_ALL_PROVIDER_TYPE_RESOURSES_COMMAND,
                                   Constants.PROVIDER_TYPE_JOYENT_NAME,
                                   ClientTestEntities.KEYS_RESOURCE_TYPE.getTypeName(),
                                   ResourceStatus.ACTIVE.toString());
    execute(command);
    Map<String, Set<ResourceMeta>> resultMap =
      getMapFromOutput(new TypeToken<Map<String, Set<ResourceMeta>>>() {
      }.getType());
    Assert.assertEquals(2, resultMap.size());
    Assert.assertTrue(resultMap.containsKey("dev"));
    Assert.assertTrue(resultMap.containsKey("research"));
  }

  @Test
  public void testGetAutomatorType() throws IOException, InvalidCommandException {
    execute(String.format(Constants.GET_AUTOMATOR_TYPE_COMMAND, Constants.AUTOMATOR_TYPE_CHEF_NAME));
    AutomatorType result = getObjectFromOutput(AutomatorType.class);
    Assert.assertEquals(result, Entities.AutomatorTypeExample.CHEF);
  }

  @Test
  public void testGetAutomatorTypeNotExist() throws IOException, InvalidCommandException {
    execute(String.format(Constants.GET_AUTOMATOR_TYPE_COMMAND, "test"));
    checkError();
  }

  @Test
  public void testGetProviderType() throws IOException, InvalidCommandException {
    execute(String.format(Constants.GET_PROVIDER_TYPE_COMMAND, Constants.PROVIDER_TYPE_JOYENT_NAME));
    ProviderType result = getObjectFromOutput(ProviderType.class);
    Assert.assertEquals(result, Entities.ProviderTypeExample.JOYENT);
  }

  @Test
  public void testGetProviderTypeNotExist() throws IOException, InvalidCommandException {
    execute(String.format(Constants.GET_PROVIDER_TYPE_COMMAND, "test"));
    checkError();
  }

  @Test
  public void testStageAutomatorTypeResourceCommand() throws IOException, InvalidCommandException {
    execute(String.format(Constants.STAGE_AUTOMATOR_TYPE_RESOURCE_COMMAND, Constants.AUTOMATOR_TYPE_CHEF_NAME,
                          ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                          ClientTestEntities.HADOOP_RESOURCE_META_V2.getName(),
                          String.valueOf(ClientTestEntities.HADOOP_RESOURCE_META_V2.getVersion())));
  }

  @Test
  public void testStageAutomatorTypeResourceUnknownVersionCommand() throws IOException, InvalidCommandException {
    execute(String.format(Constants.STAGE_AUTOMATOR_TYPE_RESOURCE_COMMAND, Constants.AUTOMATOR_TYPE_CHEF_NAME,
                          ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                          ClientTestEntities.HADOOP_RESOURCE_META_V2.getName(),
                          String.valueOf(12)));
    checkError();
  }

  @Test
  public void testStageProviderTypeResourceCommand() throws IOException, InvalidCommandException {
    execute(String.format(Constants.STAGE_PROVIDER_TYPE_RESOURCE_COMMAND, Constants.PROVIDER_TYPE_JOYENT_NAME,
                          ClientTestEntities.KEYS_RESOURCE_TYPE.getTypeName(),
                          ClientTestEntities.DEV_KEY_RESOURCE_META_V1.getName(),
                          String.valueOf(ClientTestEntities.DEV_KEY_RESOURCE_META_V1.getVersion())));
  }

  @Test
  public void testStageProviderTypeResourceUnknownVersionCommand() throws IOException, InvalidCommandException {
    execute(String.format(Constants.STAGE_AUTOMATOR_TYPE_RESOURCE_COMMAND, Constants.AUTOMATOR_TYPE_CHEF_NAME,
                          ClientTestEntities.KEYS_RESOURCE_TYPE.getTypeName(),
                          ClientTestEntities.DEV_KEY_RESOURCE_META_V1.getName(),
                          String.valueOf(12)));
    checkError();
  }

  @Test
  public void testRecallAutomatorTypeResourceCommand() throws IOException, InvalidCommandException {
    execute(String.format(Constants.RECALL_AUTOMATOR_TYPE_RESOURCE_COMMAND, Constants.AUTOMATOR_TYPE_CHEF_NAME,
                          ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                          ClientTestEntities.HADOOP_RESOURCE_META_V2.getName(),
                          String.valueOf(ClientTestEntities.HADOOP_RESOURCE_META_V2.getVersion())));
  }

  @Test
  public void testRecallAutomatorTypeResourceUnknownVersionCommand() throws IOException, InvalidCommandException {
    execute(String.format(Constants.RECALL_AUTOMATOR_TYPE_RESOURCE_COMMAND, Constants.AUTOMATOR_TYPE_CHEF_NAME,
                          ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                          ClientTestEntities.HADOOP_RESOURCE_META_V2.getName(),
                          String.valueOf(12)));
    checkError();
  }

  @Test
  public void testRecallProviderTypeResourceCommand() throws IOException, InvalidCommandException {
    execute(String.format(Constants.RECALL_PROVIDER_TYPE_RESOURCE_COMMAND, Constants.PROVIDER_TYPE_JOYENT_NAME,
                          ClientTestEntities.KEYS_RESOURCE_TYPE.getTypeName(),
                          ClientTestEntities.DEV_KEY_RESOURCE_META_V1.getName(),
                          String.valueOf(ClientTestEntities.DEV_KEY_RESOURCE_META_V1.getVersion())));
  }

  @Test
  public void testRecallProviderTypeResourceUnknownVersionCommand() throws IOException, InvalidCommandException {
    execute(String.format(Constants.RECALL_AUTOMATOR_TYPE_RESOURCE_COMMAND, Constants.AUTOMATOR_TYPE_CHEF_NAME,
                          ClientTestEntities.KEYS_RESOURCE_TYPE.getTypeName(),
                          ClientTestEntities.DEV_KEY_RESOURCE_META_V1.getName(),
                          String.valueOf(12)));
    checkError();
  }

  @Test
  public void testDeleteAutomatorTypeResourceCommand() throws IOException, InvalidCommandException {

    execute(String.format(Constants.LIST_ALL_AUTOMATOR_TYPE_RESOURSES_COMMAND, Constants.AUTOMATOR_TYPE_CHEF_NAME,
                          ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                          ResourceStatus.INACTIVE.toString()));

    Map<String, Set<ResourceMeta>> resultMap =
      getMapFromOutput(new TypeToken<Map<String, Set<ResourceMeta>>>() {
      }.getType());
    Assert.assertEquals(1, resultMap.size());
    Assert.assertEquals(Sets.newHashSet(ClientTestEntities.MYSQL_RESOURCE_META), resultMap.get("mysql"));

    OUTPUT_STREAM.reset();

    execute(String.format(Constants.DELETE_AUTOMATOR_TYPE_RESOURCE_COMMAND, Constants.AUTOMATOR_TYPE_CHEF_NAME,
                          ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                          ClientTestEntities.MYSQL_RESOURCE_META.getName(),
                          String.valueOf(ClientTestEntities.MYSQL_RESOURCE_META.getVersion())));

    execute(String.format(Constants.LIST_ALL_AUTOMATOR_TYPE_RESOURSES_COMMAND, Constants.AUTOMATOR_TYPE_CHEF_NAME,
                          ClientTestEntities.COOKBOOKS_RESOURCE_TYPE.getTypeName(),
                          ResourceStatus.INACTIVE.toString()));

    resultMap = getMapFromOutput(new TypeToken<Map<String, Set<ResourceMeta>>>() {
    }.getType());
    Assert.assertEquals(0, resultMap.size());
  }

  @Test
  public void testDeleteProviderTypeResourceCommand() throws IOException, InvalidCommandException {

    execute(String.format(Constants.LIST_ALL_PROVIDER_TYPE_RESOURSES_COMMAND, Constants.PROVIDER_TYPE_JOYENT_NAME,
                          ClientTestEntities.KEYS_RESOURCE_TYPE.getTypeName(),
                          ResourceStatus.INACTIVE));

    Map<String, Set<ResourceMeta>> resultMap =
      getMapFromOutput(new TypeToken<Map<String, Set<ResourceMeta>>>() {
      }.getType());
    Assert.assertEquals(1, resultMap.size());
    Assert.assertEquals(Sets.newHashSet(ClientTestEntities.VIEW_KEY_RESOURCE_META), resultMap.get("view"));

    OUTPUT_STREAM.reset();

    execute(String.format(Constants.DELETE_PROVIDER_TYPE_RESOURCE_COMMAND, Constants.PROVIDER_TYPE_JOYENT_NAME,
                          ClientTestEntities.KEYS_RESOURCE_TYPE.getTypeName(),
                          ClientTestEntities.VIEW_KEY_RESOURCE_META.getName(),
                          String.valueOf(ClientTestEntities.VIEW_KEY_RESOURCE_META.getVersion())));

    execute(String.format(Constants.LIST_ALL_PROVIDER_TYPE_RESOURSES_COMMAND, Constants.PROVIDER_TYPE_JOYENT_NAME,
                          ClientTestEntities.KEYS_RESOURCE_TYPE.getTypeName(),
                          ResourceStatus.INACTIVE.toString()));

    resultMap = getMapFromOutput(new TypeToken<Map<String, Set<ResourceMeta>>>() {
    }.getType());
    Assert.assertEquals(0, resultMap.size());


  }
}
