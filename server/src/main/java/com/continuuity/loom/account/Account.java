package com.continuuity.loom.account;

import com.continuuity.loom.common.conf.Constants;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * An Account represents all information about a user, such as the user id and tenant id.
 */
public final class Account {
  public static final Account SUPERADMIN = new Account(Constants.ADMIN_USER, Constants.SUPERADMIN_TENANT);
  private final String userId;
  private final String tenantId;

  public Account(String userId, String tenantId) {
    Preconditions.checkArgument(userId != null && !userId.isEmpty(), "Account must have a user id.");
    Preconditions.checkArgument(tenantId != null && !tenantId.isEmpty(), "Account must have a tenant id.");
    this.userId = userId;
    this.tenantId = tenantId;
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
      Objects.equal(tenantId, other.tenantId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(userId, tenantId);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("userId", userId)
      .add("tenantId", tenantId)
      .toString();
  }
}
