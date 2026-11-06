package net.sourceforge.kolmafia.session;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLConstants;
import org.junit.jupiter.api.Test;

class MushroomManagerTest {
  /**
   * Removes the planting folder afterwards if this test is the one that made it (it is not in git).
   */
  private static Cleanups withPlotsFolder() {
    File plots = KoLConstants.PLOTS_LOCATION;
    boolean existed = plots.exists();
    plots.mkdirs();
    return new Cleanups(
        () -> {
          if (existed) return;
          try (Stream<Path> files = Files.walk(plots.toPath())) {
            files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
          } catch (IOException e) {
            // nothing left to clean up
          }
        });
  }

  @Test
  void savedLayoutPlansElevenDays() throws IOException {
    // A plot planted with knob spores every day never runs out, so the layout stops at the
    // forecast's length
    String[][] original = new String[12][16];
    String[][] planning = new String[12][16];
    for (String[] day : original) Arrays.fill(day, "__");
    for (String[] day : planning) Arrays.fill(day, "kb");

    try (var cleanups = withPlotsFolder()) {
      MushroomManager.saveLayout("MushroomManagerTest", original, planning);

      String layout =
          Files.readString(
              new File(KoLConstants.PLOTS_LOCATION, "MushroomManagerTest.txt").toPath());
      assertThat(layout, containsString("Day 1:"));
      assertThat(layout, containsString("Day 11:"));
      assertThat(layout, not(containsString("Day 12:")));
    }
  }
}
