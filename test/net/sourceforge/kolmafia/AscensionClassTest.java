package net.sourceforge.kolmafia;

import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withStats;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Enum;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

class AscensionClassTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("AscensionClassTest");
    Preferences.reset("AscensionClassTest");
  }

  @Test
  void allClassesDoesNotIncludeAstralSpirit() {
    assertThat(AscensionClass.allClasses(), not(hasItem(AscensionClass.ASTRAL_SPIRIT)));
  }

  @ParameterizedTest
  @CsvSource({
    ",",
    "\"\",",
    "AvAtArS oF bOrIs, AVATAR_OF_BORIS",
    "Avatars of Boris, AVATAR_OF_BORIS",
    "Smavatars of Shmoris,",
  })
  void canFindByPlural(final String name, final AscensionClass clazz) {
    assertThat(AscensionClass.findByPlural(name), equalTo(clazz));
  }

  @ParameterizedTest
  @CsvSource({
    ",",
    "\"\",",
    "AvAtAr Of BoRiS, AVATAR_OF_BORIS",
    "Avatar of Boris, AVATAR_OF_BORIS",
    "Smavatar of Shmoris,",
  })
  void canFindByName(final String name, final AscensionClass clazz) {
    assertThat(AscensionClass.find(name), equalTo(clazz));
  }

  @ParameterizedTest
  @CsvSource({
    "2, TURTLE_TAMER",
    "69696969,",
  })
  void canFindById(final int id, final AscensionClass clazz) {
    assertThat(AscensionClass.find(id), equalTo(clazz));
  }

  @ParameterizedTest
  @CsvSource({
    "ACCORDION_THIEF, Accordion Thieves",
    "ED, Eds the Undying",
    "AVATAR_OF_BORIS, Avatars of Boris",
    "SEAL_CLUBBER, Seal Clubbers"
  })
  void plurals(AscensionClass clazz, String plural) {
    assertThat(clazz.getPlural(), equalTo(plural));
  }

  @Test
  void canGetClassStun() {
    assertThat(AscensionClass.AVATAR_OF_BORIS.getStun(), is("Broadside"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void canGetDefaultStun(final boolean considerShadowNoodles) {
    var cleanups = new Cleanups(withProperty("considerShadowNoodles", considerShadowNoodles));
    try (cleanups) {
      assertThat(
          AscensionClass.DISCO_BANDIT.getStun(),
          equalTo(considerShadowNoodles ? "Shadow Noodles" : "none"));
    }
  }

  @Test
  void canGetImage() {
    assertThat(AscensionClass.ZOMBIE_MASTER.getImage(), is("tombstone"));
  }

  @ParameterizedTest
  @CsvSource({
    "SEAL_CLUBBER, " + ItemPool.SEAL_CLUB,
    "TURTLE_TAMER, " + ItemPool.TURTLE_TOTEM,
    "PASTAMANCER, " + ItemPool.PASTA_SPOON,
    "SAUCEROR, " + ItemPool.SAUCEPAN,
    "DISCO_BANDIT, " + ItemPool.DISCO_BALL,
    "ACCORDION_THIEF, " + ItemPool.STOLEN_ACCORDION,
    "VAMPYRE, -1"
  })
  void canGetDefaultWeapon(AscensionClass clazz, int weaponId) {
    assertThat(clazz.getStarterWeapon(), equalTo(weaponId));
  }

  @CartesianTest
  void mainStatBasedOnHighestStatForSomeClasses(
      @Values(booleans = {true, false}) boolean inClass,
      @Values(ints = {0, 1, 2}) int highestStat,
      @Enum(names = {"PLUMBER", "GREY_GOO"}) AscensionClass clazz) {
    var cleanups =
        new Cleanups(
            withStats(
                highestStat == 0 ? 100 : 0,
                highestStat == 1 ? 100 : 0,
                highestStat == 2 ? 100 : 0));

    // Should only be stateful if the player is currently that class
    if (inClass) cleanups.add(withClass(clazz));

    try (cleanups) {
      assertThat(clazz.getPrimeStatIndex(), equalTo(inClass ? highestStat : -1));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "TURTLE_TAMER, MUSCLE",
    "PASTAMANCER, MYSTICALITY",
    "DISCO_BANDIT, MOXIE",
    "PLUMBER, NONE"
  })
  void canGetMainStat(AscensionClass clazz, KoLConstants.Stat stat) {
    assertThat(clazz.getMainStat(), equalTo(stat));
  }

  @ParameterizedTest
  @CsvSource({
    "TURTLE_TAMER, MUSCLE",
    "PASTAMANCER, MYSTICALITY",
    "DISCO_BANDIT, MOXIE",
  })
  void canGetMainStatForCurrentClass(AscensionClass clazz, KoLConstants.Stat stat) {
    var cleanups = new Cleanups(withClass(clazz));

    try (cleanups) {
      assertThat(clazz.getMainStat(), equalTo(stat));
    }
  }

  @ParameterizedTest
  @CsvSource({"SAUCEROR, true", "ED, false"})
  void canTellStandardClasses(AscensionClass clazz, boolean expected) {
    assertThat(clazz.isStandard(), equalTo(expected));
  }

  @ParameterizedTest
  @CsvSource({"SAUCEROR, S", "SEAL_CLUBBER, SC", "ED, E", "AVATAR_OF_SNEAKY_PETE, AOSP"})
  void canInitialize(AscensionClass clazz, String expected) {
    assertThat(clazz.getInitials(), equalTo(expected));
  }

  @ParameterizedTest
  @CsvSource({"SAUCEROR, NONE", "ED, ACTUALLY_ED_THE_UNDYING", "GREY_GOO, GREY_YOU"})
  void canGetPath(AscensionClass clazz, Path expected) {
    assertThat(clazz.getPath(), equalTo(expected));
  }

  @ParameterizedTest
  @CsvSource({
    "COW_PUNCHER, 18000",
    "PLUMBER, 25000",
  })
  void canGetSkilLBase(AscensionClass clazz, int expected) {
    assertThat(clazz.getSkillBase(), equalTo(expected));
  }
}
