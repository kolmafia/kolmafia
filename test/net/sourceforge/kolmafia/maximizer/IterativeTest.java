package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLevel;

import internal.helpers.Cleanups;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeAll;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

public class IterativeTest {
  private static final String TESTUSER = "IterativeTestUser";

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset(TESTUSER);
    Preferences.reset(TESTUSER);
  }

  @CartesianTest
  public void theTest(
      @CartesianTest.Values(ints = {1, 2, 3, 4, 5, 6}) int enumClassId,
      @CartesianTest.Values(ints = {1, 5, 13}) int level,
      @CartesianTest.Values(strings = {"", "weapon damage", "ranged damage"}) String maxStringPart1,
      @CartesianTest.Values(strings = {"", "effective"}) String maxStringPart2,
      @CartesianTest.Values(booleans = {true, false}) boolean equippable) {
    var cleanups = new Cleanups();
    AscensionClass thisClass = AscensionClass.find(enumClassId);
    cleanups.add(withClass(thisClass));
    cleanups.add(withLevel(level));
    dumpStats("Before");
    EnumMap<Slot, AdventureResult> equipment = EquipmentManager.allEquipment();
    for (Map.Entry<Slot, AdventureResult> entry : equipment.entrySet()) {
      if ((entry.getKey() == Slot.WEAPON) || (entry.getKey() == Slot.OFFHAND)) {
        AdventureResult thing = entry.getValue();
        if (equippable) {
          cleanups.add(withEquippableItem(thing));
        } else {
          cleanups.add(withItem(thing));
        }
      }
    }
    dumpStats("After");
    String maxStr = maxStringPart1 + " " + maxStringPart2;
    try (cleanups) {
      maximize(maxStr);
    }
    List<Boost> boosts = getBoosts();
    String boostString = boosts.toString();
    if (!boostString.contains("nothing useful found")) {
      System.out.println(boosts);
    }
  }

  private void dumpStats(String before) {
    KoLCharacter.recalculateAdjustments();
    String output =
        before
            + " "
            + KoLCharacter.getBaseMuscle()
            + " "
            + KoLCharacter.getBaseMysticality()
            + " "
            + KoLCharacter.getBaseMysticality();
    System.out.println(output);
  }
}
