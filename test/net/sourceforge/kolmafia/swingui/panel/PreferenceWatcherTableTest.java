package net.sourceforge.kolmafia.swingui.panel;

import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withoutProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import internal.helpers.Cleanups;
import javax.swing.*;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PreferenceWatcherTableTest {
  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset(true);
    KoLCharacter.reset("PreferenceWatcherTableTest");
    Preferences.reset("PreferenceWatcherTableTest");
  }

  @Nested
  class Rendering {
    @Test
    void rendersPreferenceTable() {
      var cleanups =
          new Cleanups(
              withProperty("testPrefA", "a"),
              withProperty("testPrefB", "b"),
              withProperty("watchedPreferences", "testPrefA,testPrefB,nonExistentTestPrefC"));

      try (cleanups) {
        var table = new PreferenceWatcherTable();
        assertThat(table.getRowCount(), is(3));

        var model = table.getModel();
        assertThat(model.getValueAt(0, 0), is("testPrefA"));
        assertThat(model.getValueAt(0, 1), is("a"));
        assertThat(model.getValueAt(1, 0), is("testPrefB"));
        assertThat(model.getValueAt(1, 1), is("b"));
        assertThat(model.getValueAt(2, 0), is("nonExistentTestPrefC"));
        assertThat(model.getValueAt(2, 1), equalTo(null));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rendersNullValuesProperly(final boolean expected) {
      var cleanups = new Cleanups(withProperty("watchedPreferences", "nonExistentTestPref"));

      try (cleanups) {
        var table = new PreferenceWatcherTable();
        var renderer = table.getCellRenderer(0, expected ? 0 : 1);
        var component =
            renderer.getTableCellRendererComponent(table, null, false, false, 0, expected ? 0 : 1);
        assertThat(component.isEnabled(), is(expected));
      }
    }

    @Test
    void reactsToValueChanges() {
      var cleanups =
          new Cleanups(
              withProperty("testPrefA", "a"), withProperty("watchedPreferences", "testPrefA"));

      try (cleanups) {
        var table = new PreferenceWatcherTable();
        var model = table.getModel();
        assertThat(model.getValueAt(0, 1), is("a"));

        Preferences.setString("testPrefA", "b");

        assertThat(model.getValueAt(0, 1), is("b"));
      }
    }

    @Test
    void reactsToPrefCreation() {
      var cleanups =
          new Cleanups(
              withoutProperty("testPrefA"), withProperty("watchedPreferences", "testPrefA"));

      try (cleanups) {
        var table = new PreferenceWatcherTable();
        var model = table.getModel();
        assertThat(model.getValueAt(0, 1), equalTo(null));

        Preferences.setString("testPrefA", "a");

        assertThat(model.getValueAt(0, 1), is("a"));
      }
    }

    @Test
    void reactsToWatchedPrefChanges() {
      var cleanups =
          new Cleanups(
              withProperty("testPrefA", "a"),
              withProperty("testPrefB", "b"),
              withProperty("watchedPreferences", "testPrefA"));

      try (cleanups) {
        var table = new PreferenceWatcherTable();
        var model = table.getModel();

        assertThat(table.getRowCount(), is(1));
        assertThat(model.getValueAt(0, 1), is("a"));

        Preferences.setString("watchedPreferences", "testPrefA,testPrefB");

        assertThat(table.getRowCount(), is(2));
        assertThat(model.getValueAt(1, 1), is("b"));
      }
    }

    // Some silly tests for coverage
    @Test
    void handlesInvalidColumnHeader() {
      var table = new PreferenceWatcherTable();
      var model = table.getModel();
      assertThat(model.getColumnName(3), nullValue());
    }

    @Test
    void handlesInvalidColumnValue() {
      var cleanups =
          new Cleanups(
              withProperty("testPrefA", "a"), withProperty("watchedPreferences", "testPrefA"));

      try (cleanups) {
        var table = new PreferenceWatcherTable();
        var model = table.getModel();
        assertThat(model.getValueAt(0, 3), nullValue());
      }
    }

    @Test
    void handlesInvalidRowValue() {
      var cleanups =
          new Cleanups(
              withProperty("testPrefA", "a"), withProperty("watchedPreferences", "testPrefA"));

      try (cleanups) {
        var table = new PreferenceWatcherTable();
        var model = table.getModel();
        assertThat(model.getValueAt(50, 1), nullValue());
      }
    }
  }

  @Nested
  class CRUD {
    @Test
    void canRemoveProperty() {
      var cleanups =
          new Cleanups(
              withProperty("testPrefA", "a"),
              withProperty("testPrefB", "b"),
              withProperty("watchedPreferences", "testPrefA,testPrefB"));

      try (cleanups) {
        var table = new PreferenceWatcherTable();
        table.getModel().removePreference(0);

        assertThat(table.getRowCount(), is(1));
        assertThat("watchedPreferences", isSetTo("testPrefB"));
      }
    }

    @ParameterizedTest
    @CsvSource({"testPrefB, 2, 'testPrefB,testPrefA'", "'', 1, 'testPrefA'"})
    void canAddProperty(final String initial, final int expectedCount, final String expected) {
      var cleanups =
          new Cleanups(
              withProperty("testPrefA", "a"),
              withProperty("testPrefB", "b"),
              withProperty("watchedPreferences", initial));

      try (cleanups) {
        var table = new PreferenceWatcherTable();
        table.getModel().addPreference("testPrefA");

        assertThat(table.getRowCount(), is(expectedCount));
        assertThat(table.getModel().getValueAt(expectedCount - 1, 1), is("a"));
        assertThat("watchedPreferences", isSetTo(expected));
      }
    }
  }
}
