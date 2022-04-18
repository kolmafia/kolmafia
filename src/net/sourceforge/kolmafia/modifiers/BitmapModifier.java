package net.sourceforge.kolmafia.modifiers;

import java.util.regex.Pattern;

public class BitmapModifier implements Modifier {
  private final String name;
  private final Pattern tagPattern;

  public BitmapModifier(String name, Pattern tagPattern) {
    this.name = name;
    this.tagPattern = tagPattern;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Pattern[] getDescPatterns() {
    return null;
  }

  @Override
  public Pattern getTagPattern() {
    return tagPattern;
  }

  @Override
  public String getTag() {
    return name;
  }
}
