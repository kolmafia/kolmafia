package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.swingui.widget.AutoFilterTextField;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

public class ScrollableFilteredPanel<E> extends ScrollablePanel {
  private final AutoFilterTextField filterfield;
  public ShowDescriptionList<E> elementList;

  public ScrollableFilteredPanel(
      final String title,
      final String confirmedText,
      final String cancelledText,
      final ShowDescriptionList<E> scrollComponent) {
    super(title, confirmedText, cancelledText, scrollComponent);
    this.elementList = (ShowDescriptionList<E>) this.scrollComponent;
    this.filterfield = new AutoFilterTextField(this.elementList);
    JPanel topPanel = new JPanel(new BorderLayout());

    if (!title.equals("")) {
      this.titleComponent =
          JComponentUtilities.createLabel(title, SwingConstants.CENTER, Color.black, Color.white);
      topPanel.add(this.titleComponent, BorderLayout.NORTH);
    }
    topPanel.add(this.filterfield, BorderLayout.CENTER);
    this.centerPanel.add(topPanel, BorderLayout.NORTH);
    this.filterItems();
  }

  public void filterItems() {
    this.filterfield.update();
  }
}
