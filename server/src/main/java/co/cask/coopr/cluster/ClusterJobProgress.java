package co.cask.coopr.cluster;

import co.cask.coopr.scheduler.ClusterAction;
import co.cask.coopr.scheduler.task.ClusterJob;
import co.cask.coopr.scheduler.task.ClusterTask;
import com.google.common.base.Objects;

/**
 * Summary of the progress of a {@link ClusterJob}.
 */
public class ClusterJobProgress {
  private final ClusterAction action;
  private final ClusterJob.Status actionstatus;
  private final int stepstotal;
  private final int stepscompleted;

  public ClusterJobProgress(ClusterJob job) {
    this.action = job.getClusterAction();
    this.actionstatus = job.getJobStatus();
    this.stepstotal = job.getTaskStatus().size();

    int completedTasks = 0;
    for (ClusterTask.Status taskStatus : job.getTaskStatus().values()) {
      if (taskStatus == ClusterTask.Status.COMPLETE) {
        completedTasks++;
      }
    }
    this.stepscompleted = completedTasks;
  }

  public ClusterAction getAction() {
    return action;
  }

  public ClusterJob.Status getActionstatus() {
    return actionstatus;
  }

  public int getStepstotal() {
    return stepstotal;
  }

  public int getStepscompleted() {
    return stepscompleted;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ClusterJobProgress that = (ClusterJobProgress) o;

    return action == that.action &&
      actionstatus == that.actionstatus &&
      stepscompleted == that.stepscompleted &&
      stepstotal == that.stepstotal;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(action, actionstatus, stepscompleted, stepstotal);
  }
}
