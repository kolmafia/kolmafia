package net.sourceforge.kolmafia.modifiers;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum BooleanModifier implements Modifier {
  SOFTCORE(
      "Softcore Only",
      Pattern.compile("This item cannot be equipped while in Hardcore"),
      Pattern.compile("Softcore Only")),
  SINGLE("Single Equip", Pattern.compile("Single Equip")),
  ALWAYS_FUMBLE("Always Fumble", Pattern.compile("Always Fumble")),
  NEVER_FUMBLE("Never Fumble", Pattern.compile("Never Fumble"), Pattern.compile("Never Fumble")),
  WEAKENS(
      "Weakens Monster",
      Pattern.compile("Successful hit weakens opponent"),
      Pattern.compile("Weakens Monster")),
  FREE_PULL("Free Pull", Pattern.compile("Free Pull")),
  VARIABLE("Variable", Pattern.compile("Variable")),
  NONSTACKABLE_WATCH("Nonstackable Watch", Pattern.compile("Nonstackable Watch")),
  COLD_IMMUNITY("Cold Immunity", Pattern.compile("Cold Immunity")),
  HOT_IMMUNITY("Hot Immunity", Pattern.compile("Hot Immunity")),
  SLEAZE_IMMUNITY("Sleaze Immunity", Pattern.compile("Sleaze Immunity")),
  SPOOKY_IMMUNITY("Spooky Immunity", Pattern.compile("Spooky Immunity")),
  STENCH_IMMUNITY("Stench Immunity", Pattern.compile("Stench Immunity")),
  COLD_VULNERABILITY("Cold Vulnerability", Pattern.compile("Cold Vulnerability")),
  HOT_VULNERABILITY("Hot Vulnerability", Pattern.compile("Hot Vulnerability")),
  SLEAZE_VULNERABILITY("Sleaze Vulnerability", Pattern.compile("Sleaze Vulnerability")),
  SPOOKY_VULNERABILITY("Spooky Vulnerability", Pattern.compile("Spooky Vulnerability")),
  STENCH_VULNERABILITY("Stench Vulnerability", Pattern.compile("Stench Vulnerability")),
  MOXIE_CONTROLS_MP("Moxie Controls MP", Pattern.compile("Moxie Controls MP")),
  MOXIE_MAY_CONTROL_MP("Moxie May Control MP", Pattern.compile("Moxie May Control MP")),
  FOUR_SONGS(
      "Four Songs",
      Pattern.compile("Allows you to keep 4 songs in your head instead of 3"),
      Pattern.compile("Four Songs")),
  ADVENTURE_RANDOMLY("Adventure Randomly", Pattern.compile("Adventure Randomly")),
  ADVENTURE_UNDERWATER(
      "Adventure Underwater",
      Pattern.compile("Lets you [bB]reathe [uU]nderwater"),
      Pattern.compile("Adventure Underwater")),
  UNDERWATER_FAMILIAR(
      "Underwater Familiar",
      Pattern.compile("Lets your Familiar Breathe Underwater"),
      Pattern.compile("Underwater Familiar")),
  GENERIC("Generic", Pattern.compile("Generic")),
  UNARMED(
      "Unarmed",
      Pattern.compile("Bonus&nbsp;for&nbsp;Unarmed&nbsp;Characters&nbsp;only"),
      Pattern.compile("Unarmed")),
  NOPULL("No Pull", Pattern.compile("No Pull")),
  LASTS_ONE_DAY(
      "Lasts Until Rollover",
      Pattern.compile("This item will disappear at the end of the day"),
      Pattern.compile("Lasts Until Rollover")),
  ALTERS_PAGE_TEXT("Alters Page Text", Pattern.compile("Alters Page Text")),
  ATTACKS_CANT_MISS(
      "Attacks Can't Miss",
      new Pattern[] {Pattern.compile("Regular Attacks Can't Miss"), Pattern.compile("Cannot miss")},
      Pattern.compile("Attacks Can't Miss")),
  LOOK_LIKE_A_PIRATE("Pirate", Pattern.compile("Look like a Pirate")),
  BLIND("Blind", Pattern.compile("Blind")),
  BREAKABLE("Breakable", Pattern.compile("Breakable")),
  DROPS_ITEMS("Drops Items", Pattern.compile("Drops Items")),
  DROPS_MEAT("Drops Meat", Pattern.compile("Drops Meat")),
  VOLLEYBALL_OR_SOMBRERO("Volleyball or Sombrero", Pattern.compile("Volleyball or Sombrero")),
  EXTRA_PICKPOCKET(
      "Extra Pickpocket",
      new Pattern[] {
        Pattern.compile("Gives you an additional Pickpocketing attempt"),
        Pattern.compile("1 Additional Pickpocket Attempt")
      },
      Pattern.compile("Extra Pickpocket")),
  NEGATIVE_STATUS_RESIST(
      "Negative Status Resist",
      Pattern.compile("75% Chance of Preventing Negative Status Attacks"),
      Pattern.compile("Negative Status Resist"));
  private final String name;
  private final Pattern[] descPatterns;
  private final Pattern tagPattern;

  BooleanModifier(String name, Pattern tagPattern) {
    this(name, (Pattern[]) null, tagPattern);
  }

  BooleanModifier(String name, Pattern descPattern, Pattern tagPattern) {
    this(name, new Pattern[] {descPattern}, tagPattern);
  }

  BooleanModifier(String name, Pattern[] descPattern, Pattern tagPattern) {
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

  @Override
  public ModifierValueType getType() {
    return ModifierValueType.BOOLEAN;
  }

  @Override
  public String toString() {
    return name;
  }

  public static final Set<BooleanModifier> BOOLEAN_MODIFIERS =
      Collections.unmodifiableSet(EnumSet.allOf(BooleanModifier.class));

  private static final Map<String, BooleanModifier> caselessNameToModifier =
      BOOLEAN_MODIFIERS.stream()
          .collect(Collectors.toMap(type -> type.name.toLowerCase(), Function.identity()));

  // equivalent to `Modifiers.findName`
  public static BooleanModifier byCaselessName(String name) {
    return caselessNameToModifier.get(name.toLowerCase());
  }

  // equivalent to `Modifiers.findModifier`
  public static BooleanModifier byTagPattern(final String tag) {
    for (var modifier : BOOLEAN_MODIFIERS) {
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
    for (var mod : BOOLEAN_MODIFIERS) {
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

        return tag + ": " + value.trim();
      }
    }

    return null;
  }
}
