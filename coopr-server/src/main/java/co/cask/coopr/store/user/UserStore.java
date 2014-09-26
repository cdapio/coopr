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
package co.cask.coopr.store.user;

import co.cask.coopr.account.Account;
import com.google.common.util.concurrent.Service;

import java.io.IOException;
import java.util.Map;

/**
 * Persistent store for user accounts and their associated profiles.
 */
public interface UserStore extends Service {

  /**
   * Get the profile for a given account.
   *
   * @param account account to get the profile for
   * @return profile of the account, or null if none exists
   * @throws IOException if there was an issue getting the profile
   */
  Map<String, Object> getProfile(Account account) throws IOException;

  /**
   * Write the profile for a given account.
   *
   * @param account account to write the profile for
   * @param profile account profile to write
   * @throws IOException if there was an issue writing the profile
   */
  void writeProfile(Account account, Map<String, Object> profile) throws IOException;

  /**
   * Delete the profile for an account.
   *
   * @param account account whose profile should be deleted
   * @throws IOException if there was an issue deleting the profile
   */
  void deleteProfile(Account account) throws IOException;
}
