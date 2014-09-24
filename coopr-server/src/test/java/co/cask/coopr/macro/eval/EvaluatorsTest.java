package co.cask.coopr.macro.eval;

import co.cask.coopr.macro.SyntaxException;
import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class EvaluatorsTest {

  @Test
  public void testParseMacroName() throws SyntaxException {
    Assert.assertEquals(new HostServiceEvaluator("abc", null),
                        Evaluators.evaluatorFor("host.service.abc", null));
    Assert.assertEquals(new IPServiceEvaluator("abc", "access_v4", null),
                        Evaluators.evaluatorFor("ip.access_v4.service.abc", null));
    Assert.assertEquals(new IPServiceEvaluator("abc", "bind_v4", null),
                        Evaluators.evaluatorFor("ip.service.abc", null));
    Assert.assertTrue(Evaluators.evaluatorFor("cluster.owner", null) instanceof  ClusterOwnerEvaluator);
    for (String macro : ImmutableList.of("", "host.service.", "IP_OF_SERVICE.abc", "SERVICE_BULLSHIT_abc")) {
      try {
        Evaluators.evaluatorFor(macro, null);
        Assert.fail("'" + macro + "' should have thrown syntax exception.");
      } catch (SyntaxException e) {
        // expected
      }
    }
  }
}
