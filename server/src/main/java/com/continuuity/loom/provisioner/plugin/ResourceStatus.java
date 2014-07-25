/*
 * Copyright 2012-2014, Continuuity, Inc.
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
package com.continuuity.loom.provisioner.plugin;

/**
 * Status of a plugin resource. If a resource is inactive, it has been uploaded to the plugin store, but has not been
 * marked to get pushed to provisioners on the next sync. If a resource it active, it is being used on the provisioners.
 * If a resource is staged, it has been marked to be pushed to provisioners on the next sync, but has not yet been
 * pushed. If a resource is unstaged, it has been marked to be removed from provisioners on the next sync, but is still
 * being used on the provisioners.
 */
public enum ResourceStatus {
  INACTIVE,
  ACTIVE,
  STAGED,
  UNSTAGED;

  /**
   * Whether or not the resource is live, meaning it is the version of the resource being used on the provisioners.
   *
   * @return Whether or not the resource is live
   */
  public boolean isLive() {
    return this == ACTIVE || this == UNSTAGED;
  }


  /**
   * Whether or not the resource is slated to be live, meaning it is the version of the resource that will be
   * used on the provisioners after the next sync call. A resource that is live can also be slated to be live.
   *
   * @return Whether or not the resource is slated to be live
   */
  public boolean isSlatedToBeLive() {
    return this == STAGED || this == ACTIVE;
  }

  /**
   * Return the status that corresponds to the given values of being live and slated to be live.
   *
   * @param live Whether or not the resource is live
   * @param slated Whether or not the resource is slated to be live
   * @return Status corresponding to the given flags
   */
  public static ResourceStatus fromLiveFlags(boolean live, boolean slated) {
    if (live && slated) {
      return ACTIVE;
    } else if (live && !slated) {
      return UNSTAGED;
    } else if (!live && slated) {
      return STAGED;
    } else {
      return INACTIVE;
    }
  }
}
