package net.sourceforge.kolmafia.swingui.widget;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.java.dev.spellcast.utilities.LockableListModel;
import org.junit.jupiter.api.Test;

public class AutoFilterTextFieldTest {
  @Test
  public void upperCaseStringIsVisible() {
    AutoFilterTextField<String> autoFilterTextField =
        new AutoFilterTextField<>(new LockableListModel<>());

    autoFilterTextField.setText("a");

    assertTrue(autoFilterTextField.isVisible("ALPHA"));
  }
}
