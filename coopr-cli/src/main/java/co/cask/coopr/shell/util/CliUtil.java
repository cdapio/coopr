/*
 * Copyright © 2012-2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.cask.coopr.shell.util;

import co.cask.common.cli.Arguments;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Utility class for processing arguments.
 */
public class CliUtil {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  private static final String FILE_PREFIX = "file ";
  private static final String ARG_WRAPPER = "\"";
  private static final String JSON_WRAPPER = "'";

  /**
   * Checks whether the argument is in double quotes
   *
   * @param arg the argument
   */
  public static String checkArgument(String arg) {
    if (!(arg.startsWith(ARG_WRAPPER) && arg.endsWith(ARG_WRAPPER))) {
      throw new IllegalArgumentException("Arguments must be contained in double quotes");
    }
    return arg.substring(1, arg.length() - 1);
  }

  /**
   * Converts specified object to pretty Json
   *
   * @param output the object to be converted
   * @return the pretty Json String
   */
  public static String getPrettyJson(Object output) {
    return GSON.toJson(output);
  }

  /**
   * Converts Json argument with specified key to appropriate object
   *
   * @param arguments the arguments
   * @param argKey the argument key
   * @param type the type to which convert Json string
   * @param <T> a generic type
   * @return converted from Json object of type T
   * @throws IOException in case cannot read file
   */
  public static <T> T getObjectFromJson(Arguments arguments, String argKey, Class<T> type) throws IOException {
    if (arguments.hasArgument(argKey)) {
      String arg = arguments.get(argKey);
      Gson gson = new Gson();
      if (arg.startsWith(ARG_WRAPPER) && arg.endsWith(ARG_WRAPPER)) {
        arg = arg.substring(1, arg.length() - 1);
        if (arg.startsWith(FILE_PREFIX)) {
          String argFilePath = arg.substring(FILE_PREFIX.length());
          return gson.fromJson(new InputStreamReader(new FileInputStream(argFilePath), Charsets.UTF_8), type);
        } else {
          throw new IllegalArgumentException(String.format("Argument must starts with %s prefix", FILE_PREFIX));
        }
      } else {
        if (!(arg.startsWith(JSON_WRAPPER) && arg.endsWith(JSON_WRAPPER))) {
          throw new IllegalArgumentException("Json must be contained in single quotes");
        }
        return gson.fromJson(arg.substring(1, arg.length() - 1), type);
      }
    }
    return null;
  }
}
