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

package co.cask.coopr.test.cli;

import co.cask.common.cli.CLI;
import co.cask.common.cli.Command;
import co.cask.common.cli.exception.InvalidCommandException;
import co.cask.coopr.account.Account;
import co.cask.coopr.codec.json.guice.CodecModules;
import co.cask.coopr.shell.CLIConfig;
import co.cask.coopr.shell.CLIMain;
import co.cask.coopr.shell.util.CLICodecModules;
import co.cask.coopr.test.client.ClientTest;
import com.google.gson.Gson;
import com.google.inject.Guice;
import org.junit.Assert;
import org.junit.Before;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;

/**
 * Abstract test for Coopr CLI
 */
public abstract class AbstractTest extends ClientTest {

  protected static final ByteArrayOutputStream OUTPUT_STREAM = new ByteArrayOutputStream();

  private static final PrintStream PRINT_STREAM = new PrintStream(OUTPUT_STREAM);
  private static final Gson GSON = Guice.createInjector(new CodecModules().getModule()).getInstance(Gson.class);
  private static final Gson CLI_GSON = Guice.createInjector(new CLICodecModules().getModule()).getInstance(Gson.class);

  public static CLIMain shell;
  public static CLI<Command> cli;

  public static void createCli(Account account)
    throws IOException, URISyntaxException, NoSuchFieldException, IllegalAccessException {
    CLIConfig config = new CLIConfig(HOSTNAME, port, account.getUserId(), account.getTenantId());
    shell = new CLIMain(config);

    Field field = shell.getClass().getDeclaredField("cli");
    field.setAccessible(true);

    cli = (CLI<Command>) field.get(shell);
  }

  @Before
  public void beforeTest() {
    OUTPUT_STREAM.reset();
  }

  public static void execute(String command) throws InvalidCommandException {
    cli.execute(command, PRINT_STREAM);
  }

  public static void checkCommandOutput(Object expectedOutput) throws UnsupportedEncodingException {
    Assert.assertEquals(CLI_GSON.toJson(expectedOutput), OUTPUT_STREAM.toString("UTF-8"));
  }

  public static void checkError() throws UnsupportedEncodingException {
    Assert.assertTrue(OUTPUT_STREAM.toString("UTF-8").startsWith("Error: "));
  }

  public static void checkEmptyOutput() throws UnsupportedEncodingException {
    Assert.assertEquals(0, OUTPUT_STREAM.size());
  }

  public static <T> T getObjectFromOutput(Class<T> type) throws UnsupportedEncodingException {
    return CLI_GSON.fromJson(OUTPUT_STREAM.toString("UTF-8"), type);
  }

  public static <T> Set<T> getSetFromOutput(Type listType) throws UnsupportedEncodingException {
    return CLI_GSON.fromJson(OUTPUT_STREAM.toString("UTF-8"), listType);
  }

  public static <T, V> Map<T, Set<V>> getMapFromOutput(Type mapType) throws UnsupportedEncodingException {
    return CLI_GSON.fromJson(OUTPUT_STREAM.toString("UTF-8"), mapType);
  }

  public static String getJsonFromObject(Object output) {
    return GSON.toJson(output);
  }
}
