package net.sourceforge.kolmafia.swingui.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.plaf.basic.BasicListUI;
import javax.swing.plaf.basic.BasicTableUI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PopupListenerTest {
  private static class MockJPopupMenu extends JPopupMenu {
    private boolean hasRunShow = false;

    @Override
    public void show(Component invoker, int x, int y) {
      hasRunShow = true;
    }

    @Override
    public boolean isShowing() {
      return hasRunShow;
    }
  }

  private MouseEvent createPopupTrigger(Component source, Point location) {
    return createPopupTrigger(source, location, MouseEvent.MOUSE_PRESSED);
  }

  private MouseEvent createPopupTrigger(Component source, Point location, int id) {
    return new MouseEvent(
        source,
        id,
        System.currentTimeMillis(),
        InputEvent.CTRL_DOWN_MASK,
        (int) location.getX(),
        (int) location.getY(),
        1,
        true,
        MouseEvent.BUTTON1);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void showsPopupForListOnMousePressed(final boolean alreadySelected) {
    var source = new JList<>(new String[] {"Item 1", "Item 2", "Item 3"});
    source.setUI(new BasicListUI());

    if (alreadySelected) {
      source.setSelectedIndex(1);
    }

    var location = source.indexToLocation(1);

    var popupMenu = new MockJPopupMenu();
    var listener = new PopupListener(popupMenu);
    listener.mousePressed(createPopupTrigger(source, location));

    assertThat(source.getSelectedIndex(), is(1));
    assertThat(popupMenu.isShowing(), is(true));
  }

  @Test
  void showsPopupForListOnMousePressedOutOfBounds() {
    var source = new JList<>(new String[] {"Item 1", "Item 2", "Item 3"});
    source.setUI(new BasicListUI());
    var height = source.indexToLocation(1).getY();
    var location = source.indexToLocation(2);
    location.y += height + 1;

    var popupMenu = new MockJPopupMenu();
    var listener = new PopupListener(popupMenu);
    listener.mousePressed(createPopupTrigger(source, location));

    // Even though we were out of bounds, select the last row
    assertThat(source.getSelectedIndex(), is(2));
    assertThat(popupMenu.isShowing(), is(true));
  }

  @Test
  void showsPopupForListOnMousePressEmptyList() {
    var source = new JList<>(new String[] {});
    source.setUI(new BasicListUI());

    var popupMenu = new MockJPopupMenu();
    var listener = new PopupListener(popupMenu);
    listener.mousePressed(createPopupTrigger(source, new Point(0, 0)));

    assertThat(source.getSelectedIndex(), is(-1));
    assertThat(popupMenu.isShowing(), is(true));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void showsPopupForTableOnMousePressed(final boolean alreadySelected) {
    var source =
        new JTable(
            new String[][] {{"1A", "1B"}, {"2A", "2B"}, {"3A", "3B"}},
            new String[] {"ColA", "ColB"});
    source.setUI(new BasicTableUI());

    if (alreadySelected) {
      source.setRowSelectionInterval(1, 1);
    }

    var rect = source.getCellRect(1, 0, true);
    var location = new Point((int) rect.getCenterX(), (int) rect.getCenterY());
    SwingUtilities.convertPointToScreen(location, source);

    var popupMenu = new MockJPopupMenu();
    var listener = new PopupListener(popupMenu);
    listener.mousePressed(createPopupTrigger(source, location));

    assertThat(source.getSelectedRow(), is(1));
    assertThat(popupMenu.isShowing(), is(true));
  }

  @Test
  void showsPopupForTableOnMousePressedOutOfBounds() {
    var source =
        new JTable(
            new String[][] {{"1A", "1B"}, {"2A", "2B"}, {"3A", "3B"}},
            new String[] {"ColA", "ColB"});
    source.setUI(new BasicTableUI());
    var rect = source.getCellRect(2, 0, true);
    var location = new Point((int) rect.getCenterX(), (int) rect.getMaxY() + 1);
    SwingUtilities.convertPointToScreen(location, source);

    var popupMenu = new MockJPopupMenu();
    var listener = new PopupListener(popupMenu);
    listener.mousePressed(createPopupTrigger(source, location));

    // Even though we were out of bounds, select the last row
    assertThat(source.getSelectedRow(), is(2));
    assertThat(popupMenu.isShowing(), is(true));
  }

  @Test
  void showsPopupForTableOnMousePressEmptyList() {
    var source = new JTable(new String[][] {}, new String[] {"ColA", "ColB"});
    source.setUI(new BasicTableUI());

    var popupMenu = new MockJPopupMenu();
    var listener = new PopupListener(popupMenu);
    listener.mousePressed(createPopupTrigger(source, new Point(0, 0)));

    assertThat(source.getSelectedRow(), is(-1));
    assertThat(popupMenu.isShowing(), is(true));
  }

  @Test
  void showsPopupForOtherComponentsMousePressed() {
    var source = new JLabel("Hello");

    var popupMenu = new MockJPopupMenu();
    var listener = new PopupListener(popupMenu);
    listener.mousePressed(createPopupTrigger(source, source.getLocation()));

    assertThat(popupMenu.isShowing(), is(true));
  }

  @Test
  void showsPopupForOtherComponentsMouseReleased() {
    var source = new JLabel("Hello");

    var popupMenu = new MockJPopupMenu();
    var listener = new PopupListener(popupMenu);
    listener.mouseReleased(
        createPopupTrigger(source, source.getLocation(), MouseEvent.MOUSE_RELEASED));

    assertThat(popupMenu.isShowing(), is(true));
  }

  @Test
  void doesntPopOnRegularClick() {
    var source = new JList<>(new String[] {"Item 1", "Item 2", "Item 3"});
    source.setUI(new BasicListUI());
    var location = source.indexToLocation(1);

    var popupMenu = new MockJPopupMenu();
    var listener = new PopupListener(popupMenu);
    listener.mousePressed(
        new MouseEvent(
            source,
            MouseEvent.MOUSE_PRESSED,
            System.currentTimeMillis(),
            0,
            (int) location.getX(),
            (int) location.getY(),
            1,
            false,
            MouseEvent.BUTTON1));

    assertThat(source.getSelectedIndex(), is(-1));
    assertThat(popupMenu.isShowing(), is(false));
  }
}
