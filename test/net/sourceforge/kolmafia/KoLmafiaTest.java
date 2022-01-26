package net.sourceforge.kolmafia;

import static internal.helpers.Player.addItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    addItem("seal-clubbing club", 20);
    addItem("seal tooth", 4);

    ArrayList<AdventureResult> requirements =
        new ArrayList<>(
            List.of(
                AdventureResult.tallyItem("seal-clubbing club", 11, true),
                AdventureResult.tallyItem("seal tooth", 4, true)));

    var result = KoLmafia.checkRequirements(requirements, false);
    assertTrue(result);
  }

  @Test
  public void canDetectRequirementsNotMet() {
    addItem("seal-clubbing club", 20);
    addItem("seal tooth", 2);

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
