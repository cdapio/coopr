package co.cask.coopr.spec.template;

import co.cask.coopr.layout.InvalidClusterException;
import org.junit.Test;

/**
 *
 */
public class SizeConstraintTest {

  @Test(expected = InvalidClusterException.class)
  public void testSizeTooSmall() throws InvalidClusterException {
    SizeConstraint sizeConstraint = new SizeConstraint(3, 10);
    sizeConstraint.verify(2);
  }

  @Test(expected = InvalidClusterException.class)
  public void testSizeTooBig() throws InvalidClusterException {
    SizeConstraint sizeConstraint = new SizeConstraint(3, 10);
    sizeConstraint.verify(11);
  }

  @Test
  public void testSizeOK() throws InvalidClusterException {
    SizeConstraint sizeConstraint = new SizeConstraint(3, 10);
    for (int i = sizeConstraint.getMin(); i <= sizeConstraint.getMax(); i++) {
      sizeConstraint.verify(i);
    }
  }
}
