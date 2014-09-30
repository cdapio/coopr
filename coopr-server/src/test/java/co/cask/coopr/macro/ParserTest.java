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

import co.cask.coopr.macro.eval.HostServiceEvaluator;
import co.cask.coopr.macro.eval.IPSelfEvaluator;
import co.cask.coopr.macro.eval.IPServiceEvaluator;
import co.cask.coopr.macro.eval.ServiceCardinalityEvaluator;
import co.cask.coopr.macro.eval.ServiceInstanceEvaluator;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests parsing of macro expressions.
 */
public class ParserTest {

  @Test
  public void testParseOK() throws SyntaxException {
    assertEquals(new Expression(new HostServiceEvaluator("abc", null), null, null),
                 new Parser("host.service.abc").parse());
    assertEquals(new Expression(new IPServiceEvaluator("namenode", "access_v4", null), null, null),
                 new Parser("ip.access_v4.service.namenode").parse());
    assertEquals(new Expression(new IPServiceEvaluator("a-b-c", "bind_v4", null), null, null),
                 new Parser("ip.bind_v4.service.a-b-c").parse());
    assertEquals(new Expression(new IPSelfEvaluator("access_v4"), null, null),
                 new Parser("ip.access_v4.self").parse());
    assertEquals(new Expression(new IPSelfEvaluator("bind_v4"), null, null),
                 new Parser("ip.self").parse());
    assertEquals(new Expression(new IPServiceEvaluator("a-b-c", "bind_v4", null), null, null),
                 new Parser("ip.service.a-b-c").parse());
    assertEquals(new Expression(new IPServiceEvaluator("a.b.c", "external", null), null, null),
                 new Parser("ip.external.service.a.b.c").parse());
    assertEquals(new Expression(new IPServiceEvaluator("a_b_c", "internal", null), null, null),
                 new Parser("ip.internal.service.a_b_c").parse());

    assertEquals(new Expression(new IPServiceEvaluator("nn", "access", null), "$:80", null),
                 new Parser("map(ip.access.service.nn,'$:80')").parse());
    assertEquals(new Expression(new IPServiceEvaluator("nn", "access", null), "\"$\"", null),
                 new Parser("map(ip.access.service.nn,'\"$\"')").parse());
    assertEquals(new Expression(new IPServiceEvaluator("nn", "access", null), "$:80", null),
                 new Parser("map(ip.access.service.nn,\"$:80\")").parse());
    assertEquals(new Expression(new IPServiceEvaluator("nn", "access", null), "'$'", null),
                 new Parser("map(ip.access.service.nn,\"'$'\")").parse());

    assertEquals(new Expression(new IPServiceEvaluator("nn", "access", null), null, "-"),
                 new Parser("join(ip.access.service.nn,'-')").parse());
    assertEquals(new Expression(new IPServiceEvaluator("nn", "access", null), null, "\n"),
                 new Parser("join(ip.access.service.nn,\"\n\")").parse());

    assertEquals(new Expression(new HostServiceEvaluator("nn", null), "$:2181", ","),
                 new Parser("join(map(host.service.abc,'$:2181'),',')").parse());

    assertEquals(new Expression(new HostServiceEvaluator("abc", 1), null, null),
                 new Parser("host.service.abc[1]").parse());
    assertEquals(new Expression(new IPServiceEvaluator("abc", "access", 0), null, null),
                 new Parser("ip.access.service.abc[0]").parse());
    assertEquals(new Expression(new ServiceInstanceEvaluator("abc"), null, null),
                 new Parser("instance.self.service.abc").parse());
    assertEquals(new Expression(new ServiceCardinalityEvaluator("abc"), null, null),
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
