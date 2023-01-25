package net.sourceforge.kolmafia.modifiers;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum BitmapModifier implements Modifier {
  BRIMSTONE("Brimstone", Pattern.compile("Brimstone")),
  CLOATHING("Cloathing", Pattern.compile("Cloathing")),
  SYNERGETIC("Synergetic", Pattern.compile("Synergetic")),
  RAVEOSITY("Raveosity", Pattern.compile("Raveosity: (\\+?\\d+)")),
  MUTEX("Mutually Exclusive", null),
  MUTEX_VIOLATIONS("Mutex Violations", null);
  private final String name;
  private final Pattern tagPattern;

  BitmapModifier(String name, Pattern tagPattern) {
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

  public static final Set<BitmapModifier> BITMAP_MODIFIERS =
      Collections.unmodifiableSet(EnumSet.allOf(BitmapModifier.class));

  private static final Map<String, BitmapModifier> caselessNameToModifier =
      BITMAP_MODIFIERS.stream()
          .collect(Collectors.toMap(type -> type.name.toLowerCase(), Function.identity()));

  // equivalent to `Modifiers.findName`
  public static BitmapModifier byCaselessName(String name) {
    return caselessNameToModifier.get(name.toLowerCase());
  }

  // equivalent to `Modifiers.findModifier`
  public static BitmapModifier byTagPattern(final String tag) {
    for (var modifier : BITMAP_MODIFIERS) {
      Pattern pattern = modifier.getTagPattern();
      if (pattern == null) {
        continue;
      }

      Matcher matcher = pattern.matcher(tag);
      if (matcher.find()) {
        return modifier;
      }
    }
    return null;
  }
}
