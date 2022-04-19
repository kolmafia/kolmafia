package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AdventureRequestTest {
  @BeforeEach
  public void init() {
    KoLCharacter.reset(true);
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
    AdventureRequest request = new AdventureRequest("The Ice Hole", "adventure.php", "457");
    addEffect("Fishy");
    KoLCharacter.recalculateAdjustments();
    assertEquals(1, request.getAdventuresUsed());
  }

  @Test
  public void gregariousMonstersAreQueued() throws IOException {
    KoLAdventure.setLastAdventure(AdventureDatabase.getAdventure("Barf Mountain"));
    MonsterStatusTracker.setNextMonster(MonsterDatabase.findMonster("Knob Goblin Embezzler"));

    var req = new GenericRequest("fight.php");
    req.setHasResult(true);
    req.responseText = Files.readString(Path.of("request/test_fight_gregarious_monster.html"));
    req.processResponse();

    assertThat(
        AdventureQueueDatabase.getZoneQueue("Barf Mountain"), contains("Knob Goblin Embezzler"));
  }
}
