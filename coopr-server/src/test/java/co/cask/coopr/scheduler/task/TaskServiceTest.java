package co.cask.coopr.scheduler.task;

import co.cask.coopr.BaseTest;
import co.cask.coopr.Entities;
import co.cask.coopr.account.Account;
import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.scheduler.ClusterAction;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

/**
 *
 */
public class TaskServiceTest extends BaseTest {
  private static TaskService taskService;

  @BeforeClass
  public static void setupTaskServiceTest() {
    taskService = injector.getInstance(TaskService.class);
  }

  @Test
  public void testOnlyDeleteFinishWipesCredentials() throws Exception {
    Account account = new Account("user", "tenant");
    String clusterId = "123";
    Cluster cluster = Cluster.builder()
      .setName("cluster1")
      .setID(clusterId)
      .setProvider(Entities.ProviderExample.RACKSPACE)
      .setClusterTemplate(Entities.ClusterTemplateExample.HDFS)
      .setAccount(account)
      .setStatus(Cluster.Status.ACTIVE)
      .build();

    // write credentials
    Map<String, Object> sensitiveFields = Maps.newHashMap();
    sensitiveFields.put("key", "keycontents");
    credentialStore.set(account.getTenantId(), clusterId, sensitiveFields);
    long jobNum = 1;

    for (ClusterAction action : ClusterAction.values()) {
      if (action == ClusterAction.CLUSTER_DELETE) {
        continue;
      }

      // every job except delete should leave the credentials alone
      JobId jobId = new JobId(clusterId, jobNum);
      ClusterJob job = new ClusterJob(jobId, action);
      cluster.setLatestJobId(jobId.getId());
      taskService.completeJob(job, cluster);
      jobNum++;
      Assert.assertEquals(sensitiveFields, credentialStore.get(account.getTenantId(), clusterId));
    }

    // delete job should wipe credentials
    JobId jobId = new JobId(clusterId, jobNum);
    ClusterJob job = new ClusterJob(jobId, ClusterAction.CLUSTER_DELETE);
    cluster.setLatestJobId(jobId.getId());
    taskService.completeJob(job, cluster);
    Assert.assertTrue(credentialStore.get(account.getTenantId(), clusterId).isEmpty());
  }
}
