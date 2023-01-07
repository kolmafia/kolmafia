package net.sourceforge.kolmafia;

import static internal.helpers.CliCaller.withCliOutput;
import static internal.helpers.Player.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import internal.helpers.Cleanups;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DebugModifiersTest {
  private static Matcher<String> containsDebugRow(
      String type, String name, double value, double total) {
    return matchesPattern(
        ".*<td>"
            + type
            + "</td><td>"
            + name
            + "</td>(<td></td>)*<td>.*"
            + Pattern.quote(KoLConstants.ROUNDED_MODIFIER_FORMAT.format(value))
            + "</td><td>=&nbsp;"
            + Pattern.quote(KoLConstants.ROUNDED_MODIFIER_FORMAT.format(total))
            + "</td>.*");
  }

  @BeforeAll
  public static void beforeAll() {
    Preferences.reset("DebugModifiersTest");
  }

  @Test
  void listsEffects() {
    StringBuffer output = new StringBuffer();
    try (var cleanups =
        new Cleanups(withEffect(EffectPool.SYNTHESIS_COLLECTION), withCliOutput(output))) {
      DebugModifiers.setup("item drop");
      KoLCharacter.recalculateAdjustments(true);
    }
    assertThat(
        output.toString().strip(),
        containsDebugRow("Effect", "Synthesis: Collection", 150.0, 150.0));
  }

  @Test
  void listsEquipment() {
    StringBuffer output = new StringBuffer();
    try (var cleanups =
        new Cleanups(
            withEquipped(EquipmentManager.HAT, ItemPool.WAD_OF_TAPE), withCliOutput(output))) {
      DebugModifiers.setup("item drop");
      KoLCharacter.recalculateAdjustments(true);
    }
    assertThat(output.toString().strip(), containsDebugRow("Item", "wad of used tape", 15.0, 15.0));
  }

  @Test
  void listsMCD() {
    StringBuffer output = new StringBuffer();
    try (var cleanups = new Cleanups(withMCD(10), withCliOutput(output))) {
      DebugModifiers.setup("monster level");
      KoLCharacter.recalculateAdjustments(true);
    }
    assertThat(
        output.toString().strip(), containsDebugRow("Mcd", "Monster Control Device", 10.0, 10.0));
  }

  @Test
  void listsSign() {
    StringBuffer output = new StringBuffer();
    try (var cleanups = new Cleanups(withSign(ZodiacSign.PACKRAT), withCliOutput(output))) {
      DebugModifiers.setup("item drop");
      KoLCharacter.recalculateAdjustments(true);
    }
    assertThat(output.toString().strip(), containsDebugRow("Sign", "Packrat", 10.0, 10.0));
  }

  @Test
  void listsSquint() {
    StringBuffer output = new StringBuffer();
    try (var cleanups =
        new Cleanups(
            withEquipped(EquipmentManager.HAT, ItemPool.WAD_OF_TAPE),
            withEffect(EffectPool.STEELY_EYED_SQUINT),
            withCliOutput(output))) {
      DebugModifiers.setup("item drop");
      KoLCharacter.recalculateAdjustments(true);
    }
    assertThat(output.toString().strip(), containsDebugRow("Item", "wad of used tape", 15.0, 15.0));
    assertThat(
        output.toString().strip(), containsDebugRow("Effect", "Steely-Eyed Squint", 15.0, 30.0));
  }

  @Test
  void listsMultipleInOneRow() {
    StringBuffer output = new StringBuffer();
    try (var cleanups =
        new Cleanups(withEffect(EffectPool.ELEMENTAL_SPHERE), withCliOutput(output))) {
      DebugModifiers.setup("resistance");
      KoLCharacter.recalculateAdjustments(true);
    }
    assertThat(output.toString().strip().split("<tr>"), arrayWithSize(3));
    assertThat(
        output.toString().strip(),
        stringContainsInOrder(
            "<td>+2.00</td><td>=&nbsp;+2.00</td>",
            "<td>+2.00</td><td>=&nbsp;+2.00</td>",
            "<td>+2.00</td><td>=&nbsp;+2.00</td>",
            "<td>+2.00</td><td>=&nbsp;+2.00</td>",
            "<td>+2.00</td><td>=&nbsp;+2.00</td>"));
  }
}
