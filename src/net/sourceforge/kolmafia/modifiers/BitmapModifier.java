package net.sourceforge.kolmafia.modifiers;

import java.util.regex.Pattern;

public class BitmapModifier implements Modifier {
  private final String name;
  private final Pattern[] descPatterns;
  private final Pattern tagPattern;

  public BitmapModifier(String name, Pattern[] descPatterns, Pattern tagPattern) {
    this.name = name;
    this.descPatterns = descPatterns;
    this.tagPattern = tagPattern;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Pattern[] getDescPatterns() {
    return descPatterns;
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
