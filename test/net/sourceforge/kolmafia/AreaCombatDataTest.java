package net.sourceforge.kolmafia;

import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notANumber;
import static org.hamcrest.core.Every.everyItem;

import internal.helpers.Cleanups;
import java.io.File;
import java.util.Map;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.CrystalBallManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class AreaCombatDataTest {
  static final AreaCombatData SMUT_ORC_CAMP =
      AdventureDatabase.getAreaCombatData("The Smut Orc Logging Camp");
  static final MonsterData JACKER = MonsterDatabase.findMonster("smut orc jacker");
  static final MonsterData NAILER = MonsterDatabase.findMonster("smut orc nailer");
  static final MonsterData PIPELAYER = MonsterDatabase.findMonster("smut orc pipelayer");
  static final MonsterData SCREWER = MonsterDatabase.findMonster("smut orc screwer");
  static final MonsterData PERVERT = MonsterDatabase.findMonster("smut orc pervert");
  static final MonsterData SNAKE = MonsterDatabase.findMonster("The Frattlesnake");
  static final MonsterData GHOST = MonsterDatabase.findMonster("The ghost of Richard Cockingham");
  static final MonsterData EVIL_CULTIST = MonsterDatabase.findMonster("evil cultist");
  static final MonsterData CYBORG_POLICEMAN = MonsterDatabase.findMonster("cyborg policeman");
  static final MonsterData OBESE_TOURIST = MonsterDatabase.findMonster("obese tourist");
  static final MonsterData TERRIFYING_ROBOT = MonsterDatabase.findMonster("terrifying robot");
  static final AdventureResult MULTI_PASS = ItemPool.get(ItemPool.MULTI_PASS);

  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("fakeUserName");
    Preferences.reset("fakeUsername");
  }

  @AfterAll
  public static void deleteQueueFile() {
    File queueF = new File(KoLConstants.DATA_LOCATION, "fakeusername_queue.ser");
    if (queueF.exists()) {
      queueF.delete();
    }
  }

  @Test
  public void nonstatefulData() {
    Map<MonsterData, Double> appearanceRates = SMUT_ORC_CAMP.getMonsterData();

    assertThat(
        appearanceRates,
        allOf(
            aMapWithSize(7),
            hasEntry(JACKER, 25.0),
            hasEntry(NAILER, 25.0),
            hasEntry(PIPELAYER, 25.0),
            hasEntry(SCREWER, 25.0),
            hasEntry(PERVERT, 0.0),
            hasEntry(SNAKE, 0.0),
            hasEntry(GHOST, 0.0)));
  }

  @Test
  public void saberCopy() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.BADGER),
            withEquipped(EquipmentManager.FAMILIAR, "miniature crystal ball"));
    try (cleanups) {
      Preferences.setString("_saberForceMonster", "smut orc screwer");
      Preferences.setInteger("_saberForceMonsterCount", 3);

      // Should override a crystal ball prediction
      Preferences.setString(
          "crystalBallPredictions", "0:" + SMUT_ORC_CAMP.getZone() + ":smut orc jacker");
      CrystalBallManager.reset();

      Map<MonsterData, Double> appearanceRates = SMUT_ORC_CAMP.getMonsterData(true);

      assertThat(
          appearanceRates,
          allOf(
              aMapWithSize(7),
              hasEntry(JACKER, 0.0),
              hasEntry(NAILER, 0.0),
              hasEntry(PIPELAYER, 0.0),
              hasEntry(SCREWER, 100.0),
              hasEntry(PERVERT, 0.0),
              hasEntry(SNAKE, 0.0),
              hasEntry(GHOST, 0.0)));
    }
  }

  @Test
  public void crystalBallPrediction() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.BADGER),
            withEquipped(EquipmentManager.FAMILIAR, "miniature crystal ball"));
    try (cleanups) {
      Preferences.setString(
          "crystalBallPredictions", "0:" + SMUT_ORC_CAMP.getZone() + ":smut orc nailer");
      CrystalBallManager.reset();

      Map<MonsterData, Double> appearanceRates = SMUT_ORC_CAMP.getMonsterData(true);

      assertThat(
          appearanceRates,
          allOf(
              aMapWithSize(7),
              hasEntry(JACKER, 0.0),
              hasEntry(NAILER, 100.0),
              hasEntry(PIPELAYER, 0.0),
              hasEntry(SCREWER, 0.0),
              hasEntry(PERVERT, 0.0),
              hasEntry(SNAKE, 0.0),
              hasEntry(GHOST, 0.0)));
    }
  }

  @Test
  public void crystalBallPredictionWhenNCIsUp() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.BADGER),
            withEquipped(EquipmentManager.FAMILIAR, "miniature crystal ball"));
    try (cleanups) {
      Preferences.setString(
          "crystalBallPredictions", "0:" + SMUT_ORC_CAMP.getZone() + ":smut orc nailer");
      CrystalBallManager.reset();
      Preferences.setInteger("smutOrcNoncombatProgress", 15);

      Map<MonsterData, Double> appearanceRates = SMUT_ORC_CAMP.getMonsterData(true);

      assertThat(
          appearanceRates,
          allOf(
              aMapWithSize(7),
              hasEntry(JACKER, 0.0),
              hasEntry(NAILER, 0.0),
              hasEntry(PIPELAYER, 0.0),
              hasEntry(SCREWER, 0.0),
              hasEntry(PERVERT, 0.0),
              hasEntry(SNAKE, 0.0),
              hasEntry(GHOST, 0.0)));
    }
  }

  @Test
  public void olfaction() {
    AdventureQueueDatabase.resetQueue();
    KoLConstants.activeEffects.add(EffectPool.get(EffectPool.ON_THE_TRAIL));
    Preferences.setString("olfactedMonster", "smut orc pipelayer");

    Map<MonsterData, Double> appearanceRates = SMUT_ORC_CAMP.getMonsterData(true);

    assertThat(
        appearanceRates,
        allOf(
            aMapWithSize(7),
            hasEntry(JACKER, 400 / 28.0),
            hasEntry(NAILER, 400 / 28.0),
            hasEntry(PIPELAYER, 1600 / 28.0),
            hasEntry(SCREWER, 400 / 28.0),
            hasEntry(PERVERT, 0.0),
            hasEntry(SNAKE, 0.0),
            hasEntry(GHOST, 0.0)));
  }

  @Test
  public void clumsinessGrove() {
    AdventureQueueDatabase.resetQueue();

    Preferences.setString("clumsinessGroveBoss", "The Bat in the Spats");

    Map<MonsterData, Double> appearanceRates =
        AdventureDatabase.getAreaCombatData("The Clumsiness Grove").getMonsterData(true);

    assertThat(
        appearanceRates,
        allOf(
            aMapWithSize(7),
            hasEntry(
                equalTo(MonsterDatabase.findMonster("The Bat in the Spats")),
                closeTo(100f / 6, 0.001)),
            hasEntry(MonsterDatabase.findMonster("The Thorax"), 0.0)));
  }

  @Test
  public void maelstromOfLovers() {
    AdventureQueueDatabase.resetQueue();

    Preferences.setString("maelstromOfLoversBoss", "The Terrible Pinch");

    Map<MonsterData, Double> appearanceRates =
        AdventureDatabase.getAreaCombatData("The Maelstrom of Lovers").getMonsterData(true);

    assertThat(
        appearanceRates,
        allOf(
            aMapWithSize(7),
            hasEntry(
                equalTo(MonsterDatabase.findMonster("The Terrible Pinch")),
                closeTo(100f / 6, 0.001)),
            hasEntry(MonsterDatabase.findMonster("Thug 1 and Thug 2"), 0.0)));
  }

  @Test
  public void glacierOfJerks() {
    AdventureQueueDatabase.resetQueue();

    Preferences.setString("glacierOfJerksBoss", "The Large-Bellied Snitch");

    Map<MonsterData, Double> appearanceRates =
        AdventureDatabase.getAreaCombatData("The Glacier of Jerks").getMonsterData(true);

    assertThat(
        appearanceRates,
        allOf(
            aMapWithSize(7),
            hasEntry(MonsterDatabase.findMonster("Mammon the Elephant"), 0.0),
            hasEntry(
                equalTo(MonsterDatabase.findMonster("The Large-Bellied Snitch")),
                closeTo(100f / 6, 0.001))));
  }

  @Test
  public void junglesOfAncientLoathing() {
    AdventureQueueDatabase.resetQueue();

    QuestDatabase.setQuestProgress(Quest.PRIMORDIAL, QuestDatabase.STARTED);

    Map<MonsterData, Double> appearanceRates =
        AdventureDatabase.getAreaCombatData("The Jungles of Ancient Loathing").getMonsterData(true);

    assertThat(appearanceRates.get(EVIL_CULTIST), equalTo(0.0));

    QuestDatabase.setQuestProgress(Quest.PRIMORDIAL, QuestDatabase.FINISHED);

    appearanceRates =
        AdventureDatabase.getAreaCombatData("The Jungles of Ancient Loathing").getMonsterData(true);

    assertThat(appearanceRates.get(EVIL_CULTIST), greaterThan(0.0));
  }

  @Test
  public void seasideMegalopolis() {
    AdventureQueueDatabase.resetQueue();

    QuestDatabase.setQuestProgress(Quest.FUTURE, QuestDatabase.STARTED);

    Map<MonsterData, Double> appearanceRates =
        AdventureDatabase.getAreaCombatData("Seaside Megalopolis").getMonsterData(true);

    assertThat(
        appearanceRates,
        allOf(
            hasEntry(CYBORG_POLICEMAN, 0.0),
            hasEntry(OBESE_TOURIST, 0.0),
            hasEntry(TERRIFYING_ROBOT, 0.0)));

    ResultProcessor.processResult(true, MULTI_PASS);

    appearanceRates =
        AdventureDatabase.getAreaCombatData("Seaside Megalopolis").getMonsterData(true);

    assertThat(appearanceRates.get(CYBORG_POLICEMAN), greaterThan(0.0));
    assertThat(appearanceRates.get(OBESE_TOURIST), equalTo(0.0));
    assertThat(appearanceRates.get(TERRIFYING_ROBOT), equalTo(0.0));

    QuestDatabase.setQuestProgress(Quest.FUTURE, "step2");

    appearanceRates =
        AdventureDatabase.getAreaCombatData("Seaside Megalopolis").getMonsterData(true);

    assertThat(appearanceRates.get(CYBORG_POLICEMAN), greaterThan(0.0));
    assertThat(appearanceRates.get(OBESE_TOURIST), greaterThan(0.0));
    assertThat(appearanceRates.get(TERRIFYING_ROBOT), greaterThan(0.0));

    QuestDatabase.setQuestProgress(Quest.FUTURE, QuestDatabase.FINISHED);

    appearanceRates =
        AdventureDatabase.getAreaCombatData("Seaside Megalopolis").getMonsterData(true);

    assertThat(appearanceRates.get(CYBORG_POLICEMAN), equalTo(0.0));
    assertThat(appearanceRates.get(OBESE_TOURIST), greaterThan(0.0));
    assertThat(appearanceRates.get(TERRIFYING_ROBOT), greaterThan(0.0));
  }

  @Nested
  class ZeroTotalWeighting {
    @Test
    public void canGetAppearanceRateForZeroTotalWeighting() {
      var appearanceRates = AdventureDatabase.getAreaCombatData("The Hedge Maze").getMonsterData();

      assertThat(appearanceRates, aMapWithSize(4));
      assertThat(appearanceRates.values(), everyItem(not(notANumber())));
    }

    @Test
    public void canGetAverageMLForZeroTotalWeighting() {
      var ml = AdventureDatabase.getAreaCombatData("The Hedge Maze").getAverageML();

      assertThat(ml, not(notANumber()));
    }
  }

  @Nested
  class Cyrpt {
    @ParameterizedTest
    @ValueSource(strings = {"Alcove", "Cranny", "Niche", "Nook"})
    public void certainCombatChanceIfEvilNotMoreThanLimit(String subZone) {
      String zone = "The Defiled " + subZone;
      String property = "cyrpt" + subZone + "Evilness";
      var cleanups = withProperty(property, 13);

      try (cleanups) {
        var data = AdventureDatabase.getAreaCombatData(zone);

        assertThat(data.areaCombatPercent(), equalTo(100.0));
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Alcove", "Cranny", "Niche", "Nook"})
    public void givenCombatChanceIfEvilMoreThanLimit(String subZone) {
      String zone = "The Defiled " + subZone;
      String property = "cyrpt" + subZone + "Evilness";
      var cleanups = withProperty(property, 14);

      try (cleanups) {
        var data = AdventureDatabase.getAreaCombatData(zone);

        assertThat(data.areaCombatPercent(), equalTo(85.0));
      }
    }

    @Nested
    class Alcove {
      @Test
      public void onlyBossIfEvilNotMoreThanLimit() {
        var cleanups = withProperty("cyrptAlcoveEvilness", 13);

        try (cleanups) {
          Map<MonsterData, Double> appearanceRates =
              AdventureDatabase.getAreaCombatData("The Defiled Alcove").getMonsterData(true);

          assertThat(
              appearanceRates,
              allOf(
                  aMapWithSize(4),
                  hasEntry(MonsterDatabase.findMonster("conjoined zmombie"), 100.0),
                  hasEntry(MonsterDatabase.findMonster("corpulent zobmie"), 0.0),
                  hasEntry(MonsterDatabase.findMonster("grave rober zmobie"), 0.0),
                  hasEntry(MonsterDatabase.findMonster("modern zmobie"), 0.0)));
        }
      }

      @Test
      public void enemiesIfEvilMoreThanLimit() {
        var cleanups = withProperty("cyrptAlcoveEvilness", 14);

        try (cleanups) {
          Map<MonsterData, Double> appearanceRates =
              AdventureDatabase.getAreaCombatData("The Defiled Alcove").getMonsterData(true);

          assertThat(
              appearanceRates,
              allOf(
                  aMapWithSize(4),
                  hasEntry(MonsterDatabase.findMonster("conjoined zmombie"), 0.0),
                  hasEntry(MonsterDatabase.findMonster("corpulent zobmie"), 42.5 * 0.85),
                  hasEntry(MonsterDatabase.findMonster("grave rober zmobie"), 42.5 * 0.85),
                  hasEntry(MonsterDatabase.findMonster("modern zmobie"), 15.0)));
        }
      }
    }

    @Nested
    class Cranny {
      @Test
      public void onlyBossIfEvilNotMoreThanLimit() {
        var cleanups = withProperty("cyrptCrannyEvilness", 13);

        try (cleanups) {
          Map<MonsterData, Double> appearanceRates =
              AdventureDatabase.getAreaCombatData("The Defiled Cranny").getMonsterData(true);

          assertThat(
              appearanceRates,
              allOf(
                  aMapWithSize(greaterThanOrEqualTo(3)),
                  hasEntry(MonsterDatabase.findMonster("huge ghuol"), 100.0),
                  hasEntry(MonsterDatabase.findMonster("gaunt ghuol"), 0.0),
                  hasEntry(MonsterDatabase.findMonster("gluttonous ghuol"), 0.0)));
        }
      }

      @Test
      public void enemiesIfEvilMoreThanLimit() {
        var cleanups = withProperty("cyrptCrannyEvilness", 14);

        try (cleanups) {
          Map<MonsterData, Double> appearanceRates =
              AdventureDatabase.getAreaCombatData("The Defiled Cranny").getMonsterData(true);

          assertThat(
              appearanceRates,
              allOf(
                  aMapWithSize(greaterThanOrEqualTo(3)),
                  hasEntry(MonsterDatabase.findMonster("huge ghuol"), 0.0),
                  hasEntry(MonsterDatabase.findMonster("gaunt ghuol"), 50.0 * 0.85),
                  hasEntry(MonsterDatabase.findMonster("gluttonous ghuol"), 50.0 * 0.85)));
        }
      }
    }

    @Nested
    class Niche {
      @Test
      public void onlyBossIfEvilNotMoreThanLimit() {
        var cleanups = withProperty("cyrptNicheEvilness", 13);

        try (cleanups) {
          Map<MonsterData, Double> appearanceRates =
              AdventureDatabase.getAreaCombatData("The Defiled Niche").getMonsterData(true);

          assertThat(
              appearanceRates,
              allOf(
                  aMapWithSize(5),
                  hasEntry(MonsterDatabase.findMonster("gargantulihc"), 100.0),
                  hasEntry(MonsterDatabase.findMonster("basic lihc"), 0.0),
                  hasEntry(MonsterDatabase.findMonster("dirty old lihc"), 0.0),
                  hasEntry(MonsterDatabase.findMonster("senile lihc"), 0.0),
                  hasEntry(MonsterDatabase.findMonster("slick lihc"), 0.0)));
        }
      }

      @Test
      public void enemiesIfEvilMoreThanLimit() {
        var cleanups = withProperty("cyrptNicheEvilness", 14);

        try (cleanups) {
          Map<MonsterData, Double> appearanceRates =
              AdventureDatabase.getAreaCombatData("The Defiled Niche").getMonsterData(true);

          assertThat(
              appearanceRates,
              allOf(
                  aMapWithSize(5),
                  hasEntry(MonsterDatabase.findMonster("gargantulihc"), 0.0),
                  hasEntry(MonsterDatabase.findMonster("basic lihc"), 25.0 * 0.85),
                  hasEntry(MonsterDatabase.findMonster("dirty old lihc"), 25.0 * 0.85),
                  hasEntry(MonsterDatabase.findMonster("senile lihc"), 25.0 * 0.85),
                  hasEntry(MonsterDatabase.findMonster("slick lihc"), 25.0 * 0.85)));
        }
      }
    }

    @Nested
    class Nook {
      @Test
      public void onlyBossIfEvilNotMoreThanLimit() {
        var cleanups = withProperty("cyrptNookEvilness", 13);

        try (cleanups) {
          Map<MonsterData, Double> appearanceRates =
              AdventureDatabase.getAreaCombatData("The Defiled Nook").getMonsterData(true);

          assertThat(
              appearanceRates,
              allOf(
                  aMapWithSize(4),
                  hasEntry(MonsterDatabase.findMonster("giant skeelton"), 100.0),
                  hasEntry(MonsterDatabase.findMonster("party skelteon"), 0.0),
                  hasEntry(MonsterDatabase.findMonster("spiny skelelton"), 0.0),
                  hasEntry(MonsterDatabase.findMonster("toothy sklelton"), 0.0)));
        }
      }

      @Test
      public void enemiesIfEvilMoreThanLimit() {
        var cleanups = withProperty("cyrptNookEvilness", 14);

        try (cleanups) {
          Map<MonsterData, Double> appearanceRates =
              AdventureDatabase.getAreaCombatData("The Defiled Nook").getMonsterData(true);

          assertThat(
              appearanceRates,
              allOf(
                  aMapWithSize(4),
                  hasEntry(MonsterDatabase.findMonster("giant skeelton"), 0.0),
                  hasEntry(
                      equalTo(MonsterDatabase.findMonster("party skelteon")),
                      closeTo(100f / 3 * 0.85, 0.001)),
                  hasEntry(
                      equalTo(MonsterDatabase.findMonster("spiny skelelton")),
                      closeTo(100f / 3 * 0.85, 0.001)),
                  hasEntry(
                      equalTo(MonsterDatabase.findMonster("toothy sklelton")),
                      closeTo(100f / 3 * 0.85, 0.001))));
        }
      }
    }
  }
}
