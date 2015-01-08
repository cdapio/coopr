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
package co.cask.coopr.spec.template;

/**
 * Template exception can be caused if template doe's not pass validation.
 * For example if template after merge does not has dns suffix.
 */
public class TemplateValidationException extends Exception {
  /**
   * New exception with error message.
   * @param message the error message
   */
  public TemplateValidationException(String message) {
    super(message);
  }

  public TemplateValidationException(String message, Throwable cause) {
    super(message, cause);
  }

  public TemplateValidationException(Throwable cause) {
    super(cause);
  }
}
