package co.cask.coopr.http.request;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.Map;

/**
 * Request sent by provisioners for finishing a task that was performed perform.
 */
public class FinishTaskRequest {
  private final String workerId;
  private final String provisionerId;
  private final String tenantId;
  private final String taskId;
  private final String stdout;
  private final String stderr;
  private final String hostname;
  private final int status;
  private final Map<String, String> ipaddresses;
  private final JsonObject result;

  public FinishTaskRequest(String workerId, String provisionerId, String tenantId, String taskId,
                           String stdout, String stderr, Integer status, String hostname,
                           Map<String, String> ipaddresses, JsonObject result) {
    Preconditions.checkArgument(workerId != null && !workerId.isEmpty(), "workerId must be specified.");
    Preconditions.checkArgument(provisionerId != null && !provisionerId.isEmpty(), "provisionerId must be specified.");
    Preconditions.checkArgument(tenantId != null && !tenantId.isEmpty(), "tenantId must be specified.");
    Preconditions.checkArgument(status != null, "status must be specified.");
    this.workerId = workerId;
    this.provisionerId = provisionerId;
    this.tenantId = tenantId;
    this.taskId = taskId;
    this.stdout = stdout;
    this.stderr = stderr;
    this.status = status;
    this.hostname = hostname;
    this.ipaddresses = ipaddresses == null ? null : Collections.unmodifiableMap(ipaddresses);
    this.result = result == null ? new JsonObject() : result;
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

  public String getTaskId() {
    return taskId;
  }

  public String getStdout() {
    return stdout;
  }

  public String getStderr() {
    return stderr;
  }

  public int getStatus() {
    return status;
  }

  public String getHostname() {
    return hostname;
  }

  public Map<String, String> getIpaddresses() {
    return ipaddresses;
  }

  public JsonObject getResult() {
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FinishTaskRequest)) {
      return false;
    }

    FinishTaskRequest that = (FinishTaskRequest) o;

    return Objects.equal(workerId, that.workerId) &&
      Objects.equal(provisionerId, that.provisionerId) &&
      Objects.equal(tenantId, that.tenantId) &&
      Objects.equal(taskId, that.taskId) &&
      Objects.equal(stdout, that.stdout) &&
      Objects.equal(stderr, that.stderr) &&
      status == that.status &&
      Objects.equal(hostname, that.hostname) &&
      Objects.equal(ipaddresses, that.ipaddresses) &&
      Objects.equal(result, that.result);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(workerId, provisionerId, tenantId, taskId, stdout, stderr,
                            status, hostname, ipaddresses, result);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("workerId", workerId)
      .add("provisionerId", provisionerId)
      .add("tenantId", tenantId)
      .add("taskId", taskId)
      .add("stdout", stdout)
      .add("stderr", stderr)
      .add("status", status)
      .add("hostname", hostname)
      .add("ipaddresses", ipaddresses)
      .add("result", result)
      .toString();
  }
}
