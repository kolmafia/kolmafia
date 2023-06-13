package net.sourceforge.kolmafia.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ModifierDatabaseTest {
  @Test
  public void testSynergies() {
    // The "synergy" bitmap modifier is assigned dynamically, based on appearance order in
    // Modifiers.txt
    // The first Synergetic item seen gets 0b00001, the 2nd: 0b00010, 3rd: 0b00100, etc.

    for (Entry<String, Integer> entry : ModifierDatabase.getSynergies()) {
      String name = entry.getKey();
      int mask = entry.getValue();

      int manualMask = 0;
      for (String piece : name.split("/")) {
        Modifiers mods = ModifierDatabase.getModifiers(ModifierType.ITEM, piece);
        manualMask |= mods.getRawBitmap(BitmapModifier.SYNERGETIC);
      }

      assertEquals(manualMask, mask, name);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "+50% Spell Damage, Spell Damage Percent: +50",
    "Successful hit weakens opponent, Weakens Monster",
    "Only Accordion Thieves may use this item, Class: \"Accordion Thief\"",
    "All Attributes +5, 'Muscle: +5, Mysticality: +5, Moxie: +5'",
    "All Attributes +30%, 'Muscle Percent: +30, Mysticality Percent: +30, Moxie Percent: +30'",
    "Bonus&nbsp;for&nbsp;Saucerors&nbsp;only, Class: \"Sauceror\"",
    "Monsters are much more attracted to you., Combat Rate: +10",
    "Monsters will be significantly less attracted to you. (Underwater only), Combat Rate (Underwater): -15",
    "Maximum HP/MP +200, 'Maximum HP: +200, Maximum MP: +200'",
    "Regenerate 100 MP per adventure, 'MP Regen Min: 100, MP Regen Max: 100'",
    "Regenerate 15-20 HP and MP per adventure, 'HP Regen Min: 15, HP Regen Max: 20, MP Regen Min: 15, MP Regen Max: 20'",
    "Serious Cold Resistance (+3), Cold Resistance: +3",
    "Sublime Resistance to All Elements (+9), 'Spooky Resistance: +9, Stench Resistance: +9, Hot Resistance: +9, Cold Resistance: +9, Sleaze Resistance: +9'",
    "So-So Slime Resistance (+2), Slime Resistance: +2",
    "Slight Supercold Resistance, Supercold Resistance: +1",
    "Your familiar will always act in combat, Familiar Action Bonus: +100"
  })
  public void canParseModifier(String enchantment, String modifier) {
    assertEquals(modifier, ModifierDatabase.parseModifier(enchantment));
  }

  @Test
  @Disabled("modifiers.txt would need to be modified")
  public void writeModifiersSubsetOfModifiersTxt() throws IOException {
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    PrintStream writer = new PrintStream(ostream);

    ModifierDatabase.writeModifiers(writer);
    writer.close();
    List<String> writeModifiersLines = ostream.toString().lines().collect(Collectors.toList());

    BufferedReader reader =
        DataUtilities.getReader(KoLConstants.DATA_DIRECTORY, "modifiers.txt", true);

    String line;
    Iterator<String> writeModifiersIterator = writeModifiersLines.iterator();
    String writeModifiersLine = writeModifiersIterator.next();
    while ((line = reader.readLine()) != null) {
      if (writeModifiersLine.startsWith("# ")
          ? line.startsWith(writeModifiersLine)
          : line.equals(writeModifiersLine)) {
        writeModifiersLine =
            writeModifiersIterator.hasNext() ? writeModifiersIterator.next() : null;
      }
    }

    StringBuilder message = new StringBuilder();
    if (writeModifiersLine != null) {
      int index = writeModifiersLines.indexOf(writeModifiersLine);
      for (int i = Math.min(3, index); i >= 0; i--) {
        message.append("previous line: [" + writeModifiersLines.get(index - i) + "]\n");
      }
    }
    message.append("unmatched line: [" + writeModifiersLine + "]");
    assertThat(message.toString(), writeModifiersIterator.hasNext(), is(false));
  }
}
