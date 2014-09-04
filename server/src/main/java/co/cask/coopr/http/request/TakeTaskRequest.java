package co.cask.coopr.http.request;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Request sent by provisioners for taking a task to perform.
 */
public class TakeTaskRequest {
  private final String workerId;
  private final String provisionerId;
  private final String tenantId;

  public TakeTaskRequest(String workerId, String provisionerId, String tenantId) {
    Preconditions.checkArgument(workerId != null && !workerId.isEmpty(), "workerId must be specified.");
    Preconditions.checkArgument(provisionerId != null && !provisionerId.isEmpty(), "provisionerId must be specified.");
    Preconditions.checkArgument(tenantId != null && !tenantId.isEmpty(), "tenantId must be specified.");
    this.workerId = workerId;
    this.provisionerId = provisionerId;
    this.tenantId = tenantId;
  }

  public String getWorkerId() {
    return workerId;
  }

  public String getProvisionerId() {
    return provisionerId;
  }

  public String getTenantId() {
    return tenantId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TakeTaskRequest)) {
      return false;
    }

    TakeTaskRequest that = (TakeTaskRequest) o;

    return Objects.equal(workerId, that.workerId) &&
      Objects.equal(provisionerId, that.provisionerId) &&
      Objects.equal(tenantId, that.tenantId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(workerId, provisionerId, tenantId);
  }
}
