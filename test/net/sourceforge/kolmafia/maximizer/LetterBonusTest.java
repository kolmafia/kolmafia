package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Player.withProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.AdventureResult;
import org.junit.jupiter.api.Test;

public class LetterBonusTest {
  @Test
  public void dealsWithNulls() {
    assertEquals(0, LetterBonus.letterBonus(null));
    assertEquals(0, LetterBonus.letterBonus(null, "X"));
    assertEquals(0, LetterBonus.numberBonus(null));
  }

  @Test
  public void letterBonusCountsLettersInItem() {
    AdventureResult item = AdventureResult.tallyItem("spiked femur");
    assertEquals(12, LetterBonus.letterBonus(item));
  }

  @Test
  public void letterBonusDoesNotCountMode() {
    var cleanups = withProperty("backupCameraMode", "meat");

    AdventureResult item = AdventureResult.tallyItem("backup camera");
    assertEquals(13, LetterBonus.letterBonus(item));
  }

  @Test
  public void letterBonusCountsSpecificLettersInItem() {
    AdventureResult item = AdventureResult.tallyItem("spiked femur");
    assertEquals(2, LetterBonus.letterBonus(item, "e"));
  }

  @Test
  public void numberBonusCountsNumbersInItem() {
    AdventureResult item = AdventureResult.tallyItem("X-37 gun");
    assertEquals(2, LetterBonus.numberBonus(item));
  }
}
