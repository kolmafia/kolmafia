package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Component;
import java.awt.Font;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.text.JTextComponent;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.preferences.Preferences;

public class GenericScrollPane extends JScrollPane {
  private final Component component;

  public GenericScrollPane(final LockableListModel<? extends Object> model) {
    this(model, 8);
  }

  public GenericScrollPane(final LockableListModel<? extends Object> model, final int visibleRows) {
    this(
        new ShowDescriptionList<>(model, visibleRows),
        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
  }

  public GenericScrollPane(final Component view) {
    this(
        view,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
  }

  public GenericScrollPane(final Component view, final int hsbPolicy) {
    this(view, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, hsbPolicy);
  }

  public GenericScrollPane(final Component view, final int vsbPolicy, final int hsbPolicy) {
    super(view, vsbPolicy, hsbPolicy);
    this.setOpaque(true);

    if (view instanceof JList) {
      if (Preferences.getString("swingLookAndFeel")
          .equals(UIManager.getCrossPlatformLookAndFeelClassName())) {
        Font font = view.getFont().deriveFont(Font.PLAIN);
        view.setFont(view.getFont() instanceof UIResource ? new FontUIResource(font) : font);
      }
    } else if (!(view instanceof JTextComponent)) {
      this.getVerticalScrollBar().setUnitIncrement(30);
    }

    this.component = view;
  }

  public Component getComponent() {
    return this.component;
  }
}
