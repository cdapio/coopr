package com.continuuity.loom.admin;

import org.junit.Test;

/**
 *
 */
public class ResourceTypeSpecificationTest {

  @Test(expected = IllegalArgumentException.class)
  public void testArchivePermissionsErrors() {
    new ResourceTypeSpecification(ResourceTypeFormat.ARCHIVE, "777");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFilePermissions() {
    new ResourceTypeSpecification(ResourceTypeFormat.FILE, "rwx-r-xr-x");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFilePermissionsLength() {
    new ResourceTypeSpecification(ResourceTypeFormat.FILE, "77");
  }

  @Test
  public void testValidFilePermissions() {
    new ResourceTypeSpecification(ResourceTypeFormat.FILE, "0777");
    new ResourceTypeSpecification(ResourceTypeFormat.FILE, "777");
  }
}
