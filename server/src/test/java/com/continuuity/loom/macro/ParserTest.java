/*
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
package com.continuuity.loom.macro;

import org.junit.Assert;
import org.junit.Test;

import static com.continuuity.loom.macro.Expression.Type.HOST_OF_SERVICE;
import static com.continuuity.loom.macro.Expression.Type.IP_OF_SERVICE;
import static com.continuuity.loom.macro.Expression.Type.NUM_OF_SERVICE;
import static com.continuuity.loom.macro.Expression.Type.SELF_INSTANCE_OF_SERVICE;
import static org.junit.Assert.assertEquals;

/**
 * Tests parsing of macro expressions.
 */
public class ParserTest {

  @Test
  public void testParseOK() throws SyntaxException {
    assertEquals(new Expression(HOST_OF_SERVICE, "abc", null, null, null),
                 new Parser("host.service.abc").parse());
    assertEquals(new Expression(IP_OF_SERVICE, "namenode", null, null, null),
                 new Parser("ip.service.namenode").parse());
    assertEquals(new Expression(IP_OF_SERVICE, "a-b-c", null, null, null),
                 new Parser("ip.service.a-b-c").parse());
    assertEquals(new Expression(IP_OF_SERVICE, "a.b.c", null, null, null),
                 new Parser("ip.service.a.b.c").parse());
    assertEquals(new Expression(IP_OF_SERVICE, "a_b_c", null, null, null),
                 new Parser("ip.service.a_b_c").parse());

    assertEquals(new Expression(IP_OF_SERVICE, "nn", "$:80", null, null),
                 new Parser("map(ip.service.nn,'$:80')").parse());
    assertEquals(new Expression(IP_OF_SERVICE, "nn", "\"$\"", null, null),
                 new Parser("map(ip.service.nn,'\"$\"')").parse());
    assertEquals(new Expression(IP_OF_SERVICE, "nn", "$:80", null, null),
                 new Parser("map(ip.service.nn,\"$:80\")").parse());
    assertEquals(new Expression(IP_OF_SERVICE, "nn", "'$'", null, null)
      , new Parser("map(ip.service.nn,\"'$'\")").parse());

    assertEquals(new Expression(IP_OF_SERVICE, "nn", null, "-", null),
                 new Parser("join(ip.service.nn,'-')").parse());
    assertEquals(new Expression(IP_OF_SERVICE, "nn", null, "\n", null),
                 new Parser("join(ip.service.nn,\"\n\")").parse());

    assertEquals(new Expression(HOST_OF_SERVICE, "abc", "$:2181", ",", null),
                 new Parser("join(map(host.service.abc,'$:2181'),',')").parse());

    assertEquals(new Expression(HOST_OF_SERVICE, "abc", null, null, 1),
                 new Parser("host.service.abc[1]").parse());
    assertEquals(new Expression(IP_OF_SERVICE, "abc", null, null, 0),
                 new Parser("ip.service.abc[0]").parse());
    assertEquals(new Expression(SELF_INSTANCE_OF_SERVICE, "abc", null, null, null),
                 new Parser("instance.self.service.abc").parse());
    assertEquals(new Expression(NUM_OF_SERVICE, "abc", null, null, null),
                 new Parser("num.service.abc").parse());
  }

  @Test
  public void testParseFail() throws SyntaxException {

    String[] failureCases = {
      "",
      "abc",
      "abc)",
      "abc''",
      "map(,'$!')",
      "map(host.service.abc)",
      "map(host.service.abc,$!)",
      "map(host.service.abc,'$!",
      "map(host.service.abc,'$!'",
      "map(host.service.abc,'$!'))",
      "join(,'-')",
      "join(host.service.abc)",
      "join(host.service.abc,-)",
      "join(host.service.abc,)",
      "join(host.service.abc,'-'",
      "join(host.service.abc,'-'))",
      "join(map(host.service.abc,'-')))",
      "host.service.abc[123",
      "host.service.abc[]",
      "host.service.abc]",
      "host.service.abc[123.0]"
    };

    for (String failureCase : failureCases) {
      try {
        new Parser(failureCase).parse();
        Assert.fail("parse() should have failed for: " + failureCase);
      } catch (SyntaxException e) {
        // expected
      }
    }
  }


}
