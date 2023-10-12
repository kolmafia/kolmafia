package net.sourceforge.kolmafia;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withResponseMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class KoLmafiaTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset(true);
  }

  @Test
  public void canDetectRequirementsMet() {
    var cleanups = new Cleanups(withItem("seal-clubbing club", 20), withItem("seal tooth", 4));

    try (cleanups) {
      ArrayList<AdventureResult> requirements =
          new ArrayList<>(
              List.of(
                  AdventureResult.tallyItem("seal-clubbing club", 11, true),
                  AdventureResult.tallyItem("seal tooth", 4, true)));

      var result = KoLmafia.checkRequirements(requirements, false);
      assertTrue(result);
    }
  }

  @Test
  public void canDetectRequirementsNotMet() {
    var cleanups = new Cleanups(withItem("seal-clubbing club", 20), withItem("seal tooth", 2));

    try (cleanups) {
      ArrayList<AdventureResult> requirements =
          new ArrayList<>(
              List.of(
                  AdventureResult.tallyItem("seal-clubbing club", 11, true),
                  AdventureResult.tallyItem("seal tooth", 4, true)));

      var result = KoLmafia.checkRequirements(requirements, false);
      assertFalse(result);

      assertThat(requirements, hasSize(1));
      assertEquals(2, requirements.get(0).getCount());
    }
  }

  @Test
  public void refreshSessionSetsPassiveModifiers() {
    // This charsheet contains Stomach of Steel.
    var cleanups =
        new Cleanups(
            withResponseMap(
                Map.of(
                    "https://www.kingdomofloathing.com:443/charsheet.php",
                    new FakeHttpResponse<>(200, html("request/test_charsheet_normal.html")))));

    try (cleanups) {
      assertEquals(15, KoLCharacter.getStomachCapacity());
      KoLmafia.refreshSession();
      assertEquals(20, KoLCharacter.getStomachCapacity());
    }
  }
}
