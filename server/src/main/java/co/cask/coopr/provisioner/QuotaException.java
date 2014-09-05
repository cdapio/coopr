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

package co.cask.coopr.provisioner;

/**
 * Thrown to indicate that there was some quota violation.
 */
public class QuotaException extends Exception {

  /**
   * New exception with error message.
   * @param message the error message
   */
  public QuotaException(String message) {
    super(message);
  }

  public QuotaException(String message, Throwable cause) {
    super(message, cause);
  }

  public QuotaException(Throwable cause) {
    super(cause);
  }

}
