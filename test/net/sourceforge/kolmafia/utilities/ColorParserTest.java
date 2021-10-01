package net.sourceforge.kolmafia.utilities;

import org.junit.Assert;
import org.junit.Test;

public class ColorParserTest {
  private static int rgb(int r, int g, int b) {
    return (r << 16) + (g << 8) + b;
  }

  @Test
  public void itShouldParseKnownColorNames() {
    Assert.assertEquals(rgb(255, 0, 0), ColorParser.parseColor("red"));
    Assert.assertEquals(rgb(0, 255, 0), ColorParser.parseColor("lime"));
    Assert.assertEquals(rgb(0, 0, 255), ColorParser.parseColor("blue"));
    Assert.assertEquals(rgb(184, 134, 11), ColorParser.parseColor("darkgoldenrod"));
  }

  @Test
  public void itShouldParseDifferentCasedNames() {
    Assert.assertEquals(rgb(255, 0, 0), ColorParser.parseColor("RED"));
    Assert.assertEquals(rgb(0, 255, 0), ColorParser.parseColor("Lime"));
    Assert.assertEquals(rgb(0, 0, 255), ColorParser.parseColor("blUe"));
  }

  @Test
  public void itShouldNotParseUnknownColorNames() {
    Assert.assertEquals(-1, ColorParser.parseColor("poo"));
    Assert.assertEquals(-1, ColorParser.parseColor("FOO"));
  }

  @Test
  public void itShouldNotParseMultipleColorNames() {
    Assert.assertEquals(-1, ColorParser.parseColor("red blue"));
    Assert.assertEquals(-1, ColorParser.parseColor("red\nblue"));
  }

  @Test
  public void itShouldParse6DigitHexStrings() {
    Assert.assertEquals(rgb(255, 0, 0), ColorParser.parseColor("#FF0000"));
    Assert.assertEquals(rgb(18, 52, 86), ColorParser.parseColor("#123456"));
    Assert.assertEquals(rgb(184, 134, 11), ColorParser.parseColor("#b8860B"));
  }

  @Test
  public void itShouldNotYetParse3DigitHexStrings() {
    Assert.assertEquals(-1, ColorParser.parseColor("#abc"));
    Assert.assertEquals(-1, ColorParser.parseColor("#FEF"));
  }

  @Test
  public void itShouldNotYetParseRGBorHSLStrings() {
    Assert.assertEquals(-1, ColorParser.parseColor("rgb(5,11,20)"));
    Assert.assertEquals(-1, ColorParser.parseColor("rgba(184,134,11,0.8)"));
    Assert.assertEquals(-1, ColorParser.parseColor("hsl(120,100%,25%)"));
  }

  @Test
  public void itShouldNotParseInvalidHexStrings() {
    Assert.assertEquals(-1, ColorParser.parseColor("#abcd"));
    Assert.assertEquals(-1, ColorParser.parseColor("#foobar"));
    Assert.assertEquals(-1, ColorParser.parseColor("#FOOBAR"));
    Assert.assertEquals(-1, ColorParser.parseColor("#11"));
  }

  @Test
  public void itShouldNotParseUnprefixedHexStrings() {
    Assert.assertEquals(-1, ColorParser.parseColor("FF001E"));
    Assert.assertEquals(-1, ColorParser.parseColor("123456"));
  }
}
