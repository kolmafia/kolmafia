package net.sourceforge.kolmafia.modifiers;

import java.util.regex.Pattern;

public class BooleanModifier implements Modifier {
  private final String name;
  private final Pattern[] descPatterns;
  private final Pattern tagPattern;

  public BooleanModifier(String name, Pattern tagPattern) {
    this(name, (Pattern[])null, tagPattern);
  }

  public BooleanModifier(String name, Pattern descPattern, Pattern tagPattern) {
    this(name, new Pattern[]{descPattern}, tagPattern);
  }

  public BooleanModifier(String name, Pattern[] descPattern, Pattern tagPattern) {
    this.name = name;
    this.descPatterns = descPattern;
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
