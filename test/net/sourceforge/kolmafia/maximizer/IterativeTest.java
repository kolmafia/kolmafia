package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withStats;

import internal.helpers.Cleanups;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

public class IterativeTest {
  private static final String TESTUSER = "IterativeTestUser";

  private static final Map<String, Integer> considerThese = getItemsOfInterest();

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset(TESTUSER);
    Preferences.reset(TESTUSER);
  }

  @CartesianTest
  public void theTest(
      @CartesianTest.Values(ints = {1, 2, 3, 4, 5, 6}) int enumClassId,
      @CartesianTest.Values(strings = {"", "weapon damage", "ranged damage"}) String maxStringPart1,
      @CartesianTest.Values(strings = {"", "effective"}) String maxStringPart2) {
    var cleanups = new Cleanups();
    AscensionClass thisClass = AscensionClass.find(enumClassId);
    cleanups.add(withClass(thisClass));
    cleanups.add(withStats(250, 250, 250));
    for (String it : considerThese.keySet()) {
      cleanups.add(withItem(it));
    }
    String maxStr = maxStringPart1 + " " + maxStringPart2;
    try (cleanups) {
      maximize(maxStr);
    }
    double score = Maximizer.best.getScore();
    List<Boost> boosts = getBoosts();
    String boostString = boosts.toString();
    boolean weaponPicked = boostString.contains("equip weapon");
    boolean offPicked = boostString.contains("equip off-hand");
    if (!weaponPicked) {
      System.out.println(score + " " + thisClass + " " + maxStr);
      System.out.println(boosts);
    }
  }

  private static Map<String, Integer> getItemsOfInterest() {
    Map<String, Integer> retVal = new TreeMap<>();
    int weaponPower = -1;
    int offPower = -1;
    for (Map.Entry<Integer, String> entry : ItemDatabase.dataNameEntrySet()) {
      Integer key = entry.getKey();
      String name = entry.getValue();
      int power = EquipmentDatabase.getPower(key);
      KoLConstants.ConsumptionType type = ItemDatabase.getConsumptionType(key);
      switch (type) {
        case WEAPON:
          //if (power > weaponPower) {
          //  weaponPower = power;
          retVal.put(name, key);
          //}
          break;
        case OFFHAND:
          //if (power > offPower) {
          //  offPower = power;
            retVal.put(name, key);
          //}
          break;
      }
    }
    return retVal;
  }
}
