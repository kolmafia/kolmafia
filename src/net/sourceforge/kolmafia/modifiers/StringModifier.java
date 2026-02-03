package net.sourceforge.kolmafia.modifiers;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AscensionClass;

public enum StringModifier implements Modifier {
  CLASS(
      "Class",
      new Pattern[] {
        Pattern.compile("Only (.*?) may use this item"),
        Pattern.compile("Bonus for (.*?) only"),
        Pattern.compile("Bonus&nbsp;for&nbsp;(.*?)&nbsp;only"),
      },
      Pattern.compile("Class: \"(.*?)\"")),
  INTRINSIC_EFFECT(
      "Intrinsic Effect",
      Pattern.compile("Intrinsic Effect: <a.*?><font color=blue>(.*)</font></a>"),
      Pattern.compile("Intrinsic Effect: \"(.*?)\"")),
  EQUALIZE("Equalize", Pattern.compile("Equalize: \"(.*?)\"")),
  WIKI_NAME("Wiki Name", Pattern.compile("Wiki Name: \"(.*?)\"")),
  MODIFIERS("Modifiers", Pattern.compile("^(none)$")),
  OUTFIT("Outfit", null),
  STAT_TUNING("Stat Tuning", Pattern.compile("Stat Tuning: \"(.*?)\"")),
  EFFECT("Effect", Pattern.compile("Effect: \"(.*?)\""), true),
  EQUIPS_ON("Equips On", Pattern.compile("Equips On: \"(.*?)\"")),
  FAMILIAR_EFFECT("Familiar Effect", Pattern.compile("Familiar Effect: \"(.*?)\"")),
  JIGGLE("Jiggle", Pattern.compile("Jiggle: *(.*?)$"), Pattern.compile("Jiggle: \"(.*?)\"")),
  EQUALIZE_MUSCLE("Equalize Muscle", Pattern.compile("Equalize Muscle: \"(.*?)\"")),
  EQUALIZE_MYST("Equalize Mysticality", Pattern.compile("Equalize Mysticality: \"(.*?)\"")),
  EQUALIZE_MOXIE("Equalize Moxie", Pattern.compile("Equalize Moxie: \"(.*?)\"")),
  AVATAR(
      "Avatar",
      new Pattern[] {
        Pattern.compile("Makes you look like (?:a |an |the )?(.++)(?<!doctor|gross doctor)"),
        Pattern.compile("Te hace ver como un (.++)"),
      },
      Pattern.compile("Avatar: \"(.*?)\"")),
  ROLLOVER_EFFECT(
      "Rollover Effect",
      Pattern.compile("Adventures of <b><a.*?>(.*)</a></b> at Rollover"),
      Pattern.compile("Rollover Effect: \"(.*?)\""),
      true),
  SKILL(
      "Skill",
      Pattern.compile("Grants Skill:.*?<b>(.*?)</b>"),
      Pattern.compile("Skill: \"(.*?)\"")),
  FLOOR_BUFFED_MUSCLE("Floor Buffed Muscle", Pattern.compile("Floor Buffed Muscle: \"(.*?)\"")),
  FLOOR_BUFFED_MYST(
      "Floor Buffed Mysticality", Pattern.compile("Floor Buffed Mysticality: \"(.*?)\"")),
  FLOOR_BUFFED_MOXIE("Floor Buffed Moxie", Pattern.compile("Floor Buffed Moxie: \"(.*?)\"")),
  PLUMBER_STAT("Plumber Stat", Pattern.compile("Plumber Stat: \"(.*?)\"")),
  RECIPE("Recipe", Pattern.compile("Recipe: \"(.*?)\"")),
  EVALUATED_MODIFIERS("Evaluated Modifiers"),
  LAST_AVAILABLE_DATE("Last Available", Pattern.compile("Last Available: \"(.*?)\"")),
  CONDITIONAL_SKILL_EQUIPPED(
      "Conditional Skill (Equipped)",
      new Pattern[] {Pattern.compile("Grants \"(.*?)\" Combat Skill")},
      Pattern.compile("Conditional Skill \\(Equipped\\): \"(.*?)\""),
      true),
  CONDITIONAL_SKILL_INVENTORY(
      "Conditional Skill (Inventory)",
      Pattern.compile("Conditional Skill \\(Inventory\\): \"(.*?)\""),
      true),
  LANTERN_ELEMENT("Lantern Element", Pattern.compile("Lantern Element: \"(.*?)\""), true);
  private final String name;
  private final Pattern[] descPatterns;
  private final Pattern tagPattern;
  private final boolean multiple;

