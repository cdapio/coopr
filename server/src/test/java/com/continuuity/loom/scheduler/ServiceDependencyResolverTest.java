package com.continuuity.loom.scheduler;

import com.continuuity.loom.admin.ProvisionerAction;
import com.continuuity.loom.admin.Service;
import com.continuuity.loom.admin.ServiceAction;
import com.continuuity.loom.admin.ServiceDependencies;
import com.continuuity.loom.admin.ServiceStageDependencies;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class ServiceDependencyResolverTest {

  @Test
  public void testMinimizeDependencies() {
    Map<ProvisionerAction, ServiceAction> emptyActions = ImmutableMap.of();
    Service base =  new Service("base", "", ImmutableSet.<String>of(), emptyActions);
    Service s1 =  new Service("s1", "", ImmutableSet.<String>of("base"), emptyActions);
    Service s2 =  new Service("s2", "", ImmutableSet.<String>of("base", "s1"), emptyActions);
    Service s3 =  new Service("s3", "", ImmutableSet.<String>of("base", "s1", "s2"), emptyActions);
    Service s4 =  new Service("s4", "", ImmutableSet.<String>of("base"), emptyActions);
    Service s5 =  new Service("s5", "", ImmutableSet.<String>of("base", "s1", "s2", "s3", "s4"), emptyActions);
    Map<String, Service> serviceMap = Maps.newHashMap();
    serviceMap.put(base.getName(), base);
    serviceMap.put(s1.getName(), s1);
    serviceMap.put(s2.getName(), s2);
    serviceMap.put(s3.getName(), s3);
    serviceMap.put(s4.getName(), s4);
    serviceMap.put(s5.getName(), s5);
    SetMultimap<String, String> expected = HashMultimap.create();
    expected.put("s1", "base");
    expected.put("s2", "s1");
    expected.put("s3", "s2");
    expected.put("s4", "base");
    expected.put("s5", "s3");
    expected.put("s5", "s4");
    ServiceDependencyResolver resolver = new ServiceDependencyResolver(Actions.getInstance(), serviceMap);
    SetMultimap<String, String> actual = resolver.getRuntimeServiceDependencies();
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.get("base").isEmpty());
  }

  @Test
  public void testMinimizeDependenciesWithProvides() {
    Map<ProvisionerAction, ServiceAction> emptyActions = ImmutableMap.of();
    Service base =  new Service("base", "", ImmutableSet.<String>of(), emptyActions);
    Service s1 =  new Service("s1", "", ImmutableSet.<String>of("base"), emptyActions);
    Service s2v1 =  new Service("s2-v1", "",
                                new ServiceDependencies(
                                  ImmutableSet.<String>of("s2"),
                                  null, null,
                                  new ServiceStageDependencies(
                                    ImmutableSet.<String>of("base", "s1"),
                                    null
                                  )
                                ), emptyActions);
    Service s2v2 =  new Service("s2-v2", "",
                                new ServiceDependencies(
                                  ImmutableSet.<String>of("s2"),
                                  null, null,
                                  new ServiceStageDependencies(
                                    ImmutableSet.<String>of("base", "s1"),
                                    null
                                  )
                                ), emptyActions);
    Service s3 =  new Service("s3", "", ImmutableSet.<String>of("base", "s1", "s2"), emptyActions);
    Map<String, Service> serviceMap = Maps.newHashMap();
    serviceMap.put(base.getName(), base);
    serviceMap.put(s1.getName(), s1);
    serviceMap.put(s2v1.getName(), s2v1);
    serviceMap.put(s2v2.getName(), s2v2);
    serviceMap.put(s3.getName(), s3);
    SetMultimap<String, String> expected = HashMultimap.create();
    expected.put("s1", "base");
    expected.put("s2-v1", "s1");
    expected.put("s2-v2", "s1");
    expected.put("s3", "s2-v1");
    expected.put("s3", "s2-v2");
    ServiceDependencyResolver resolver = new ServiceDependencyResolver(Actions.getInstance(), serviceMap);
    SetMultimap<String, String> actual = resolver.getRuntimeServiceDependencies();
    Assert.assertEquals(expected, actual);
    Assert.assertTrue(actual.get("base").isEmpty());
  }

  /**
   *     |---> s2 ---|           |---> s6
   * s1--|           |---> s4 ---|
   *     |---> s3 ---|           |---> s7
   *           |
   *           |---------> s5
   */
  @Test
  public void testDependsOn() {
    Multimap<String, String> dependencies = HashMultimap.create();
    dependencies.put("s2", "s1");
    dependencies.put("s3", "s1");
    dependencies.put("s4", "s3");
    dependencies.put("s4", "s2");
    dependencies.put("s5", "s3");
    dependencies.put("s6", "s4");
    dependencies.put("s7", "s4");
    Map<String, Set<String>> serviceDeps = Maps.newHashMap();
    serviceDeps.put("s1", ImmutableSet.<String>of());
    serviceDeps.put("s2", ImmutableSet.<String>of("s1"));
    serviceDeps.put("s3", ImmutableSet.<String>of("s1"));
    serviceDeps.put("s4", ImmutableSet.<String>of("s1", "s2", "s3"));
    serviceDeps.put("s5", ImmutableSet.<String>of("s1", "s3"));
    serviceDeps.put("s6", ImmutableSet.<String>of("s1", "s2", "s3", "s4"));
    serviceDeps.put("s7", ImmutableSet.<String>of("s1", "s2", "s3", "s4"));

    for (String service1 : serviceDeps.keySet()) {
      for (String service2 : serviceDeps.keySet()) {
        Assert.assertEquals(serviceDeps.get(service1).contains(service2),
                            ServiceDependencyResolver.doesDependOn(service1, service2, dependencies));
      }
    }
  }

  /**
   *     |---> s2 ---|
   * s1--|           |---> s4
   *     |---> s3 ---|      |
   *                        |
   * s5 -----> s6 ----------|
   *
   */
  @Test
  public void testClusterRuntimeDependencies() {
    ServiceAction sAction = new ServiceAction(null, null, null);
    // s1 has initialize and start
    Service s1 =  new Service("s1", "", ImmutableSet.<String>of(),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INITIALIZE, sAction,
                                ProvisionerAction.START, sAction));
    // s2 has configure and initialize
    Service s2 =  new Service("s2", "", ImmutableSet.<String>of("s1"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.CONFIGURE, sAction,
                                ProvisionerAction.INITIALIZE, sAction));
    // s3 has start
    Service s3 =  new Service("s3", "", ImmutableSet.<String>of("s1"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.START, sAction));
    // s4 has initialize and start
    Service s4 =  new Service("s4", "", ImmutableSet.<String>of("s2", "s3", "s6"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INITIALIZE, sAction,
                                ProvisionerAction.START, sAction));
    Service s5 =  new Service("s5", "", ImmutableSet.<String>of(),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INITIALIZE, sAction,
                                ProvisionerAction.START, sAction));
    Service s6 =  new Service("s6", "", ImmutableSet.<String>of("s5"),
                              ImmutableMap.<ProvisionerAction, ServiceAction>of(
                                ProvisionerAction.INITIALIZE, sAction));

    Map<String, Service> serviceMap = Maps.newHashMap();
    serviceMap.put(s1.getName(), s1);
    serviceMap.put(s2.getName(), s2);
    serviceMap.put(s3.getName(), s3);
    serviceMap.put(s4.getName(), s4);
    serviceMap.put(s5.getName(), s5);
    serviceMap.put(s6.getName(), s6);

    SetMultimap<ActionOnService, ActionOnService> expected = HashMultimap.create();

    // s2 initialize depends on s1 start
    expected.put(new ActionOnService(ProvisionerAction.INITIALIZE, "s2"),
                 new ActionOnService(ProvisionerAction.START, "s1"));
    // s3 start depends on s1 start
    expected.put(new ActionOnService(ProvisionerAction.START, "s3"),
                 new ActionOnService(ProvisionerAction.START, "s1"));
    // s4 start depends on s1 start, s3 start, and s5 start.  but s3 depends on s1 so s3 start should not be here.
    expected.put(new ActionOnService(ProvisionerAction.START, "s4"),
                 new ActionOnService(ProvisionerAction.START, "s3"));
    expected.put(new ActionOnService(ProvisionerAction.START, "s4"),
                 new ActionOnService(ProvisionerAction.START, "s5"));
    // s4 initialize depends on s1 start, s3 start, and s5 start.  but s3 depends on s1 so s3 start should not be here.
    expected.put(new ActionOnService(ProvisionerAction.INITIALIZE, "s4"),
                 new ActionOnService(ProvisionerAction.START, "s3"));
    expected.put(new ActionOnService(ProvisionerAction.INITIALIZE, "s4"),
                 new ActionOnService(ProvisionerAction.START, "s5"));
    // s6 initialize depends on s5 start
    expected.put(new ActionOnService(ProvisionerAction.INITIALIZE, "s6"),
                 new ActionOnService(ProvisionerAction.START, "s5"));

    ServiceDependencyResolver resolver = new ServiceDependencyResolver(Actions.getInstance(), serviceMap);
    SetMultimap<ActionOnService, ActionOnService> actual = resolver.getClusterDependencies();
    Assert.assertEquals(expected, actual);
  }

  /**
   *     |---> s2 ---|
   * s1--|           |---> s4
   *     |---> s3 ---|      |
   *                        |
   * s5 -----> s6 ----------|
   *
   */
  @Test
  public void testClusterInstallDependencies() {
    ServiceAction sAction = new ServiceAction(null, null, null);
    Map<ProvisionerAction, ServiceAction> installActions = ImmutableMap.of(ProvisionerAction.INSTALL, sAction);
    Map<ProvisionerAction, ServiceAction> emptyActions = ImmutableMap.of();
    Service s1 =  new Service(
      "s1", "", new ServiceDependencies(null,
                                        null,
                                        new ServiceStageDependencies(
                                          ImmutableSet.<String>of(),
                                          ImmutableSet.<String>of()),
                                        null), installActions);
    Service s2 =  new Service(
      "s2", "", new ServiceDependencies(null,
                                        null,
                                        new ServiceStageDependencies(
                                          ImmutableSet.<String>of("s1"),
                                          ImmutableSet.<String>of()),
                                        null), emptyActions);
    Service s3 =  new Service(
      "s3", "", new ServiceDependencies(null,
                                        null,
                                        new ServiceStageDependencies(
                                          ImmutableSet.<String>of("s1"),
                                          ImmutableSet.<String>of()),
                                        null), installActions);
    Service s4 =  new Service(
      "s4", "", new ServiceDependencies(null,
                                        null,
                                        new ServiceStageDependencies(
                                          ImmutableSet.<String>of("s2", "s3", "s6"),
                                          ImmutableSet.<String>of()),
                                        null), installActions);
    Service s5 =  new Service(
      "s5", "", new ServiceDependencies(null,
                                        null,
                                        new ServiceStageDependencies(
                                          ImmutableSet.<String>of(),
                                          ImmutableSet.<String>of()),
                                        null), installActions);
    Service s6 =  new Service(
      "s6", "", new ServiceDependencies(null,
                                        null,
                                        new ServiceStageDependencies(
                                          ImmutableSet.<String>of("s5"),
                                          ImmutableSet.<String>of()),
                                        null), emptyActions);

    Map<String, Service> serviceMap = Maps.newHashMap();
    serviceMap.put(s1.getName(), s1);
    serviceMap.put(s2.getName(), s2);
    serviceMap.put(s3.getName(), s3);
    serviceMap.put(s4.getName(), s4);
    serviceMap.put(s5.getName(), s5);
    serviceMap.put(s6.getName(), s6);

    SetMultimap<ActionOnService, ActionOnService> expected = HashMultimap.create();

    // s2 depends on s1, but s2 has no install action so it is ignored.
    // s3 depends on s1
    expected.put(new ActionOnService(ProvisionerAction.INSTALL, "s3"),
                 new ActionOnService(ProvisionerAction.INSTALL, "s1"));

    // s4 depends on s3. s4 also depends on s2, but s2 has no install action so only s4 depends on s3 is needed.
    expected.put(new ActionOnService(ProvisionerAction.INSTALL, "s4"),
                 new ActionOnService(ProvisionerAction.INSTALL, "s3"));

    // s4 depends on s6, but s6 has no install action, so it gets transferred to s4 depends on s5.
    expected.put(new ActionOnService(ProvisionerAction.INSTALL, "s4"),
                 new ActionOnService(ProvisionerAction.INSTALL, "s5"));

    ServiceDependencyResolver resolver = new ServiceDependencyResolver(Actions.getInstance(), serviceMap);
    SetMultimap<ActionOnService, ActionOnService> actual = resolver.getClusterDependencies();
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testServiceUsesDependency() {
    ServiceAction sAction = new ServiceAction(null, null, null);
    Map<ProvisionerAction, ServiceAction> installActions = ImmutableMap.of(ProvisionerAction.INSTALL, sAction);
    Service s1 =  new Service(
      "s1", "", new ServiceDependencies(null,
                                        null,
                                        new ServiceStageDependencies(
                                          ImmutableSet.<String>of(),
                                          ImmutableSet.<String>of()),
                                        null), installActions);
    Service s2 =  new Service(
      "s2", "", new ServiceDependencies(null,
                                        null,
                                        new ServiceStageDependencies(
                                          ImmutableSet.<String>of(),
                                          ImmutableSet.<String>of("s1")),
                                        null), installActions);

    Map<String, Service> serviceMap = Maps.newHashMap();
    serviceMap.put(s1.getName(), s1);
    serviceMap.put(s2.getName(), s2);

    SetMultimap<ActionOnService, ActionOnService> expected = HashMultimap.create();

    // s2 depends on s1
    expected.put(new ActionOnService(ProvisionerAction.INSTALL, "s2"),
                 new ActionOnService(ProvisionerAction.INSTALL, "s1"));

    ServiceDependencyResolver resolver = new ServiceDependencyResolver(Actions.getInstance(), serviceMap);
    SetMultimap<ActionOnService, ActionOnService> actual = resolver.getClusterDependencies();
    Assert.assertEquals(expected, actual);
  }
}
