package net.sourceforge.kolmafia.utilities;

import java.util.Locale;
import java.util.regex.Pattern;

/** A color as HTML writes it, "#rrggbb", kept in lower case. */
public record HexColor(String value) {
  private static final Pattern FORMAT = Pattern.compile("#[0-9a-f]{6}");

  public HexColor {
    value = value.toLowerCase(Locale.ROOT);
    if (!FORMAT.matcher(value).matches()) {
      throw new IllegalArgumentException("Not a color of the form #rrggbb: " + value);
    }
  }

  public HexColor(final int red, final int green, final int blue) {
    this(String.format("#%02x%02x%02x", red, green, blue));
  }

  @Override
  public String toString() {
    return this.value;
  }
}
