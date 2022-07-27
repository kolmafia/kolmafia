package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.JuneCleaverManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AdventureRequestTest {
  @BeforeEach
  public void init() {
    Preferences.saveSettingsToFile = false;
    KoLCharacter.reset("AdventureRequestTest");
    Preferences.reset("AdventureRequestTest");
  }

  @Test
  public void aboveWaterZonesCostOneAdventure() {
    AdventureRequest request = new AdventureRequest("Noob Cave", "adventure.php", "240");
    assertEquals(1, request.getAdventuresUsed());
  }

  @Test
  public void underwaterZonesCostTwoAdventures() {
    AdventureRequest request = new AdventureRequest("The Ice Hole", "adventure.php", "457");
    assertEquals(2, request.getAdventuresUsed());
  }

  @Test
  public void underwaterZonesCostOneAdventureWithFishy() {
    var cleanups = withEffect("Fishy");

    try (cleanups) {
      AdventureRequest request = new AdventureRequest("The Ice Hole", "adventure.php", "457");
      KoLCharacter.recalculateAdjustments();
      assertEquals(1, request.getAdventuresUsed());
    }
  }

  @Test
  public void gregariousMonstersAreQueued() {
    KoLAdventure.setLastAdventure(AdventureDatabase.getAdventure("Barf Mountain"));
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("Knob Goblin Embezzler"));

    var req = new GenericRequest("fight.php");
    req.setHasResult(true);
    req.responseText = html("request/test_fight_gregarious_monster.html");
    req.processResponse();

    assertThat(
        AdventureQueueDatabase.getZoneQueue("Barf Mountain"), contains("Knob Goblin Embezzler"));
  }

  @Test
  public void juneCleaverTrackingTest() {
    // regular encounter
    JuneCleaverManager.parseChoice("choice.php?whichchoice=1467&option=1");
    assertEquals(Preferences.getString("juneCleaverQueue"), "1467");
    assertEquals(Preferences.getInteger("_juneCleaverEncounters"), 1);
    assertEquals(Preferences.getInteger("_juneCleaverFightsLeft"), 6);
    assertEquals(Preferences.getInteger("_juneCleaverSkips"), 0);

    // Skip
    JuneCleaverManager.parseChoice("choice.php?whichchoice=1468&option=4");
    assertEquals(Preferences.getString("juneCleaverQueue"), "1467,1468");
    assertEquals(Preferences.getInteger("_juneCleaverEncounters"), 1);
    assertEquals(Preferences.getInteger("_juneCleaverFightsLeft"), 2);
    assertEquals(Preferences.getInteger("_juneCleaverSkips"), 1);

    // Wrong choice
    JuneCleaverManager.parseChoice("choice.php?whichchoice=7000&option=4");
    assertEquals(Preferences.getString("juneCleaverQueue"), "1467,1468");
    assertEquals(Preferences.getInteger("_juneCleaverEncounters"), 1);
    assertEquals(Preferences.getInteger("_juneCleaverFightsLeft"), 2);
    assertEquals(Preferences.getInteger("_juneCleaverSkips"), 1);

    // No option
    JuneCleaverManager.parseChoice("choice.php?whichchoice=1469");
    assertEquals(Preferences.getString("juneCleaverQueue"), "1467,1468");
    assertEquals(Preferences.getInteger("_juneCleaverEncounters"), 1);
    assertEquals(Preferences.getInteger("_juneCleaverFightsLeft"), 2);
    assertEquals(Preferences.getInteger("_juneCleaverSkips"), 1);

    // Can load queue
    JuneCleaverManager.queue = new ArrayList();
    Preferences.setString("juneCleaverQueue", "1467,1468,1469,1470,1471");
    JuneCleaverManager.parseChoice("choice.php?whichchoice=1472&option=3");
    assertEquals(Preferences.getString("juneCleaverQueue"), "1467,1468,1469,1470,1471,1472");
    assertEquals(Preferences.getInteger("_juneCleaverEncounters"), 2);
    assertEquals(Preferences.getInteger("_juneCleaverFightsLeft"), 10);
    assertEquals(Preferences.getInteger("_juneCleaverSkips"), 1);

    // Queue has max length of 6
    JuneCleaverManager.parseChoice("choice.php?whichchoice=1473&option=1");
    assertEquals(Preferences.getString("juneCleaverQueue"), "1468,1469,1470,1471,1472,1473");
    assertEquals(Preferences.getInteger("_juneCleaverEncounters"), 3);
    assertEquals(Preferences.getInteger("_juneCleaverFightsLeft"), 12);
    assertEquals(Preferences.getInteger("_juneCleaverSkips"), 1);
  }
}
