/*
 * Copyright © 2012-2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.cask.coopr.spec.plugin;

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
