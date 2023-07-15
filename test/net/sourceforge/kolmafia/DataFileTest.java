package net.sourceforge.kolmafia;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
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
            "statuseffects.txt",
            1,
            new String[] {
              "\\d+", // effectid
              ".+", // name
              "[a-zA-Z_0-9/\\-]+\\.gif", // img
              "([a-f0-9]+|)", // descid
              "(bad|neutral|good)", // quality
              "(none|song|nohookah|nopvp|noremove|hottub)([ ,](none|song|nohookah|nopvp|noremove|hottub))*", // attributes
            }),
        Arguments.of(
            "fullness.txt",
            2,
            new String[] {
              ".+", // name
              "\\d+", // fullness
              "\\d+", // level
              // Missing quality is only for fake food "[glitch season reward name]"
              "(crappy|decent|good|awesome|EPIC|\\?\\?\\?|drippy|sushi|quest|)", // quality
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
              // Missing quality is only for fake drink "ice stein"
              "(crappy|decent|good|awesome|EPIC|\\?\\?\\?|drippy|quest|)", // quality
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
            }),
        Arguments.of(
            "questscouncil.txt",
            1,
            new String[] {
              "questL(02Larva|03Rat|04Bat|05Goblin|06Friar|07Cyrptic|08Trapper|09Topping|10Garbage|11MacGuffin|12War|12HippyFrat|13Final|13Warehouse)", // property
              "(started|step1|step7|step13|finished)", // quest step
              ".+", // council text
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
    try (BufferedReader reader = FileUtilities.getVersionedReader(fname, version)) {
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
    } catch (IOException e) {
      fail("Exception in tearing down reader:" + e.toString());
    }
  }
}
