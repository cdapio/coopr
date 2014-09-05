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
package co.cask.coopr.macro;

import co.cask.coopr.common.utils.ImmutablePair;
import co.cask.coopr.macro.eval.Evaluator;
import co.cask.coopr.macro.eval.Evaluators;

/**
 * Parses a macro expression. Syntax:
 *
 * <pre>
 *  <expression> ::= join(<inner>,<literal>) | <inner>
 *  <inner>      ::= map(<basic>,<literal>) | <basic>
 *  <basic>      ::= <type><service name> | <type><service name><instance>
 *  <type>       ::= host.service | ip.service | num.service | num.self.service
 *  <literal>    ::= "[^"]*" | '[^']'
 *  <instance>   ::= [\d+]
 * </pre>
 */
public class Parser {

  private final String input;

  private String macro = null;
  private String format = null;
  private String separator = null;
  private Integer instanceNum = null;

  /**
   * Construct a parser from a string. It is meant for single use.
   * @param input The input string to parse.
   */
  public Parser(String input) {
    this.input = input;
  }

  /**
   * Parse the input and return the resulting expression.
   * @throws SyntaxException in case of syntax errors.
   */
  public Expression parse() throws SyntaxException {
    macro = null;
    format = null;
    separator = null;
    int pos;
    if (input.startsWith("join(")) {
      pos = parseInner(5);
      pos = expect(pos, ',');
      pos = parseSeparator(pos);
      pos = expect(pos, ')');
    } else {
      pos = parseInner(0);
    }
    expectEnd(pos);
    if (macro == null) {
      throw new SyntaxException("A macro name must be present.");
    }
    Evaluator evaluator = Evaluators.evaluatorFor(macro, instanceNum);
    return new Expression(evaluator, format, separator);
  }

  /**
   * Parse an inner expression, that is a map or a basic, starting at pos and return the position reached.
   */
  private int parseInner(int pos) throws SyntaxException {
    if (input.indexOf("map(", pos) == pos) {
      pos = parseBasic(pos + 4);
      pos = expect(pos, ',');
      pos = parseFormat(pos);
      pos = expect(pos, ')');
    } else {
      pos = parseBasic(pos);
    }
    return pos;
  }

  /**
   * Parse a basic expression, starting at pos and return the position reached.
   */
  private int parseBasic(int pos) throws SyntaxException {
    int pos1;
    for (pos1 = pos; pos1 < input.length(); pos1++) {
      if (!isAllowed(input.charAt(pos1))) {
        break;
      }
    }
    if (pos == pos1) {
      throw new SyntaxException("A macro name must not be empty.");
    }
    macro = input.substring(pos, pos1);
    if (pos1 < input.length()) {
      pos1 = parseInstance(pos1);
    }
    return pos1;
  }

  /**
   * Parse an instance number, starting at pos and return the position reached.
   */
  private int parseInstance(int pos) throws SyntaxException {
    int pos1 = pos;
    if (input.charAt(pos) == '[') {
      pos1++;
      boolean endSeen = false;
      for (pos1 = pos + 1; pos1 < input.length(); pos1++) {
        char character = input.charAt(pos1);
        if (character == ']') {
          endSeen = true;
          break;
        }
        if (!Character.isDigit(character)) {
          throw new SyntaxException("A service number must consists of digits only.");
        }
      }
      if (pos1 == pos + 1) {
        throw new SyntaxException("No service number seen.");
      }
      if (!endSeen) {
        throw new SyntaxException("No ending bracket ']' seen.");
      }
      instanceNum = Integer.valueOf(input.substring(pos + 1, pos1));
      pos1++;
    }
    return pos1;
  }

  /**
   * Parse the separator of a join.
   */
  private int parseSeparator(int pos) throws SyntaxException {
    ImmutablePair<String, Integer> result = parseLiteral(pos);
    separator = result.getFirst();
    return result.getSecond();
  }

  /**
   * Parse the format string of a map.
   */
  private int parseFormat(int pos) throws SyntaxException {
    ImmutablePair<String, Integer> result = parseLiteral(pos);
    format = result.getFirst();
    return result.getSecond();
  }

  /**
   * Parse a literal. Must begin and end with ' or with ".
   */
  private ImmutablePair<String, Integer> parseLiteral(int pos) throws SyntaxException {
    if (pos >= input.length()) {
      throw new SyntaxException("Expected ' or \" at position " + pos);
    }
    char quote = input.charAt(pos);
    if (quote != '"' && quote != '\'') {
      throw new SyntaxException("Expected ' or \" at position " + pos);
    }
    int pos1 = pos + 1;
    while (pos1 < input.length() && input.charAt(pos1) != quote) {
      pos1++;
    }
    if (pos1 >= input.length() || input.charAt(pos1) != quote) {
      throw new SyntaxException("Expected " + quote + " at position " + pos);
    }
    return ImmutablePair.of(input.substring(pos + 1, pos1), pos1 + 1);
  }

  /**
   * Whether a character is allowed in a basic macro name.
   */
  private boolean isAllowed(char c) {
    return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.';
  }

  /**
   * Expect a character at a given position, throw if it is not there, return the new position.
   */
  private int expect(int pos, char c) throws SyntaxException {
    if (pos >= input.length() || input.charAt(pos) != c) {
      throw new SyntaxException("'" + c + "' expected at position " + pos);
    }
    return pos + 1;
  }

  /**
   * Verify that the end of the input is reached.
   */
  private void expectEnd(int pos) throws SyntaxException {
    if (pos < input.length()) {
      throw new SyntaxException("Extra characters after macro at position " + pos);
    }
  }
}
