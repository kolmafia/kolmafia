package net.sourceforge.kolmafia.swingui.menu;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.PartialMRUList;
import net.sourceforge.kolmafia.utilities.ScriptMRUList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MRUListComboBoxesTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("MRUListComboBoxesTest");
  }

  private static Cleanups withLists() {
    return new Cleanups(
        withProperty("testMRUList", ""),
        withProperty("testMRULength", 3),
        withProperty("testMRUDefaults", "one | two"));
  }

  private static List<String> items(final JComboBox<String> box) {
    List<String> items = new ArrayList<>();
    for (int i = 0; i < box.getItemCount(); i++) {
      items.add(box.getItemAt(i));
    }
    return items;
  }

  @Test
  void showsRecentEntriesThenDefaultsWithASeparatorBetween() {
    try (var cleanups = withLists()) {
      var list = new PartialMRUList("testMRUList", "testMRULength", "testMRUDefaults");
      Preferences.setString("testMRUList", "myst;mus");
      var box = new JComboBox<String>();

      MRUListComboBoxes.updateJComboData(list, box);

      assertThat(items(box), contains("myst", "mus", "one", "two"));
      assertThat(box.getSelectedIndex(), is(0));
      // the separator goes under the last recent entry, and nowhere else
      var renderer = box.getRenderer();
      var jlist = new JList<String>();
      assertThat(
          renderer.getListCellRendererComponent(jlist, "mus", 1, false, false),
          instanceOf(JPanel.class));
      assertThat(
          renderer.getListCellRendererComponent(jlist, "myst", 0, false, false),
          not(instanceOf(JPanel.class)));
    }
  }

  @Test
  void showsAPartialListsDefaultsWhateverTypeItIsHeldAs() {
    try (var cleanups = withLists()) {
      ScriptMRUList list = new PartialMRUList("testMRUList", "testMRULength", "testMRUDefaults");
      Preferences.setString("testMRUList", "myst");
      var box = new JComboBox<String>();

      MRUListComboBoxes.updateJComboData(list, box);

      assertThat(items(box), contains("myst", "one", "two"));
    }
  }

  @Test
  void leavesTheBoxAloneWhenAScriptListIsEmpty() {
    try (var cleanups = withLists()) {
      var list = new ScriptMRUList("testMRUList", "testMRULength");
      var box = new JComboBox<>(new String[] {"kept"});

      MRUListComboBoxes.updateJComboData(list, box);

      assertThat(items(box), contains("kept"));
    }
  }
}
