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

public enum MultiDoubleModifier implements Modifier {
  EFFECT_DURATION("Effect Duration", Pattern.compile("Effect Duration: " + EXPR)),
  ROLLOVER_EFFECT_DURATION(
      "Rollover Effect Duration",
      Pattern.compile("Grants (\\d+) Adventures of <b>.*?</b> at Rollover"),
      Pattern.compile("Rollover Effect Duration: " + EXPR));
  private final String name;
  private final Pattern[] descPatterns;
  private final Pattern tagPattern;

  MultiDoubleModifier(String name) {
    this(name, null);
  }

  MultiDoubleModifier(String name, Pattern tagPattern) {
    this(name, (Pattern[]) null, tagPattern);
  }

  MultiDoubleModifier(String name, Pattern descPattern, Pattern tagPattern) {
    this(name, new Pattern[] {descPattern}, tagPattern);
  }

  MultiDoubleModifier(String name, Pattern[] descPatterns, Pattern tagPattern) {
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
    return ModifierValueType.MULTINUMERIC;
  }

  @Override
  public String toString() {
    return name;
  }

  public static final Set<MultiDoubleModifier> MULTIDOUBLE_MODIFIERS =
      Collections.unmodifiableSet(EnumSet.allOf(MultiDoubleModifier.class));

  private static final Map<String, MultiDoubleModifier> caselessNameToModifier =
      MULTIDOUBLE_MODIFIERS.stream()
          .collect(Collectors.toMap(type -> type.name.toLowerCase(), Function.identity()));

  // equivalent to `Modifiers.findName`
  public static MultiDoubleModifier byCaselessName(String name) {
    return caselessNameToModifier.get(name.toLowerCase());
  }

  // equivalent to `Modifiers.findModifier`
  public static MultiDoubleModifier byTagPattern(final String tag) {
    for (var modifier : MULTIDOUBLE_MODIFIERS) {
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
    for (var mod : MULTIDOUBLE_MODIFIERS) {
      Pattern[] patterns = mod.getDescPatterns();

      if (patterns == null) {
        continue;
      }

      for (Pattern pattern : patterns) {
        Matcher matcher = pattern.matcher(enchantment);
        if (!matcher.find()) {
          continue;
        }

        String tag = mod.getTag();

        if (matcher.groupCount() == 0) {
          return tag + ": 1";
        }

        String value = matcher.group(1);

        return tag + ": " + value.trim();
      }
    }

    return null;
  }
}
