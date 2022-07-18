package net.sourceforge.kolmafia.modifiers;

import java.util.regex.Pattern;

public class DoubleModifier implements Modifier {
  private final String name;
  private final Pattern[] descPatterns;
  private final Pattern tagPattern;
  private final String tag;

  public DoubleModifier(String name, Pattern tagPattern) {
    this(name, (Pattern[]) null, tagPattern, name);
  }

  public DoubleModifier(String name, Pattern tagPattern, String tag) {
    this(name, (Pattern[]) null, tagPattern, tag);
  }

  public DoubleModifier(String name, Pattern descPattern, Pattern tagPattern) {
    this(name, new Pattern[] {descPattern}, tagPattern, name);
  }

  public DoubleModifier(String name, Pattern descPattern, Pattern tagPattern, String tag) {
    this(name, new Pattern[] {descPattern}, tagPattern, tag);
  }

  public DoubleModifier(String name, Pattern[] descPatterns, Pattern tagPattern) {
    this(name, descPatterns, tagPattern, name);
  }

  public DoubleModifier(String name, Pattern[] descPatterns, Pattern tagPattern, String tag) {
    this.name = name;
    this.descPatterns = descPatterns;
    this.tagPattern = tagPattern;
    this.tag = tag;
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
    return tag;
  }
}
