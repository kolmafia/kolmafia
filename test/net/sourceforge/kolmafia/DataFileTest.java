package net.sourceforge.kolmafia;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DataFileTest {
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {
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
            }
          },
          {
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
            }
          },
          {
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
            }
          },
          {
            "spleenhit.txt",
            2,
            new String[] {
              ".+", // name
              "\\d+", // fullness
              "\\d+", // level
              "(crappy|decent|good|awesome|EPIC)", // quality
              "-?\\d+(-\\d+)?", // adv
              "-?\\d+(-\\d+)?", // mus
              "-?\\d+(-\\d+)?", // mys
              "-?\\d+(-\\d+)?", // mox
            }
          },
        });
  }

  private String fname;
  private int version;
  private String[] regexes;

  public DataFileTest(String fname, int version, String[] regexes) {
    this.fname = fname;
    this.version = version;
    this.regexes = regexes;
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

  @Test
  public void testDataFileAgainstRegex() {
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
            "Field " + i + " (" + fields[i] + ") did not match:\n" + join(fields, "\t"),
            Pattern.matches(regexes[i], fields[i]));
      }
    }

    try {
      reader.close();
    } catch (Exception e) {
      fail("Exception in tearing down reader:" + e.toString());
    }
  }
}