  StringModifier(String name) {
    this(name, null);
  }

  StringModifier(String name, Pattern tagPattern) {
    this(name, (Pattern[]) null, tagPattern);
  }

  StringModifier(String name, Pattern tagPattern, boolean multiple) {
    this(name, (Pattern[]) null, tagPattern, multiple);
  }

  StringModifier(String name, Pattern descPattern, Pattern tagPattern) {
    this(name, new Pattern[] {descPattern}, tagPattern);
  }

  StringModifier(String name, Pattern descPattern, Pattern tagPattern, boolean multiple) {
    this(name, new Pattern[] {descPattern}, tagPattern, multiple);
  }

  StringModifier(String name, Pattern[] descPatterns, Pattern tagPattern) {
    this(name, descPatterns, tagPattern, false);
  }

  StringModifier(String name, Pattern[] descPatterns, Pattern tagPattern, boolean multiple) {
    this.name = name;
    this.descPatterns = descPatterns;
    this.tagPattern = tagPattern;
    this.multiple = multiple;
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

  public boolean isMultiple() {
    return multiple;
  }

  @Override
  public ModifierValueType getType() {
    return multiple ? ModifierValueType.MULTISTRING : ModifierValueType.STRING;
  }

  @Override
  public String toString() {
    return name;
  }

  public static final Set<StringModifier> STRING_MODIFIERS =
      Collections.unmodifiableSet(EnumSet.allOf(StringModifier.class));

  private static final Map<String, StringModifier> caselessNameToModifier =
      STRING_MODIFIERS.stream()
          .collect(Collectors.toMap(type -> type.name.toLowerCase(), Function.identity()));

  // equivalent to `Modifiers.findName`
  public static StringModifier byCaselessName(String name) {
    return caselessNameToModifier.get(name.toLowerCase());
  }

  // equivalent to `Modifiers.findModifier`
  public static StringModifier byTagPattern(final String tag) {
    for (var modifier : STRING_MODIFIERS) {
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

    for (var mod : STRING_MODIFIERS) {
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

        if (mod == StringModifier.CLASS) {
          value = StringModifier.depluralizeClassName(value);
        }

        return tag + ": " + quote + value.trim() + quote;
      }
    }

    return null;
  }

  private static final String[][] classStrings = {
    {
      AscensionClass.SEAL_CLUBBER.getName(), "Seal Clubbers", "Seal&nbsp;Clubbers",
    },
    {
      AscensionClass.TURTLE_TAMER.getName(), "Turtle Tamers", "Turtle&nbsp;Tamers",
    },
    {
      AscensionClass.PASTAMANCER.getName(), "Pastamancers",
    },
    {
      AscensionClass.SAUCEROR.getName(), "Saucerors",
    },
    {
      AscensionClass.DISCO_BANDIT.getName(), "Disco Bandits", "Disco&nbsp;Bandits",
    },
    {
      AscensionClass.ACCORDION_THIEF.getName(), "Accordion Thieves", "Accordion&nbsp;Thieves",
    },
  };

  public static String depluralizeClassName(final String string) {
    for (String[] results : classStrings) {
      String result = results[0];
      for (String candidate : results) {
        if (candidate.equals(string)) {
          return result;
        }
      }
    }
    return string;
  }
}
