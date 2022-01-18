package net.sourceforge.kolmafia;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.util.Map;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.CrystalBallManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

  @BeforeAll
  public static void beforeAll() {
    Preferences.saveSettingsToFile = false;
  }

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

  @Test
  public void crystalBallPrediction() {
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

  @Test
  public void crystalBallPredictionWhenNCIsUp() {
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
}
