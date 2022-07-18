package net.sourceforge.kolmafia.swingui;

import static internal.helpers.Player.setProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.OptionsFrame.ConfigQueuingPanel;
import net.sourceforge.kolmafia.swingui.OptionsFrame.PreferenceCheckBox;
import net.sourceforge.kolmafia.swingui.OptionsFrame.PreferenceIntegerTextField;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class OptionsFrameTest {

  @BeforeAll
  private static void beforeAll() {
    Preferences.saveSettingsToFile = false;
  }

  @AfterAll
  private static void afterAll() {
    Preferences.saveSettingsToFile = true;
  }

  @Nested
  class PreferenceCheckBoxTest {
    @BeforeEach
    private void beforeEach() {
      KoLCharacter.reset("checkbox");
    }

    @AfterEach
    private void afterEach() {
      PreferenceListenerRegistry.reset();
    }

    private ConfigQueuingPanel makePanel(String... preferences) {
      ConfigQueuingPanel panel = new ConfigQueuingPanel();

      for (String pref : preferences) {
        String tip = "<html>" + pref + "</html>";
        PreferenceCheckBox checkbox = new PreferenceCheckBox(pref, pref, tip);
        panel.queue(checkbox);
      }
      panel.makeLayout();
      return panel;
    }

    private void fireActionListeners(JCheckBox box) {
      for (ActionListener a : box.getActionListeners()) {
        a.actionPerformed(new ActionEvent(box, ActionEvent.ACTION_PERFORMED, null) {});
      }
    }

    @Test
    void preferenceCheckBoxListensForPreferenceChange() {
      String pref = "preference1";
      ConfigQueuingPanel panel = makePanel(pref);

      Component[] components = panel.getComponents();
      assertEquals(1, components.length);
      assertTrue(components[0] instanceof PreferenceCheckBox);
      PreferenceCheckBox checkbox = (PreferenceCheckBox) components[0];
      JCheckBox box = checkbox.getCheckBox();
      assertFalse(box.isSelected());

      var cleanups = new Cleanups(setProperty(pref, true));
      try (cleanups) {
        assertTrue(box.isSelected());
      }
      assertFalse(box.isSelected());
    }

    @Test
    void preferenceCheckBoxSetsPreference() {
      String pref = "preference2";
      ConfigQueuingPanel panel = makePanel(pref);

      Component[] components = panel.getComponents();
      assertEquals(1, components.length);
      assertTrue(components[0] instanceof PreferenceCheckBox);
      PreferenceCheckBox checkbox = (PreferenceCheckBox) components[0];
      JCheckBox box = checkbox.getCheckBox();

      var cleanups = new Cleanups(setProperty(pref, false));
      try (cleanups) {
        assertFalse(box.isSelected());
        assertFalse(Preferences.getBoolean(pref));
        box.setSelected(true);
        fireActionListeners(box);
        assertTrue(box.isSelected());
        assertTrue(Preferences.getBoolean(pref));
      }
      assertFalse(box.isSelected());
      assertFalse(Preferences.getBoolean(pref));
    }
  }

  @Nested
  class PreferenceIntegerTextFieldTest {
    @BeforeEach
    private void beforeEach() {
      KoLCharacter.reset("textfield");
    }

    @AfterEach
    private void afterEach() {
      PreferenceListenerRegistry.reset();
    }

    private ConfigQueuingPanel makePanel(String... preferences) {
      ConfigQueuingPanel panel = new ConfigQueuingPanel();

      for (String pref : preferences) {
        String tip = "<html>" + pref + "</html>";
        PreferenceIntegerTextField textfield = new PreferenceIntegerTextField(pref, 4, pref, tip);
        panel.queue(textfield);
      }
      panel.makeLayout();
      return panel;
    }

    private void fireFocusListeners(Component component, boolean gained) {
      for (FocusListener f : component.getFocusListeners()) {
        if (gained) {
          f.focusGained(new FocusEvent(component, 1));
        } else {
          f.focusLost(new FocusEvent(component, 1));
        }
      }
    }

    @Test
    void preferenceTextFieldListensForPreferenceChange() {
      String pref = "preference3";
      ConfigQueuingPanel panel = makePanel(pref);

      Component[] components = panel.getComponents();
      assertEquals(1, components.length);
      assertTrue(components[0] instanceof PreferenceIntegerTextField);
      PreferenceIntegerTextField textfield = (PreferenceIntegerTextField) components[0];
      JTextField field = textfield.getTextField();
      assertEquals("0", field.getText());

      var cleanups = new Cleanups(setProperty(pref, 10));
      try (cleanups) {
        assertEquals("10", field.getText());
      }
      assertEquals("0", field.getText());
    }

    @Test
    void preferenceTextFieldSetsPreference() {
      String pref = "preference4";
      ConfigQueuingPanel panel = makePanel(pref);

      Component[] components = panel.getComponents();
      assertEquals(1, components.length);
      assertTrue(components[0] instanceof PreferenceIntegerTextField);
      PreferenceIntegerTextField textfield = (PreferenceIntegerTextField) components[0];
      JTextField field = textfield.getTextField();
      assertEquals("0", field.getText());

      var cleanups = new Cleanups(setProperty(pref, 10));
      try (cleanups) {
        assertEquals("10", field.getText());
        field.setText("20");
        // fire listener
        fireFocusListeners(field, false);
        assertEquals("20", field.getText());
        assertEquals(20, Preferences.getInteger(pref));
      }
      assertEquals("0", field.getText());
      assertEquals(0, Preferences.getInteger(pref));
    }
  }
}
