package net.sourceforge.kolmafia.swingui.widget;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
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

  @Test
  public void contentsChangedCoversOnlyVisibleElements() {
    LockableListModel<String> model = new LockableListModel<>();
    model.add("ALPHA");
    model.add("BETA");

    AutoFilterTextField<String> autoFilterTextField = new AutoFilterTextField<>(model);
    autoFilterTextField.setText("ALPHA");
    assertThat(model.getSize(), equalTo(1));

    List<ListDataEvent> events = new ArrayList<>();
    model.addListDataListener(
        new ListDataListener() {
          @Override
          public void intervalAdded(ListDataEvent e) {}

          @Override
          public void intervalRemoved(ListDataEvent e) {}

          @Override
          public void contentsChanged(ListDataEvent e) {
            events.add(e);
          }
        });

    autoFilterTextField.fireContentsChanged();

    assertThat(events.size(), equalTo(1));
    assertThat(events.get(0).getIndex0(), equalTo(0));
    assertThat(events.get(0).getIndex1(), equalTo(0));
  }
}
