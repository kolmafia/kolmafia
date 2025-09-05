package net.sourceforge.kolmafia;

import static internal.helpers.Player.withBanishedMonsters;
import static internal.helpers.Player.withBanishedPhyla;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withCurrentRun;
import static internal.helpers.Player.withDay;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withTrackedMonsters;
import static internal.helpers.Player.withTrackedPhyla;
import static internal.helpers.Player.withTurnsPlayed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notANumber;
import static org.hamcrest.core.Every.everyItem;

import internal.helpers.Cleanups;
import java.io.File;
import java.time.Month;
import java.util.Map;
import net.sourceforge.kolmafia.equipment.Slot;
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
import net.sourceforge.kolmafia.session.ResultProcessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

  static final String BATTLEFIELD_FRATUNIFORM = "The Battlefield (Frat Uniform)";
  static final String BATTLEFIELD_HIPPYUNIFORM = "The Battlefield (Hippy Uniform)";
  static final MonsterData FRATBOY_BOSS = MonsterDatabase.findMonster("The Man");
  static final MonsterData HIPPY_BOSS = MonsterDatabase.findMonster("The Big Wisniewski");
  static final MonsterData FRATBOY_JUNKYARD =
      MonsterDatabase.findMonster("War Frat Mobile Grill Unit");
  static final MonsterData HIPPY_JUNKYARD = MonsterDatabase.findMonster("Bailey's Beetle");
  static final Map<MonsterData, Integer> HIPPY_DEFEATED =
      Map.ofEntries(
          Map.entry(MonsterDatabase.findMonster("Mobile Armored Sweat Lodge"), 151),
          Map.entry(MonsterDatabase.findMonster("Green Ops Soldier"), 401),
          Map.entry(MonsterDatabase.findMonster("Slow Talkin' Elliot"), 501),
          Map.entry(MonsterDatabase.findMonster("Neil"), 601),
          Map.entry(MonsterDatabase.findMonster("Zim Merman"), 701),
          Map.entry(MonsterDatabase.findMonster("C.A.R.N.I.V.O.R.E. Operative"), 801),
          Map.entry(MonsterDatabase.findMonster("Glass of Orange Juice"), 901));
  static final Map<MonsterData, Integer> FRATBOY_DEFEATED =
      Map.ofEntries(
          Map.entry(MonsterDatabase.findMonster("Sorority Operator"), 151),
          Map.entry(MonsterDatabase.findMonster("Panty Raider Frat Boy"), 401),
          Map.entry(MonsterDatabase.findMonster("Next-generation Frat Boy"), 501),
          Map.entry(MonsterDatabase.findMonster("Monty Basingstoke-Pratt, IV"), 601),
          Map.entry(MonsterDatabase.findMonster("Brutus, the toga-clad lout"), 701),
          Map.entry(MonsterDatabase.findMonster("Danglin' Chad"), 801),
          Map.entry(MonsterDatabase.findMonster("War Frat Streaker"), 901));

  private static final String TESTUSERNAME = "AreaCombatDataTestUser";

  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset(TESTUSERNAME);
    Preferences.reset(TESTUSERNAME);
    AdventureQueueDatabase.allowSerializationWrite = false;
  }

  @AfterAll
  public static void deleteQueueFile() {
    File queueF = new File(KoLConstants.DATA_LOCATION, TESTUSERNAME.toLowerCase() + "_queue.ser");
    if (queueF.exists()) {
      queueF.delete();
    }
    AdventureQueueDatabase.allowSerializationWrite = true;
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
  public void nonstatefulDataWithNonzeroCombatRate() {
    try (var cleanup = withEffect(EffectPool.TAUNT_OF_HORUS)) {
      Map<MonsterData, Double> appearanceRates =
          AdventureDatabase.getAreaCombatData("Sonofa Beach").getMonsterData();
      assertThat(appearanceRates, hasEntry(MonsterDatabase.findMonster("lobsterfrogman"), 30.0));
    }
  }

  @Test
  public void saberCopy() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.BADGER),
            withEquipped(Slot.FAMILIAR, "miniature crystal ball"));
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
            withEquipped(Slot.FAMILIAR, "miniature crystal ball"));
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
            withEquipped(Slot.FAMILIAR, "miniature crystal ball"));
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
    var cleanups =
        new Cleanups(
            withCurrentRun(30),
            withEffect(EffectPool.ON_THE_TRAIL),
            withTrackedMonsters("smut orc pipelayer:Transcendent Olfaction:26"));

    try (cleanups) {
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
  }

  @Nested
  class NosyNose {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void onlyAppliesIfNosyNoseActive(boolean active) {
      var cleanups =
          new Cleanups(
              withCurrentRun(30),
              withFamiliar(active ? FamiliarPool.NOSY_NOSE : FamiliarPool.MOSQUITO),
              withTrackedMonsters("swamp duck:Nosy Nose:26"));

      try (cleanups) {
        var bog = AdventureDatabase.getAreaCombatData("McMillicancuddy's Bog");
        Map<MonsterData, Double> appearanceRates = bog.getMonsterData(true);

        assertThat(
            appearanceRates,
            allOf(
                aMapWithSize(2),
                hasEntry(MonsterDatabase.findMonster("swamp duck"), active ? 200 / 3.0 : 50.0)));
      }
    }
  }

  @Test
  public void banishOverridesTrack() {
    var cleanups =
        new Cleanups(
            withTrackedMonsters(
                "smut orc nailer:Gallapagosian Mating Call:1:smut orc pipelayer:Transcendent Olfaction:2"),
            withBanishedMonsters("smut orc pipelayer:snokebomb:2"));

    try (cleanups) {
      Map<MonsterData, Double> appearanceRates = SMUT_ORC_CAMP.getMonsterData(true);

      assertThat(
          appearanceRates,
          allOf(
              hasEntry(JACKER, 25.0),
              hasEntry(NAILER, 50.0),
              hasEntry(PIPELAYER, -3.0),
              hasEntry(SCREWER, 25.0)));
    }
  }

  @Test
  public void battlefieldHippyUniform() {
    AdventureQueueDatabase.resetQueue();

    // Defeated count
    for (var entry : FRATBOY_DEFEATED.entrySet()) {
      Preferences.setInteger("fratboysDefeated", 0);
      assertThat(
          AdventureDatabase.getAreaCombatData(BATTLEFIELD_HIPPYUNIFORM)
              .getMonsterData(true)
              .get(entry.getKey()),
          equalTo(0.0));
      Preferences.setInteger("fratboysDefeated", entry.getValue());
      assertThat(
          AdventureDatabase.getAreaCombatData(BATTLEFIELD_HIPPYUNIFORM)
              .getMonsterData(true)
              .get(entry.getKey()),
          not(0.0));
    }

    // Boss
    Preferences.setInteger("fratboysDefeated", 999);
    assertThat(
        AdventureDatabase.getAreaCombatData(BATTLEFIELD_HIPPYUNIFORM)
            .getMonsterData(true)
            .get(FRATBOY_BOSS),
        equalTo(0.0));
    Preferences.setInteger("fratboysDefeated", 1000);
    assertThat(
        AdventureDatabase.getAreaCombatData(BATTLEFIELD_HIPPYUNIFORM)
            .getMonsterData(true)
            .get(FRATBOY_BOSS),
        equalTo(100.0));

    // Junkyard sidequest state
    Preferences.setInteger("fratboysDefeated", 0);
    Preferences.setString("sidequestJunkyardCompleted", "fratboy");
    assertThat(
        AdventureDatabase.getAreaCombatData(BATTLEFIELD_HIPPYUNIFORM)
            .getMonsterData(true)
            .get(FRATBOY_JUNKYARD),
        greaterThan(0.0));

    Preferences.setString("sidequestJunkyardCompleted", "hippy");
    assertThat(
        AdventureDatabase.getAreaCombatData(BATTLEFIELD_HIPPYUNIFORM)
            .getMonsterData(true)
            .get(FRATBOY_JUNKYARD),
        equalTo(0.0));
  }

  @Test
  public void battlefieldFratUniform() {
    AdventureQueueDatabase.resetQueue();

    // Defeated count
    for (var entry : HIPPY_DEFEATED.entrySet()) {
      Preferences.setInteger("hippiesDefeated", 0);
      assertThat(
          AdventureDatabase.getAreaCombatData(BATTLEFIELD_FRATUNIFORM)
              .getMonsterData(true)
              .get(entry.getKey()),
          equalTo(0.0));
      Preferences.setInteger("hippiesDefeated", entry.getValue());
      assertThat(
          AdventureDatabase.getAreaCombatData(BATTLEFIELD_FRATUNIFORM)
              .getMonsterData(true)
              .get(entry.getKey()),
          not(0.0));
    }

    // Boss
    Preferences.setInteger("hippiesDefeated", 999);
    assertThat(
        AdventureDatabase.getAreaCombatData(BATTLEFIELD_FRATUNIFORM)
            .getMonsterData(true)
            .get(HIPPY_BOSS),
        equalTo(0.0));
    Preferences.setInteger("hippiesDefeated", 1000);
    assertThat(
        AdventureDatabase.getAreaCombatData(BATTLEFIELD_FRATUNIFORM)
            .getMonsterData(true)
            .get(HIPPY_BOSS),
        equalTo(100.0));

    // Junkyard sidequest state
    Preferences.setInteger("hippiesDefeated", 0);
    Preferences.setString("sidequestJunkyardCompleted", "hippy");
    assertThat(
        AdventureDatabase.getAreaCombatData(BATTLEFIELD_FRATUNIFORM)
            .getMonsterData(true)
            .get(HIPPY_JUNKYARD),
        greaterThan(0.0));

    Preferences.setString("sidequestJunkyardCompleted", "fratboy");
    assertThat(
        AdventureDatabase.getAreaCombatData(BATTLEFIELD_FRATUNIFORM)
            .getMonsterData(true)
            .get(HIPPY_JUNKYARD),
        equalTo(0.0));
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
  class SmutOrcLoggingCamp {
    @Test
    public void blechHouseDoesNotHappenAfterFinishingBridge() {
      var cleanups =
          new Cleanups(
              withProperty("smutOrcNoncombatProgress", 15), // Blech House is up
              withProperty("chasmBridgeProgress", 30) // ...but bridge is finished
              );
      try (cleanups) {
        assertThat(SMUT_ORC_CAMP.areaCombatPercent(), equalTo(100.0));
      }
    }

    @Test
    public void blechHouseHappensWhenEnoughProgressIsReached() {
      var cleanups =
          new Cleanups(
              withProperty("smutOrcNoncombatProgress", 16), // Blech House is up
              withProperty("chasmBridgeProgress", 26) // and bridge is not finished yet
              );
      try (cleanups) {
        assertThat(SMUT_ORC_CAMP.areaCombatPercent(), equalTo(0.0));
      }
    }

    @Test
    public void blechHouseDoesNotHappenWhenNotEnoughProgressIsReached() {
      var cleanups =
          new Cleanups(
              withProperty("smutOrcNoncombatProgress", 12) // Blech House is not yet up
              );
      try (cleanups) {
        assertThat(SMUT_ORC_CAMP.areaCombatPercent(), equalTo(100.0));
      }
    }
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

  @Nested
  class ShadowRifts {
    @Test
    public void ingressPointAffectsAvailableMonsters() {
      var cleanups = withProperty("shadowRiftIngress", "manor3");

      try (cleanups) {
        Map<MonsterData, Double> appearanceRates =
            AdventureDatabase.getAreaCombatData("Shadow Rift").getMonsterData(true);

        assertThat(
            appearanceRates,
            allOf(
                aMapWithSize(12),
                hasEntry(
                    equalTo(MonsterDatabase.findMonster("shadow bat")), closeTo(100f / 3, 0.001)),
                hasEntry(
                    equalTo(MonsterDatabase.findMonster("shadow devil")), closeTo(100f / 3, 0.001)),
                hasEntry(
                    equalTo(MonsterDatabase.findMonster("shadow spider")),
                    closeTo(100f / 3, 0.001)),
                hasEntry(MonsterDatabase.findMonster("shadow cow"), -4.0),
                hasEntry(MonsterDatabase.findMonster("shadow guy"), -4.0),
                hasEntry(MonsterDatabase.findMonster("shadow hexagon"), -4.0),
                hasEntry(MonsterDatabase.findMonster("shadow orb"), -4.0),
                hasEntry(MonsterDatabase.findMonster("shadow prism"), -4.0),
                hasEntry(MonsterDatabase.findMonster("shadow slab"), -4.0),
                hasEntry(MonsterDatabase.findMonster("shadow snake"), -4.0),
                hasEntry(MonsterDatabase.findMonster("shadow stalk"), -4.0),
                hasEntry(MonsterDatabase.findMonster("shadow tree"), -4.0)));
      }
    }
  }

  @Nested
  class ItemDrops {
    @Test
    void shouldShowItemDrops() {
      var funHouse = AdventureDatabase.getAreaCombatData("The \"Fun\" House");

      var data = funHouse.toString(true);
      assertThat(data, containsString("bloody clown pants 10%"));
      assertThat(data, containsString("polka-dot bow tie (pickpocket only, cannot steal)"));
      assertThat(data, containsString("empty greasepaint tube (bounty)"));
    }

    @Test
    void shouldShowPickpocketRatesIfCanPickpocket() {
      var cleanups = withClass(AscensionClass.ACCORDION_THIEF);

      try (cleanups) {
        var funHouse = AdventureDatabase.getAreaCombatData("The \"Fun\" House");

        var data = funHouse.toString(true);
        assertThat(data, containsString("bloody clown pants 16% (7% steal, 10% drop)"));
        assertThat(data, containsString("polka-dot bow tie 5.0% (pickpocket only)"));
        assertThat(data, containsString("empty greasepaint tube (bounty)"));
      }
    }

    @Test
    void cookingIngredientsAreAffectedByFoodDrops() {
      var cleanups = withEffect(EffectPool.WHET_APPETITE);

      try (cleanups) {
        var lab = AdventureDatabase.getAreaCombatData("Cobb's Knob Laboratory");

        var data = lab.toString(true);
        assertThat(data, containsString("scrumptious reagent 50%"));
      }
    }

    @Test
    void mixingIngredientsAreAffectedByBoozeDrops() {
      var cleanups = withEffect(EffectPool.INFERNAL_THIRST);

      try (cleanups) {
        var iceHotel = AdventureDatabase.getAreaCombatData("The Ice Hotel");

        var data = iceHotel.toString(true);
        assertThat(data, containsString("perfect ice cube 90%"));
      }
    }
  }

  @Nested
  class RedWhiteBlueBlast {
    @Test
    public void simpleBlastBanishesOtherMonsters() {
      var cleanups =
          new Cleanups(
              withProperty("rwbLocation", "The Smut Orc Logging Camp"),
              withProperty("rwbMonster", "smut orc jacker"),
              withProperty("rwbMonsterCount", 2));

      try (cleanups) {
        Map<MonsterData, Double> appearanceRates = SMUT_ORC_CAMP.getMonsterData(true);

        assertThat(
            appearanceRates,
            allOf(
                hasEntry(JACKER, 100.0),
                hasEntry(NAILER, -3.0),
                hasEntry(PIPELAYER, -3.0),
                hasEntry(SCREWER, -3.0)));
      }
    }

    @Test
    public void blastForcesWithOtherCopies() {
      var cleanups =
          new Cleanups(
              withProperty("rwbLocation", "The Smut Orc Logging Camp"),
              withProperty("rwbMonster", "smut orc jacker"),
              withProperty("rwbMonsterCount", 2),
              withTrackedMonsters("smut orc nailer:Gallapagosian Mating Call:1"));

      try (cleanups) {
        Map<MonsterData, Double> appearanceRates = SMUT_ORC_CAMP.getMonsterData(true);

        assertThat(
            appearanceRates,
            allOf(
                hasEntry(JACKER, 100.0),
                hasEntry(NAILER, -3.0),
                hasEntry(PIPELAYER, -3.0),
                hasEntry(SCREWER, -3.0)));
      }
    }
  }

  @Nested
  class PatrioticScreech {
    @Test
    public void screechBanishesPhylumCopies() {
      var cleanups =
          new Cleanups(
              withCurrentRun(30),
              withBanishedPhyla("dude:Patriotic Screech:25"),
              withBanishedMonsters("bearpig topiary animal:snokebomb:26"),
              withEffect(EffectPool.TAUNT_OF_HORUS));

      try (cleanups) {
        var twinPeak = AdventureDatabase.getAreaCombatData("Twin Peak");
        Map<MonsterData, Double> appearanceRates = twinPeak.getMonsterData(true);

        assertThat(appearanceRates, aMapWithSize(8));
        assertThat(
            appearanceRates,
            allOf(
                hasEntry(MonsterDatabase.findMonster("bearpig topiary animal"), -3.0),
                hasEntry(MonsterDatabase.findMonster("elephant (meatcar?) topiary animal"), 50.0),
                hasEntry(MonsterDatabase.findMonster("spider (duck?) topiary animal"), 50.0),
                hasEntry(MonsterDatabase.findMonster("Big Wheelin' Twins"), -3.0),
                hasEntry(MonsterDatabase.findMonster("Bubblemint Twins"), -3.0),
                hasEntry(MonsterDatabase.findMonster("Creepy Ginger Twin"), -3.0),
                hasEntry(MonsterDatabase.findMonster("Mismatched Twins"), -3.0),
                hasEntry(MonsterDatabase.findMonster("Troll Twins"), -3.0)));
      }
    }
  }

  @Nested
  class PhylumTrack {
    @Test
    public void trackedPhylaStackWithTrackedMonsters() {
      var cleanups =
          new Cleanups(
              withCurrentRun(30),
              withEffect(EffectPool.A_BEASTLY_ODOR),
              withEffect(EffectPool.EW_THE_HUMANITY),
              withTrackedPhyla("dude:Ew, The Humanity:25:beast:A Beastly Odor:25"),
              withTrackedMonsters("eagle:Gallapagosian Mating Call:26"),
              withEffect(EffectPool.TAUNT_OF_HORUS));

      try (cleanups) {
        var zone = AdventureDatabase.getAreaCombatData("A Mob of Zeppelin Protesters");
        Map<MonsterData, Double> appearanceRates = zone.getMonsterData(true);

        assertThat(appearanceRates, aMapWithSize(6));
        assertThat(
            appearanceRates,
            allOf(
                hasEntry(MonsterDatabase.findMonster("Blue Oyster cultist"), 300.0 / 16.0),
                hasEntry(MonsterDatabase.findMonster("eagle"), 400.0 / 16.0),
                hasEntry(MonsterDatabase.findMonster("fleet woodsman"), 300.0 / 16.0),
                hasEntry(MonsterDatabase.findMonster("Jefferson pilot"), 300.0 / 16.0),
                hasEntry(MonsterDatabase.findMonster("lynyrd skinner"), 300.0 / 16.0),
                hasEntry(MonsterDatabase.findMonster("The Nuge"), -3.0)));
      }
    }
  }

  @Nested
  class Spaaaace {
    @ParameterizedTest
    @CsvSource({
      "15, 0.25",
      "2, 0.0",
      "22, 0.37",
    })
    void alienAppearanceAffectedByRonaldLight(final int dayOfJune2024, final double alienWeight) {
      var cleanups = withDay(2024, Month.JUNE, dayOfJune2024);

      try (cleanups) {
        var zone = AdventureDatabase.getAreaCombatData("Domed City of Ronaldus");
        var appearanceRates = zone.getMonsterData(true);

        var survivorWeight = 1.0;
        var totalWeight = (survivorWeight * 3) + (alienWeight * 3);

        var alienChance = (alienWeight / totalWeight) * 100;
        var survivorChance = (survivorWeight / totalWeight) * 100;

        assertThat(appearanceRates, aMapWithSize(6));
        assertThat(
            appearanceRates,
            allOf(
                hasEntry(
                    equalTo(MonsterDatabase.findMonster("dogcat")), closeTo(alienChance, 0.001)),
                hasEntry(
                    equalTo(MonsterDatabase.findMonster("ferrelf")), closeTo(alienChance, 0.001)),
                hasEntry(
                    equalTo(MonsterDatabase.findMonster("hamsterpus")),
                    closeTo(alienChance, 0.001)),
                hasEntry(
                    equalTo(MonsterDatabase.findMonster("overarmed survivor")),
                    closeTo(survivorChance, 0.001)),
                hasEntry(
                    equalTo(MonsterDatabase.findMonster("primitive survivor")),
                    closeTo(survivorChance, 0.001)),
                hasEntry(
                    equalTo(MonsterDatabase.findMonster("unlikely survivor")),
                    closeTo(survivorChance, 0.001))));
      }
    }

    @ParameterizedTest
    @CsvSource({"13, 0.25", "2, 0.0", "27, 0.37"})
    void alienAppearanceAffectedByGrimaceLight(final int dayOfJune2024, final double alienWeight) {
      var cleanups = withDay(2024, Month.JUNE, dayOfJune2024);

      try (cleanups) {
        var zone = AdventureDatabase.getAreaCombatData("Domed City of Grimacia");
        var appearanceRates = zone.getMonsterData(true);

        var survivorWeight = 1.0;
        var totalWeight = (survivorWeight * 3) + (alienWeight * 3);

        var alienChance = (alienWeight / totalWeight) * 100;
        var survivorChance = (survivorWeight / totalWeight) * 100;

        assertThat(appearanceRates, aMapWithSize(6));
        assertThat(
            appearanceRates,
            allOf(
                hasEntry(
                    equalTo(MonsterDatabase.findMonster("alielf")), closeTo(alienChance, 0.001)),
                hasEntry(
                    equalTo(MonsterDatabase.findMonster("cat-alien")), closeTo(alienChance, 0.001)),
                hasEntry(
                    equalTo(MonsterDatabase.findMonster("dog-alien")), closeTo(alienChance, 0.001)),
                hasEntry(
                    equalTo(MonsterDatabase.findMonster("grizzled survivor")),
                    closeTo(survivorChance, 0.001)),
                hasEntry(
                    equalTo(MonsterDatabase.findMonster("unhinged survivor")),
                    closeTo(survivorChance, 0.001)),
                hasEntry(
                    equalTo(MonsterDatabase.findMonster("whiny survivor")),
                    closeTo(survivorChance, 0.001))));
      }
    }
  }

  @Nested
  class SmoochArmyHq {
    static final MonsterData SMOOCH_PRIVATE = MonsterDatabase.findMonster("SMOOCH private");
    static final MonsterData SMOOCH_SERGEANT = MonsterDatabase.findMonster("SMOOCH sergeant");
    static final MonsterData SMOOCH_GENERAL = MonsterDatabase.findMonster("SMOOCH general");

    @ParameterizedTest
    @CsvSource({
      "0, 100, 0, 0",
      "19, 100, 0, 0",
      "20, 50, 50, 0",
      "40, 33.3333, 33.3333, 33.3333",
    })
    public void monsterRateChanges(
        final int combats,
        final double smoochPrivate,
        final double smoochSergeant,
        final double smoochGeneral) {
      var cleanups = withProperty("_smoochArmyHQCombats", combats);

      try (cleanups) {
        var appearanceRates =
            AdventureDatabase.getAreaCombatData("The SMOOCH Army HQ").getMonsterData(true);
        assertThat(appearanceRates.get(SMOOCH_PRIVATE), closeTo(smoochPrivate, 0.001));
        assertThat(appearanceRates.get(SMOOCH_SERGEANT), closeTo(smoochSergeant, 0.001));
        assertThat(appearanceRates.get(SMOOCH_GENERAL), closeTo(smoochGeneral, 0.001));
      }
    }

    @Test
    public void forcedNonCombat() {
      var cleanups = withProperty("_smoochArmyHQCombats", 50);

      try (cleanups) {
        var combat =
            AdventureDatabase.getAreaCombatData("The SMOOCH Army HQ").areaCombatPercent(true);
        assertThat(combat, is(0.0));
      }
    }
  }

  @Nested
  class TheSea {
    static final MonsterData TROPHYFISH = MonsterDatabase.findMonster("trophyfish");

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void trophyFishNeedsUnlocked(final boolean unlocked) {
      var cleanups = withProperty("grandpaUnlockedTrophyFish", unlocked);

      try (cleanups) {
        var appearanceRates =
            AdventureDatabase.getAreaCombatData("The Brinier Deepers").getMonsterData(true);
        assertThat(appearanceRates.get(TROPHYFISH), unlocked ? greaterThan(0.0) : is(0.0));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "cargo crab, 15, false",
      "cargo crab, 20, true",
      "mine crab, 19, true",
      "unholy diver, 21, false",
      "Mer-kin scavenger, 19, true",
      "Mer-kin scavenger, 20, true",
    })
    void hatchAffectsFitzsimmonsMonsters(
        final String monster, final int turnsAgo, final boolean expected) {
      var turns = 30;
      var cleanups =
          new Cleanups(
              withTurnsPlayed(turns), withProperty("_lastFitzsimmonsHatch", turns - turnsAgo));

      try (cleanups) {
        var appearanceRates =
            AdventureDatabase.getAreaCombatData("The Wreck of the Edgar Fitzsimmons")
                .getMonsterData(true);
        assertThat(
            appearanceRates.get(MonsterDatabase.findMonster(monster)),
            expected ? closeTo(100.0 / 3, 0.001) : is(0.0));
      }
    }

    @Test
    void hatchNotOpenInFirstFewTurns() {
      var cleanups = new Cleanups(withTurnsPlayed(4), withProperty("_lastFitzsimmonsHatch", -1));

      try (cleanups) {
        var appearanceRates =
            AdventureDatabase.getAreaCombatData("The Wreck of the Edgar Fitzsimmons")
                .getMonsterData(true);
        assertThat(appearanceRates.get(MonsterDatabase.findMonster("unholy diver")), is(0.0));
      }
    }
  }
}
