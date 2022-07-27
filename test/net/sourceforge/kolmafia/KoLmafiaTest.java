package net.sourceforge.kolmafia;

import static internal.helpers.Player.withItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.ArrayList;
import java.util.List;
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
}
