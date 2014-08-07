package com.continuuity.loom.scheduler.task;

import com.continuuity.loom.admin.ServiceAction;

/**
 * Service in a {@link SchedulableTaskConfig}, containing the name of the service and the action to be performed.
 * Used purely for serialization into json.
 */
public class TaskServiceAction {
  private final String name;
  private final ServiceAction action;

  public TaskServiceAction(String name, ServiceAction action) {
    this.name = name;
    this.action = action;
  }

  public String getName() {
    return name;
  }

  public ServiceAction getAction() {
    return action;
  }
}
