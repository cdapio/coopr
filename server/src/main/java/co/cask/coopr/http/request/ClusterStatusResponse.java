package co.cask.coopr.http.request;

import co.cask.coopr.cluster.Cluster;
import co.cask.coopr.scheduler.ClusterAction;
import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.scheduler.task.ClusterTask;
import com.google.common.base.Objects;

import java.util.Map;

/**
 * The response to a cluster status call.
 */
public class ClusterStatusResponse {
  private final String clusterid;
  private final int stepstotal;
  private final int stepscompleted;
  private final Cluster.Status status;
  private final ClusterJob.Status actionstatus;
  private final ClusterAction action;

  public ClusterStatusResponse(Cluster cluster, ClusterJob job) {
    this.clusterid = cluster.getId();
    this.status = cluster.getStatus();
    this.actionstatus = job.getJobStatus();
    this.action = job.getClusterAction();
    Map<String, ClusterTask.Status> taskStatus = job.getTaskStatus();

    int completedTasks = 0;
    for (Map.Entry<String, ClusterTask.Status> entry : taskStatus.entrySet()) {
      if (entry.getValue().equals(ClusterTask.Status.COMPLETE)) {
        completedTasks++;
      }
    }
    this.stepscompleted = completedTasks;
    this.stepstotal = taskStatus.size();
  }

  public String getClusterid() {
    return clusterid;
  }

  public int getStepstotal() {
    return stepstotal;
  }

  public int getStepscompleted() {
    return stepscompleted;
  }

  public Cluster.Status getStatus() {
    return status;
  }

  public ClusterJob.Status getActionstatus() {
    return actionstatus;
  }

  public ClusterAction getAction() {
    return action;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ClusterStatusResponse that = (ClusterStatusResponse) o;

    return Objects.equal(clusterid, that.clusterid) &&
      stepscompleted == that.stepscompleted &&
      stepstotal == that.stepstotal &&
      status == that.status &&
      action == that.action &&
      actionstatus == that.actionstatus;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(clusterid, stepstotal, stepscompleted, status, actionstatus, action);
  }
}
