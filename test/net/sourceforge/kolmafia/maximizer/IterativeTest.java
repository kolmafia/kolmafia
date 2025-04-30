package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;

import internal.helpers.Cleanups;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

public class IterativeTest {
  private static final String TESTUSER = "IterativeTestUser";

  private static final Map<String, Integer> considerThese = getItemsOfInterest(false);
  private static final Map<String, Integer> considerTheseTerse = getItemsOfInterest(true);
  private static final Set<String> usedBoosts = new TreeSet<>();

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset(TESTUSER);
    Preferences.reset(TESTUSER);
    usedBoosts.clear();
  }

  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset(TESTUSER);
    Preferences.reset(TESTUSER);
  }

  @AfterAll
  public static void dumpData() {
    for (String boost : usedBoosts) {
      System.out.println(boost);
    }
  }

  /*
  This test is to explore possibilities.  It does, however, take time to run.  The list below shows things
  that were chosen to be equipped.  It is the basis for some of the other tests.  There were no cases where
  a weapon was not picked but an off hand was.  There were also no cases where a weapon was not picked.

      [equip weapon Granny Hackleton's Gatling gun (+200), equip off-hand 9-ball (+8)]
      [equip weapon Granny Hackleton's Gatling gun (+200), equip off-hand left-handed melodica (+25)]
      [equip weapon Granny Hackleton's Gatling gun (+200), equip off-hand replica Operation Patriot Shield (+20)]
      [equip weapon Great Wolf's right paw (+145), equip off-hand replica august scepter (+50)]
      [equip weapon Stick-Knife of Loathing (+130), equip off-hand replica august scepter (+50)]
      [equip weapon triple barreled barrel gun (+130)]
   */

  @Disabled("This takes time to run and is intended as an exploratory tool.")
  @CartesianTest
  public void theExploratoryTest(
      @CartesianTest.Values(ints = {1, 2, 3, 4, 5, 6}) int enumClassId,
      @CartesianTest.Values(strings = {"weapon damage", "ranged damage"}) String maxStringPart,
      @CartesianTest.Values(booleans = {true, false}) boolean withKnifeSkill,
      @CartesianTest.Values(booleans = {true}) boolean withTerseList,
      @CartesianTest.Values(booleans = {true, false}) boolean withEffective) {
    var cleanups = new Cleanups();
    AscensionClass thisClass = AscensionClass.find(enumClassId);
    cleanups.add(withClass(thisClass));
    cleanups.add(withStats(250, 250, 250));
    if (withKnifeSkill) {
      cleanups.add(withSkill("Tricky Knifework"));
    }
    Map<String, Integer> useThese = considerThese;
    if (withTerseList) {
      useThese = considerTheseTerse;
    }

    for (String it : useThese.keySet()) {
      cleanups.add(withItem(it));
    }
    String maxStr = maxStringPart;
    if (withEffective) {
      maxStr = maxStr + ",effective";
    }
    try (cleanups) {
      maximize(maxStr);
    }
    double score = Maximizer.best.getScore();
    List<Boost> boosts = getBoosts();
    String boostString = boosts.toString();
    usedBoosts.add(boostString);
    boolean weaponPicked = boostString.contains("equip weapon");
    boolean offPicked = boostString.contains("equip off-hand");
    if (!weaponPicked) {
      System.out.println(score + " " + thisClass + " " + withKnifeSkill + " " + maxStr);
      System.out.println(boosts);
    }
    if (!weaponPicked && offPicked) {
      System.out.println(score + " " + thisClass + " " + withKnifeSkill + " " + maxStr);
      System.out.println(boosts);
    }
  }

  @CartesianTest
  public void theRefinedTest(
      @CartesianTest.Values(ints = {1, 2, 3, 4, 5, 6}) int enumClassId,
      @CartesianTest.Values(strings = {"weapon damage", "ranged damage"}) String maxStringPart,
      @CartesianTest.Values(booleans = {true, false}) boolean withKnifeSkill,
      @CartesianTest.Values(booleans = {true, false}) boolean withEffective) {
    var cleanups = new Cleanups();
    AscensionClass thisClass = AscensionClass.find(enumClassId);
    cleanups.add(withClass(thisClass));
    cleanups.add(withStats(250, 250, 250));
    if (withKnifeSkill) {
      cleanups.add(withSkill("Tricky Knifework"));
    }
    Map<String, Integer> useThese = considerThese;
    cleanups.add(withItem(ItemPool.GRANNY_HACKLETONS_GATLING_GUN));
    cleanups.add(withItem(ItemPool.GREAT_WOLFS_RIGHT_PAW));
    cleanups.add(withItem(ItemPool.STICK_KNIFE_OF_LOATHING));
    cleanups.add(withItem(ItemPool.TRIPLE_BARRELLED_BARREL_GUN));
    cleanups.add(withItem(ItemPool.NINE_BALL));
    cleanups.add(withItem(ItemPool.LEFT_HANDED_MELODICA));
    cleanups.add(withItem(ItemPool.REPLICA_PATRIOT_SHIELD));
    cleanups.add(withItem(ItemPool.REPLICA_AUGUST_SCEPTER));
    String maxStr = maxStringPart;
    if (withEffective) {
      maxStr = maxStr + ",effective";
    }
    try (cleanups) {
      maximize(maxStr);
    }
    double score = Maximizer.best.getScore();
    List<Boost> boosts = getBoosts();
    String boostString = boosts.toString();
    usedBoosts.add(boostString);
    boolean weaponPicked = boostString.contains("equip weapon");
    boolean offPicked = boostString.contains("equip off-hand");
    if (!weaponPicked) {
      System.out.println(score + " " + thisClass + " " + withKnifeSkill + " " + maxStr);
      System.out.println(boosts);
    }
    if (!weaponPicked && offPicked) {
      System.out.println(score + " " + thisClass + " " + withKnifeSkill + " " + maxStr);
      System.out.println(boosts);
    }
  }

  /**
   * This looks at all equipment in the game and produces a list of weapons and offhand items. If a
   * terse list is desired then an item is only added of it has a higher power than anything of the
   * type (weapon or offhand) that has been previously added. The contents of the terse list are
   * dependent upon the order provided by ItemDatabase.dataNameEntrySet() but that really doesn't
   * matter since the terse list is intended be some kind of sample.
   *
   * @param terse - If true then the list of items is reduced.
   * @return A list of items to be made available for maximization.
   */
  private static Map<String, Integer> getItemsOfInterest(boolean terse) {
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
          if (terse) {
            if (power > weaponPower) {
              weaponPower = power;
              retVal.put(name, key);
            } else {
              retVal.put(name, key);
            }
          }
          break;
        case OFFHAND:
          if (terse) {
            if (power > offPower) {
              offPower = power;
              retVal.put(name, key);
            } else {
              retVal.put(name, key);
            }
          }
          break;
      }
    }
    return retVal;
  }
}
