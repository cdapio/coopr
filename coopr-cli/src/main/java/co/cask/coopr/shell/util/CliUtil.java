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
import co.cask.coopr.codec.json.guice.CodecModules;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.Socket;

/**
 * Utility class for processing arguments.
 */
public class CliUtil {

  private static final Logger LOG = LoggerFactory.getLogger(CliUtil.class);
  private static final String FILE_PREFIX = "file ";
  private static final Gson GSON = Guice.createInjector(new CLICodecModules().getModule()).getInstance(Gson.class);

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
      if (arg.startsWith(FILE_PREFIX)) {
        String argFilePath = arg.substring(FILE_PREFIX.length());
        Reader reader = null;
        try {
          reader = new InputStreamReader(new FileInputStream(argFilePath), Charsets.UTF_8);
          return GSON.fromJson(reader, type);
        } finally {
          if (reader != null) {
            try {
              reader.close();
            } catch (IOException e) {
              LOG.warn("Cannot close stream for file: {}", argFilePath, e);
            }
          }
        }
      }
      return GSON.fromJson(arg, type);
    }
    return null;
  }

  /**
   * Check if specified port on specified host is available.
   *
   * @param host the host
   * @param port the port
   * @return {@code true} if specified port on specified host is available, otherwise {@code false}
   */
  public static boolean isAvailable(String host, int port) {
    Socket socket = null;
    try {
      socket = new Socket(host, port);
      return true;
    } catch (Exception ignored) {
    } finally {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException e) {
          LOG.warn("Cannot close socket with host: {}, port: {}", host, port, e);
        }
      }
    }
    return false;
  }
}
