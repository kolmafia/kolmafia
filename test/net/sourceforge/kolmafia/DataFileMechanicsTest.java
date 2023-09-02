package net.sourceforge.kolmafia;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DataFileMechanicsTest {

  // This is a simplistic test that just counts tab delimited fields in src/data.  It is primarily
  // expected to catch mechanical issues such as using spaces instead of tabs and does not look at
  // content.  It is possible that an error would remain undetected in files where the low and high
  // counts are not the same.

  public static Stream<Arguments> data() {
    return Stream.of(
        // file name, version number, low field count, high field count
        Arguments.of("adventures.txt", 6, 4, 5),
        Arguments.of("bounty.txt", 2, 7, 7),
        Arguments.of("buffbots.txt", 1, 3, 3),
        Arguments.of("cafe_booze.txt", 1, 2, 2),
        Arguments.of("cafe_food.txt", 1, 2, 2),
        Arguments.of("classskills.txt", 4, 6, 7),
        Arguments.of("coinmasters.txt", 2, 4, 5),
        // combats.txt too complex
        // concoctions.txt too complex
        // consequences.txt too complex
        // cultshorts.txt too complex
        Arguments.of("defaults.txt", 1, 2, 3),
        Arguments.of("encounters.txt", 1, 3, 3),
        Arguments.of("equipment.txt", 2, 3, 4),
        Arguments.of("fambattle.txt", 1, 8, 8),
        Arguments.of("familiars.txt", 4, 10, 11),
        Arguments.of("faxbots.txt", 1, 2, 2),
        // foldgroups.txt is too complex
        Arguments.of("fullness.txt", 2, 8, 9),
        Arguments.of("inebriety.txt", 2, 8, 10),
        Arguments.of("items.txt", 1, 7, 8),
        // mallprices.txt is no longer bundled
        // modifiers.txt is too complex
        // monsters.txt is too complex
        Arguments.of("nonfilling.txt", 1, 2, 3),
        Arguments.of("npcstores.txt", 2, 4, 5),
        // Trick-or-treat candy is optional if too complicated
        Arguments.of("outfits.txt", 3, 4, 5),
        Arguments.of("packages.txt", 1, 4, 4),
        Arguments.of("pulverize.txt", 2, 2, 2),
        Arguments.of("questscouncil.txt", 1, 3, 5),
        // questslogs.txt is too complex
        Arguments.of("restores.txt", 2, 7, 8),
        Arguments.of("spleenhit.txt", 1, 8, 9),
        Arguments.of("statuseffects.txt", 1, 6, 7),
        Arguments.of("TCRS.astral_consumables.txt", 0, 4, 4),
        Arguments.of("TCRS.astral_pets.txt", 0, 4, 4),
        // zapgroups.txt is too simple
        Arguments.of("zonelist.txt", 2, 3, 4));
  }

  // Field counts that are not really an error.
  boolean skipMe(int checkVal) {
    if (checkVal == 0) return true;
    return checkVal == 1;
  }

  // This will only catch data entry errors when editing the test parameters.
  boolean precheck(int lowCount, int highCount) {
    if (lowCount <= 1) return false;
    return highCount >= lowCount;
    // Need to check that file exists, is internal and not an external file overriding an
    // internal file and that the file has the expected version.  FileUtilities doesn't
    // really expose that information to a test.
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testDataFileFieldCounts(String fname, int version, int lowCount, int highCount) {
    // If the precheck fails then the parameters in this test file are wrong and this file needs
    // to be edited.
    assertTrue(precheck(lowCount, highCount), fname + " failed precheck.");
    // FileUtilities will log "errors" but tries very hard to return a reader no matter what.
    try (BufferedReader reader = FileUtilities.getVersionedReader(fname, version)) {
      String[] fields;
      boolean noLines = true;
      while ((fields = FileUtilities.readData(reader)) != null) {
        noLines = false;
        int fieldsRead = fields.length;
        if (skipMe(fieldsRead)) continue;
        StringBuilder msg = new StringBuilder(fname);
        for (String field : fields) {
          msg.append(" ").append(field);
        }
        // Line has too many or too few fields.
        assertThat(
            msg.toString(),
            fieldsRead,
            allOf(greaterThanOrEqualTo(lowCount), lessThanOrEqualTo(highCount)));
      }
      // No lines is sometimes a symptom caused by a bad file name.
      assertFalse(noLines, "No lines in " + fname);
    } catch (IOException e) {
      fail("Exception in tearing down reader:" + e);
    }
  }
}
