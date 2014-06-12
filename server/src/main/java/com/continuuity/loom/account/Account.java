package com.continuuity.loom.account;

import com.continuuity.loom.conf.Constants;
import com.google.common.base.Objects;
import com.google.gson.annotations.Expose;

/**
 * An Account represents all information about a user, such as the user id and tenant id.
 */
public final class Account {
  private final String userId;
  private final String tenantId;
  @Expose(serialize = false, deserialize = false)
  private final boolean isSystem;
  public static final Account SYSTEM_ACCOUNT = new Account("", "", true);

  // keep for Gson deserialization
  private Account() {
    this("", "", false);
  }

  public Account(String userId, String tenantId) {
    this(userId, tenantId, false);
  }

  private Account(String userId, String tenantId, boolean isSystem) {
    this.userId = userId;
    this.tenantId = tenantId;
    this.isSystem = isSystem;
  }

  public String getUserId() {
    return userId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public boolean isAdmin() {
    return Constants.ADMIN_USER.equals(userId);
  }

  public boolean isSuperadmin() {
    return Constants.ADMIN_USER.equals(userId) && Constants.SUPERADMIN_TENANT.equals(tenantId);
  }

  public boolean isSystem() {
    return isSystem;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Account)) {
      return false;
    }

    Account other = (Account) o;
    return Objects.equal(userId, other.userId) &&
      Objects.equal(tenantId, other.tenantId) &&
      Objects.equal(isSystem, other.isSystem);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(userId, tenantId, isSystem);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("userId", userId)
      .add("tenantId", tenantId)
      .add("isSystem", isSystem)
      .toString();
  }
}
