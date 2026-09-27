package net.sourceforge.kolmafia;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import net.sourceforge.kolmafia.combat.CustomCombatLookup;
import org.junit.jupiter.api.Test;

public class CustomCombatScriptTest {
  @Test
  public void canHandleMultipleClosingBrackets() {
    // https://wiki.kolmafia.us/index.php/Custom_Combat_Script#Using_ASH_Constants_in_CCS
    String source =
        """
            [ $element[ spooky ] $item[ hobo nickel ] ]
            skill entangling noodles
            skill weapon of the pastalord""";

    CustomCombatLookup lookup = new CustomCombatLookup();
    lookup.addEncounterKey("default");

    try (var reader = new BufferedReader(new StringReader(source))) {
      lookup.load(reader);

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      try (PrintStream out = new PrintStream(outputStream, true)) {
        lookup.store(out);
      }

      assertThat(outputStream.toString(StandardCharsets.UTF_8), containsString(source));
    } catch (IOException e) {
      fail("Exception in loading Custom Combat Script:" + e.toString());
    }
  }
}
