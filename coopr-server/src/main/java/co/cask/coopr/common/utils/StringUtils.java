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
package co.cask.coopr.common.utils;

import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Utility for strings.
 */
public class StringUtils {
  private static final Pattern labelPattern = Pattern.compile("^[a-zA-Z0-9\\-]{0,62}[a-zA-Z0-9]$");

  /**
   * Strip all leading digits from a string. For example, "1234abc" becomes "abc". If the string is comprised
   * entirely of digits, the empty string is returned.
   *
   * @param str string to strip
   * @return string with leading digits stripped
   */
  public static String stripLeadingDigits(String str) {
    for (int i = 0; i < str.length(); i++) {
      if (!Character.isDigit(str.charAt(i))) {
        return str.substring(i);
      }
    }
    return "";
  }

  /**
   * Check if the given DNS suffix is valid by checking that each label is at least 1 and at most 63 characters
   * in length. Also check that each label has only characters, numbers, or '-', and that the entire suffix is
   * at most 255 - 63 - 1 characters, since it will be appended to a single label later on to create a hostname.
   * Also need to check that no label ends in a dash.
   *
   * @param suffix the suffix to check
   * @return true is it is valid and false if it is not
   */
  public static boolean isValidDNSSuffix(String suffix) throws IllegalArgumentException {
    if (suffix.length() > 191) {
      return false;
    }
    for (String label : Splitter.on('.').split(suffix)) {
      if (!labelPattern.matcher(label).matches()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns an arraylist of strings.
   * @param str the comma seperated string values
   * @return the arraylist of the comma seperated string values
   */
  public static String[] getStrings(String str) {
    Collection<String> values = getStringCollection(str);
    if (values.isEmpty()) {
      return null;
    }
    return values.toArray(new String[values.size()]);
  }

  /**
   * Returns a collection of strings.
   * @param str comma seperated string values
   * @return an <code>ArrayList</code> of string values
   */
  public static Collection<String> getStringCollection(String str) {
    List<String> values = new ArrayList<String>();
    if (str == null) {
      return values;
    }
    StringTokenizer tokenizer = new StringTokenizer (str, ",");
    values = new ArrayList<String>();
    while (tokenizer.hasMoreTokens()) {
      values.add(tokenizer.nextToken());
    }
    return values;
  }

  /**
   * Splits a comma separated value <code>String</code>, trimming leading and trailing whitespace on each value.
   * @param str a comma separated <String> with values
   * @return a <code>Collection</code> of <code>String</code> values
   */
  public static Collection<String> getTrimmedStringCollection(String str) {
    return new ArrayList<String>(
      Arrays.asList(getTrimmedStrings(str)));
  }

  /**
   * Splits a comma separated value <code>String</code>, trimming leading and trailing whitespace on each value.
   * @param str a comma separated <String> with values
   * @return an array of <code>String</code> values
   */
  public static String[] getTrimmedStrings(String str) {
    if (null == str || "".equals(str.trim())) {
      return EMPTY_STRING_ARRAY;
    }

    return str.trim().split("\\s*,\\s*");
  }

  public static final String[] EMPTY_STRING_ARRAY = {};


  /**
   * Given an array of strings, return a comma-separated list of its elements.
   * @param strs Array of strings
   * @return Empty string if strs.length is 0, comma separated list of strings
   * otherwise
   */

  public static String arrayToString(String[] strs) {
    if (strs.length == 0) { return ""; }
    StringBuilder sbuf = new StringBuilder();
    sbuf.append(strs[0]);
    for (int idx = 1; idx < strs.length; idx++) {
      sbuf.append(",");
      sbuf.append(strs[idx]);
    }
    return sbuf.toString();
  }

  /**
   * The traditional binary prefixes, kilo, mega, ..., exa,
   * which can be represented by a 64-bit integer.
   * TraditionalBinaryPrefix symbol are case insensitive.
   */
  public static enum TraditionalBinaryPrefix {
    KILO(1024),
    MEGA(KILO.value << 10),
    GIGA(MEGA.value << 10),
    TERA(GIGA.value << 10),
    PETA(TERA.value << 10),
    EXA(PETA.value << 10);

    public final long value;
    public final char symbol;

    TraditionalBinaryPrefix(long value) {
      this.value = value;
      this.symbol = toString().charAt(0);
    }

    /**
     * @return The TraditionalBinaryPrefix object corresponding to the symbol.
     */
    public static TraditionalBinaryPrefix valueOf(char symbol) {
      symbol = Character.toUpperCase(symbol);
      for (TraditionalBinaryPrefix prefix : TraditionalBinaryPrefix.values()) {
        if (symbol == prefix.symbol) {
          return prefix;
        }
      }
      throw new IllegalArgumentException("Unknown symbol '" + symbol + "'");
    }

    /**
     * Convert a string to long.
     * The input string is first be trimmed
     * and then it is parsed with traditional binary prefix.
     *
     * For example,
     * "-1230k" will be converted to -1230 * 1024 = -1259520;
     * "891g" will be converted to 891 * 1024^3 = 956703965184;
     *
     * @param s input string
     * @return a long value represented by the input string.
     */
    public static long string2long(String s) {
      s = s.trim();
      final int lastpos = s.length() - 1;
      final char lastchar = s.charAt(lastpos);
      if (Character.isDigit(lastchar)) {
        return Long.parseLong(s);
      } else {
        long prefix;
        try {
          prefix = TraditionalBinaryPrefix.valueOf(lastchar).value;
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException("Invalid size prefix '" + lastchar
                                               + "' in '" + s
                                               + "'. Allowed prefixes are k, m, g, t, p, e(case insensitive)");
        }
        long num = Long.parseLong(s.substring(0, lastpos));
        if (num > (Long.MAX_VALUE / prefix) || num < (Long.MIN_VALUE / prefix)) {
          throw new IllegalArgumentException(s + " does not fit in a Long");
        }
        return num * prefix;
      }
    }
  }
}
