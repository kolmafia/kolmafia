package net.sourceforge.kolmafia.modifiers;

import java.util.regex.Pattern;

public class DerivedModifier implements Modifier {
  private final String name;

  public DerivedModifier(String name) {
    this.name = name;
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
    return null;
  }

  @Override
  public String getTag() {
    return null;
  }
}
