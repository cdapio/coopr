/**
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
package co.cask.coopr.test.drivers;

import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.UnreachableBrowserException;

/**
 * Custom driver for avoiding exception.
 *
 */
  public class CustomPhantomJSDriver extends PhantomJSDriver {

    @Override
    public void get(String url) {
        int count = 0;
        int maxTries = 10;
        while (count < maxTries) {
            try {
                super.get(url);
                break;
            } catch (UnreachableBrowserException e) {
                count++;
            }
        }
        if (count == maxTries) {
            throw new UnreachableBrowserException(url);
        }
    }
}
