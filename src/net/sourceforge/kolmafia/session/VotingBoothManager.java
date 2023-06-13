package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.modifiers.ModifierList.ModifierValue;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;
import net.sourceforge.kolmafia.utilities.PHPRandom;

public class VotingBoothManager {
  private static final ModifierValue[] VOTING_BOOTH_POSITIVE_MODIFIERS = {
    new ModifierValue("Monster Level", "+10"),
    new ModifierValue("Food Drop", "+30"),
    new ModifierValue("Monster Level", "-10"),
    new ModifierValue("Initiative", "+25"),
    new ModifierValue("Stench Damage", "+10"),
    new ModifierValue("Sleaze Damage", "+10"),
    new ModifierValue("Pants Drop", "+30"),
    new ModifierValue("Maximum MP Percent", "+30"),
    new ModifierValue("Moxie Percent", "+25"),
    new ModifierValue("Ranged Damage Percent", "+100"),
    new ModifierValue("Experience (Mysticality)", "+4"),
    new ModifierValue("Experience (Moxie)", "+4"),
    new ModifierValue("Weapon Damage Percent", "+100"),
    new ModifierValue("Stench Resistance", "+3"),
    new ModifierValue("Booze Drop", "+30"),
    new ModifierValue("Item Drop", "+15"),
    new ModifierValue("Cold Damage", "+10"),
    new ModifierValue("Hot Resistance", "+3"),
    new ModifierValue("Weapon Damage Unarmed", "+20"),
    new ModifierValue("Muscle Percent", "+25"),
    new ModifierValue("Experience", "+3"),
    new ModifierValue("Spell Damage Percent", "+20"),
    new ModifierValue("Spooky Resistance", "+3"),
    new ModifierValue("Hot Damage", "+10"),
    new ModifierValue("Meat Drop", "+30"),
    new ModifierValue("Experience (familiar)", "+2"),
    new ModifierValue("Mysticality Percent", "+25"),
    new ModifierValue("Cold Resistance", "+3"),
    new ModifierValue("Experience (Muscle)", "+4"),
    new ModifierValue("Gear Drop", "+30"),
    new ModifierValue("Adventures", "+1"),
    new ModifierValue("Candy Drop", "+30"),
    new ModifierValue("Maximum HP Percent", "+30"),
    new ModifierValue("Sleaze Resistanc", "+3"),
  };

  private static final ModifierValue[] VOTING_BOOTH_NEGATIVE_MODIFIERS = {
    new ModifierValue("Maximum MP Percent", "-50"),
    new ModifierValue("Initiative", "-30"),
    new ModifierValue("Moxie", "-20"),
    new ModifierValue("Experience", "-3"),
    new ModifierValue("Spell Damage Percent", "-50"),
    new ModifierValue("Muscle", "-20"),
    new ModifierValue("Meat Drop", "-30"),
    new ModifierValue("Adventures", "-2"),
    new ModifierValue("Item Drop", "-20"),
    new ModifierValue("Critical Hit Percent", "-10"),
    new ModifierValue("Experience (familiar)", "-2"),
    new ModifierValue("Gear Drop", "-50"),
    new ModifierValue("Maximum HP Percent", "-50"),
    new ModifierValue("Mysticality", "-20"),
    new ModifierValue("Weapon Damage Percent", "-50"),
  };

  private VotingBoothManager() {}

  public static final int calculateSeed(final int clss, final int path, final int daycount) {
    return (4 * path) + (9 * clss) + (79 * daycount);
  }

  public static final ModifierValue[] getPositiveInitiatives(final int seed) {
    PHPRandom rng = new PHPRandom(seed);

    int[] positives = rng.array(VOTING_BOOTH_POSITIVE_MODIFIERS.length, 3);

    ModifierValue[] modifiers = new ModifierValue[3];

    for (int p = 0; p < modifiers.length; p++) {
      modifiers[p] = VOTING_BOOTH_POSITIVE_MODIFIERS[positives[p]];
    }

    return modifiers;
  }

  public static final ModifierValue getNegativeInitiative(final int seed) {
    PHPMTRandom mtRng = new PHPMTRandom(seed);

    int n = 15;
    while (n > 14) {
      n = mtRng.nextInt(0, 15);
    }

    return VOTING_BOOTH_NEGATIVE_MODIFIERS[n];
  }

  public static final ModifierValue[] getInitiatives(
      final int clss, final int path, final int daycount) {
    int seed = calculateSeed(clss, path, daycount);

    ModifierValue[] modifiers = new ModifierValue[4];

    ModifierValue[] positive = getPositiveInitiatives(seed);

    for (int i = 0; i < positive.length; i++) {
      modifiers[i] = positive[i];
    }

    modifiers[3] = getNegativeInitiative(seed);

    return modifiers;
  }
}
