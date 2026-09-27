package net.sourceforge.kolmafia.utilities;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class ColorParserTest {
  private static int rgb(int r, int g, int b) {
    return (r << 16) + (g << 8) + b;
  }

  @Test
  public void itShouldParseKnownColorNames() {
    assertEquals(rgb(255, 0, 0), ColorParser.parseColor("red"));
    assertEquals(rgb(0, 255, 0), ColorParser.parseColor("lime"));
    assertEquals(rgb(0, 0, 255), ColorParser.parseColor("blue"));
    assertEquals(rgb(184, 134, 11), ColorParser.parseColor("darkgoldenrod"));
  }

  @Test
  public void itShouldParseDifferentCasedNames() {
    assertEquals(rgb(255, 0, 0), ColorParser.parseColor("RED"));
    assertEquals(rgb(0, 255, 0), ColorParser.parseColor("Lime"));
    assertEquals(rgb(0, 0, 255), ColorParser.parseColor("blUe"));
  }

  @Test
  public void itShouldNotParseUnknownColorNames() {
    assertEquals(-1, ColorParser.parseColor("poo"));
    assertEquals(-1, ColorParser.parseColor("FOO"));
  }

  @Test
  public void itShouldNotParseMultipleColorNames() {
    assertEquals(-1, ColorParser.parseColor("red blue"));
    assertEquals(-1, ColorParser.parseColor("red\nblue"));
  }

  @Test
  public void itShouldParse6DigitHexStrings() {
    assertEquals(rgb(255, 0, 0), ColorParser.parseColor("#FF0000"));
    assertEquals(rgb(18, 52, 86), ColorParser.parseColor("#123456"));
    assertEquals(rgb(184, 134, 11), ColorParser.parseColor("#b8860B"));
  }

  @Test
  public void itShouldNotYetParse3DigitHexStrings() {
    assertEquals(-1, ColorParser.parseColor("#abc"));
    assertEquals(-1, ColorParser.parseColor("#FEF"));
  }

  @Test
  public void itShouldNotYetParseRGBorHSLStrings() {
    assertEquals(-1, ColorParser.parseColor("rgb(5,11,20)"));
    assertEquals(-1, ColorParser.parseColor("rgba(184,134,11,0.8)"));
    assertEquals(-1, ColorParser.parseColor("hsl(120,100%,25%)"));
  }

  @Test
  public void itShouldNotParseInvalidHexStrings() {
    assertEquals(-1, ColorParser.parseColor("#abcd"));
    assertEquals(-1, ColorParser.parseColor("#foobar"));
    assertEquals(-1, ColorParser.parseColor("#FOOBAR"));
    assertEquals(-1, ColorParser.parseColor("#11"));
  }

  @Test
  public void itShouldNotParseUnprefixedHexStrings() {
    assertEquals(-1, ColorParser.parseColor("FF001E"));
    assertEquals(-1, ColorParser.parseColor("123456"));
  }
}
