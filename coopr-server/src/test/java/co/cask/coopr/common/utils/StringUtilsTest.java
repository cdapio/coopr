package co.cask.coopr.common.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class StringUtilsTest {

  @Test
  public void testDNSSuffixLengthValidation() {
    // 26 + 26 + 10 + 1 = 63 characters.
    String validLabel = "abcdefghijklmnopqrstuvwxyz0123456789-ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    Assert.assertTrue(StringUtils.isValidDNSSuffix(validLabel + "." + validLabel + "." + validLabel));

    // test overall length check
    Assert.assertFalse(StringUtils.isValidDNSSuffix(validLabel + "." + validLabel + "." + validLabel + "." + "asdf"));
    // test label length
    Assert.assertFalse(StringUtils.isValidDNSSuffix(validLabel + "." + validLabel + "0"));
    // test dash at end of label
    Assert.assertFalse(StringUtils.isValidDNSSuffix("dev.123fasd-.net"));
    // test some invalid chars
    Assert.assertFalse(StringUtils.isValidDNSSuffix("dev.compa_y.net"));
    Assert.assertFalse(StringUtils.isValidDNSSuffix("dev.compa*y.net"));
    Assert.assertFalse(StringUtils.isValidDNSSuffix("dev.compa?y.net"));
    Assert.assertFalse(StringUtils.isValidDNSSuffix("dev.compa&y.net"));
    Assert.assertFalse(StringUtils.isValidDNSSuffix("dev.compa^y.net"));
    Assert.assertFalse(StringUtils.isValidDNSSuffix("dev.compa%y.net"));
    Assert.assertFalse(StringUtils.isValidDNSSuffix("dev.compa#y.net"));
    Assert.assertFalse(StringUtils.isValidDNSSuffix("dev.compa@y.net"));
    Assert.assertFalse(StringUtils.isValidDNSSuffix("dev.compa!y.net"));
    Assert.assertFalse(StringUtils.isValidDNSSuffix("dev.compa(y.net"));
    Assert.assertFalse(StringUtils.isValidDNSSuffix("dev.compa)y.net"));
    Assert.assertFalse(StringUtils.isValidDNSSuffix("dev.compa/y.net"));
  }

  @Test
  public void testStripLeadingDigits() {
    Assert.assertEquals("", StringUtils.stripLeadingDigits("1234567890"));
    Assert.assertEquals("abc", StringUtils.stripLeadingDigits("1234567890abc"));
  }
}
