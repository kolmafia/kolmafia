package net.sourceforge.kolmafia.utilities;

import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import java.io.File;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ScriptMRUListTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("ScriptMRUListTest");
  }

  private static Cleanups withEmptyList(final int length) {
    return new Cleanups(
        withProperty("testMRUList", ""),
        withProperty("testMRULength", length),
        withProperty("testMRUDefaults", "one | two"));
  }

  @Nested
  class Recent {
    @Test
    void putsTheLatestEntryFirstWithoutDuplicates() {
      try (var cleanups = withEmptyList(3)) {
        var list = new ScriptMRUList("testMRUList", "testMRULength");

        list.addItem("a.ash");
        list.addItem("b.ash");
        list.addItem("a.ash");

        assertThat(list.getItems(), contains("a.ash", "b.ash"));
        assertThat(list.getFirst(), is("a.ash"));
        assertThat("testMRUList", isSetTo("a.ash;b.ash"));
      }
    }

    @Test
    void keepsOnlyTheConfiguredNumberOfEntries() {
      try (var cleanups = withEmptyList(2)) {
        var list = new ScriptMRUList("testMRUList", "testMRULength");

        list.addItem("a.ash");
        list.addItem("b.ash");
        list.addItem("c.ash");

        assertThat(list.getItems(), contains("c.ash", "b.ash"));
      }
    }

    @Test
    void ignoresEmptyNamesAndZeroLength() {
      try (var cleanups = withEmptyList(0)) {
        var list = new ScriptMRUList("testMRUList", "testMRULength");

        list.addItem("a.ash");
        list.addItem("");

        assertThat(list.getItems(), empty());
        assertThat(list.getFirst(), is("Unknown"));
      }
    }

    @Test
    void loadsEntriesFromThePreference() {
      var cleanups =
          new Cleanups(
              withProperty("testMRUList", "x.ash;y.ash"), withProperty("testMRULength", 4));

      try (cleanups) {
        var list = new ScriptMRUList("testMRUList", "testMRULength");

        assertThat(list.getItems(), contains("x.ash", "y.ash"));
      }
    }
  }

  @Nested
  class RelativePath {
    @Test
    void dropsTheScriptDirectory() {
      File script = new File(KoLConstants.SCRIPT_LOCATION, "sub/test.ash");

      assertThat(ScriptMRUList.getRelativePath(script), is("sub/test.ash"));
    }

    @Test
    void keepsAbsolutePathsOutsideKoLmafia() {
      File script = new File("/elsewhere/test.ash");

      assertThat(ScriptMRUList.getRelativePath(script), is(script.getAbsolutePath()));
    }
  }

  @Nested
  class Partial {
    @Test
    void offersRecentEntriesThenDefaults() {
      try (var cleanups = withEmptyList(3)) {
        var list = new PartialMRUList("testMRUList", "testMRULength", "testMRUDefaults");

        // The list is created before the user's preferences are loaded
        Preferences.setString("testMRUList", "myst;mus");

        assertThat(list.getComboItems(), contains("myst", "mus", "one", "two"));
        assertThat(list.getRecentCount(), is(2));
      }
    }

    @Test
    void loadsTheRecentEntriesOnlyOnce() {
      try (var cleanups = withEmptyList(3)) {
        var list = new PartialMRUList("testMRUList", "testMRULength", "testMRUDefaults");
        Preferences.setString("testMRUList", "myst");

        list.getComboItems();

        assertThat(list.getComboItems(), contains("myst", "one", "two"));
      }
    }
  }
}
