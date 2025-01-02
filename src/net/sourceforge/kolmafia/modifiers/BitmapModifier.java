package net.sourceforge.kolmafia.modifiers;

import static net.sourceforge.kolmafia.persistence.ModifierDatabase.EXPR;

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
  SURGEONOSITY(
      "Surgeonosity",
      new Pattern[] {
        Pattern.compile("Makes you look like a doctor"),
        Pattern.compile("Makes you look like a gross doctor"),
      },
      Pattern.compile("Surgeonosity: (\\+?\\d+)")),
  CLOWNINESS(
      "Clowniness",
      Pattern.compile("Makes you look (\\d+)% clowny"),
      Pattern.compile("Clowniness: " + EXPR)),
  RAVEOSITY("Raveosity", Pattern.compile("Raveosity: (\\+?\\d+)")),
  MCHUGELARGE("McHugeLarge", Pattern.compile("McHugeLarge")),
  MUTEX("Mutually Exclusive", null),
  MUTEX_VIOLATIONS("Mutex Violations", null);
  private final String name;
  private final Pattern[] descPatterns;
  private final Pattern tagPattern;

  BitmapModifier(String name, Pattern tagPattern) {
    this(name, (Pattern[]) null, tagPattern);
  }

  BitmapModifier(String name, Pattern descPattern, Pattern tagPattern) {
    this(name, new Pattern[] {descPattern}, tagPattern);
  }

  BitmapModifier(String name, Pattern[] descPatterns, Pattern tagPattern) {
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

  @Override
  public ModifierValueType getType() {
    return ModifierValueType.NUMERIC;
  }

  @Override
  public String toString() {
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
      if (matcher.matches()) {
        return modifier;
      }
    }
    return null;
  }

  // equivalent to `Modifiers.parseModifier`
  public static String parseModifier(final String enchantment) {
    for (var mod : BITMAP_MODIFIERS) {
      Pattern[] patterns = mod.getDescPatterns();

      if (patterns == null) {
        continue;
      }

      for (Pattern pattern : patterns) {
        Matcher matcher = pattern.matcher(enchantment);
        if (!matcher.find()) {
          continue;
        }

        if (matcher.groupCount() == 0) {
          String tag = mod.getTag();
          // Kludge for Surgeonosity, which always gives +1
          if (mod == BitmapModifier.SURGEONOSITY) {
            return tag + ": +1";
          }
          return tag;
        }

        String tag = mod.getTag();

        String value = matcher.group(1);

        return tag + ": " + value.trim();
      }
    }

    return null;
  }
}
