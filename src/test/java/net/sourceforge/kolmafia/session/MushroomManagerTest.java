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
  /** Deletes the planting folder afterwards, unless it was already there. */
  private static Cleanups withPlotsFolder() {
    File plots = KoLConstants.PLOTS_LOCATION;
    boolean existed = plots.exists();
    plots.mkdirs();
    return new Cleanups(
        () -> {
          if (existed) return;
          try (Stream<Path> files = Files.walk(plots.toPath())) {
            files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
          } catch (IOException ignored) {
          }
        });
  }

  private static Cleanups withShouldNotOpenBrowser() {
    MushroomManager.shouldOpenBrowser = false;
    return new Cleanups(() -> MushroomManager.shouldOpenBrowser = true);
  }

  @Test
  void savedLayoutPlansElevenDays() throws IOException {
    // A plot planted with knob spores every day never runs out, so the layout stops at the
    // forecast's length
    String[][] original = new String[12][16];
    String[][] planning = new String[12][16];
    for (String[] day : original) Arrays.fill(day, "__");
    for (String[] day : planning) Arrays.fill(day, "kb");

    try (var cleanups = new Cleanups(withPlotsFolder(), withShouldNotOpenBrowser())) {
      MushroomManager.saveLayout("MushroomManagerTest", original, planning);

      String layout =
          Files.readString(
              new File(KoLConstants.PLOTS_LOCATION, "MushroomManagerTest.txt").toPath());
      assertThat(layout, containsString("Day 1:"));
      assertThat(layout, containsString("Day 11:"));
      assertThat(layout, not(containsString("Day 12:")));
    }
  }

  @Test
  void savedLayoutStopsAfterTheFirstEmptyDay() throws IOException {
    String[][] original = new String[12][16];
    String[][] planning = new String[12][16];
    for (String[] day : original) Arrays.fill(day, "__");
    for (String[] day : planning) Arrays.fill(day, "__");
    Arrays.fill(planning[0], "kb");

    try (var cleanups = new Cleanups(withPlotsFolder(), withShouldNotOpenBrowser())) {
      MushroomManager.saveLayout("MushroomManagerTest", original, planning);

      String layout =
          Files.readString(
              new File(KoLConstants.PLOTS_LOCATION, "MushroomManagerTest.txt").toPath());
      assertThat(layout, containsString("Day 2:"));
      assertThat(layout, not(containsString("Day 3:")));
    }
  }
}
