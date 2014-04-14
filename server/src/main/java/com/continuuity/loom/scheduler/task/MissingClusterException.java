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
package com.continuuity.loom.scheduler.task;

/**
 * Thrown to indicate that there was a problem creating a cluster job or task.
 */
public class MissingClusterException extends Exception {

  /**
   * New exception with error message.
   * @param message the error message
   */
  public MissingClusterException(String message) {
    super(message);
  }

  public MissingClusterException(String message, Throwable cause) {
    super(message, cause);
  }

  public MissingClusterException(Throwable cause) {
    super(cause);
  }
}
