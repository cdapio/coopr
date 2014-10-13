package co.cask.coopr.shell.util;

import co.cask.common.cli.Arguments;
import co.cask.coopr.http.request.ClusterOperationRequest;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for processing arguments
 */
public class CliUtil {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  /**
   * Checks whether the argument is in double quotes
   *
   * @param arg the argument
   */
  public static void checkArgument(String arg) {
    checkArguments(Arrays.asList(arg));
  }

  /**
   * Checks whether each argument is in double quotes
   *
   * @param args the arguments
   */
  public static void checkArguments(List<String> args) {
    for (String arg : args) {
      if (!(arg.startsWith("\"") && arg.endsWith("\""))) {
        throw new IllegalArgumentException("Arguments must be contained in double quotes");
      }
    }
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
      if (arg.startsWith("file ")) {
        String argFilePath = arg.substring(5);
        String argument = Files.toString(new File(argFilePath), Charset.forName("UTF-8"));
        return gson.fromJson(argument, type);
      } else {
        if (!(arg.startsWith("'") && arg.endsWith("'"))) {
          throw new IllegalArgumentException("Json must be contained in single quotes");
        }
        return gson.fromJson(arg.substring(1, arg.length() - 1), type);
      }
    }
    return null;
  }
}
