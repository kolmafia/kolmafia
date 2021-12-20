package net.sourceforge.kolmafia;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DataFileTest {
  public static Stream<Arguments> data() {
    return Stream.of(
        Arguments.of(
            "items.txt",
            1,
            new String[] {
              "\\d+", // itemid
              ".+", // name
              "\\d+", // descid
              "[a-zA-Z_0-9/\\-]+\\.gif", // img
              ("(none|food|drink|spleen|usable|multiple|reusable|message|grow|pokepill|"
                  + "hat|weapon|sixgun|offhand|container|shirt|pants|accessory|familiar|sticker|"
                  + "card|folder|bootspur|bootskin|food helper|drink helper|zap|sphere|guardian|"
                  + "avatar|potion)(\\s*,\\s*(usable|multiple|reusable|combat|combat reusable|single|"
                  + "solo|curse|bounty|candy1|candy2|candy|chocolate|matchable|fancy|paste|smith|cook|mix))*"), // use
              "([qgtd](,[qgtd])*)?", // access
              "\\d+", // autosell
            }),
        Arguments.of(
            "fullness.txt",
            2,
            new String[] {
              ".+", // name
              "\\d+", // fullness
              "\\d+", // level
              // Sushi does not specify quality, for some reason.
              "(crappy|decent|good|awesome|EPIC|)", // quality
              "-?\\d+(-\\d+)?", // adv
              "-?\\d+(-\\d+)?", // mus
              "-?\\d+(-\\d+)?", // mys
              "-?\\d+(-\\d+)?", // mox
            }),
        Arguments.of(
            "inebriety.txt",
            2,
            new String[] {
              ".+", // name
              "\\d+", // fullness
              "\\d+", // level
              // Missing quality for old Crimbo cafe items.
              "(crappy|decent|good|awesome|EPIC|)", // quality
              "-?\\d+(-\\d+)?", // adv
              "-?\\d+(-\\d+)?", // mus
              "-?\\d+(-\\d+)?", // mys
              "-?\\d+(-\\d+)?", // mox
            }),
        Arguments.of(
            "spleenhit.txt",
            2,
            new String[] {
              ".+", // name
              "\\d+", // fullness
              "\\d+", // level
              "(|crappy|decent|good|awesome|EPIC)", // quality
              "-?\\d+(-\\d+)?", // adv
              "-?\\d+(-\\d+)?", // mus
              "-?\\d+(-\\d+)?", // mys
              "-?\\d+(-\\d+)?", // mox
            }));
  }

  private static String join(String[] parts, String delimiter) {
    if (parts.length == 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder(parts[0]);
    for (int i = 1; i < parts.length; ++i) {
      sb.append(parts[i]);
      sb.append(delimiter);
    }
    return sb.toString();
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testDataFileAgainstRegex(String fname, int version, String[] regexes) {
    BufferedReader reader = FileUtilities.getVersionedReader(fname, version);
    String[] fields;

    while ((fields = FileUtilities.readData(reader)) != null) {
      if (fields.length == 1) {
        // Placeholder.
        continue;
      }
      if (fields.length < regexes.length) {
        fail("Entry for " + fields[0] + " is missing fields");
      }
      for (int i = 0; i < regexes.length; ++i) {
        // Assume fields[0] is something that uniquely identifies the row.
        assertTrue(
            Pattern.matches(regexes[i], fields[i]),
            "Field " + i + " (" + fields[i] + ") did not match:\n" + join(fields, "\t"));
      }
    }

    try {
      reader.close();
    } catch (Exception e) {
      fail("Exception in tearing down reader:" + e.toString());
    }
  }
}
