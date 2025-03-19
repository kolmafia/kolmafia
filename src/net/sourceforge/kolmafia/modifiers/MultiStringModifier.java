package net.sourceforge.kolmafia.modifiers;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum MultiStringModifier implements Modifier {
  EFFECT("Effect", Pattern.compile("Effect: \"(.*?)\"")),
  ROLLOVER_EFFECT(
      "Rollover Effect",
      Pattern.compile("Adventures of <b><a.*?>(.*)</a></b> at Rollover"),
      Pattern.compile("Rollover Effect: \"(.*?)\"")),
  CONDITIONAL_SKILL_EQUIPPED(
      "Conditional Skill (Equipped)",
      new Pattern[] {Pattern.compile("Grants \"(.*?)\" Combat Skill")},
      Pattern.compile("Conditional Skill \\(Equipped\\): \"(.*?)\"")),
  CONDITIONAL_SKILL_INVENTORY(
      "Conditional Skill (Inventory)",
      Pattern.compile("Conditional Skill \\(Inventory\\): \"(.*?)\"")),
  LANTERN_ELEMENT("Lantern Element", Pattern.compile("Lantern Element: \"(.*?)\""));
  private final String name;
  private final Pattern[] descPatterns;
  private final Pattern tagPattern;

  MultiStringModifier(String name) {
    this(name, null);
  }

  MultiStringModifier(String name, Pattern tagPattern) {
    this(name, (Pattern[]) null, tagPattern);
  }

  MultiStringModifier(String name, Pattern descPattern, Pattern tagPattern) {
    this(name, new Pattern[] {descPattern}, tagPattern);
  }

  MultiStringModifier(String name, Pattern[] descPatterns, Pattern tagPattern) {
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
    return ModifierValueType.MULTISTRING;
  }

  @Override
  public String toString() {
    return name;
  }

  public static final Set<MultiStringModifier> MULTISTRING_MODIFIERS =
      Collections.unmodifiableSet(EnumSet.allOf(MultiStringModifier.class));

  private static final Map<String, MultiStringModifier> caselessNameToModifier =
      MULTISTRING_MODIFIERS.stream()
          .collect(Collectors.toMap(type -> type.name.toLowerCase(), Function.identity()));

  // equivalent to `Modifiers.findName`
  public static MultiStringModifier byCaselessName(String name) {
    return caselessNameToModifier.get(name.toLowerCase());
  }

  // equivalent to `Modifiers.findModifier`
  public static MultiStringModifier byTagPattern(final String tag) {
    for (var modifier : MULTISTRING_MODIFIERS) {
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
    String quote = "\"";

    for (var mod : MULTISTRING_MODIFIERS) {
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
          return tag;
        }

        String value = matcher.group(1);

        return tag + ": " + quote + value.trim() + quote;
      }
    }

    return null;
  }
}
