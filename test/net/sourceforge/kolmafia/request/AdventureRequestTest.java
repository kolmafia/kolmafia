package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.KoLCharacter;
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
}
