package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.SwingConstants;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public abstract class LabeledPanel extends GenericPanel {
  private final String panelTitle;

  public LabeledPanel(final String panelTitle, final Dimension left, final Dimension right) {
    super(left, right, panelTitle != null && !panelTitle.equals(""));
    this.panelTitle = panelTitle;
  }

  public LabeledPanel(
      final String panelTitle,
      final String confirmButton,
      final Dimension left,
      final Dimension right) {
    super(confirmButton, left, right, panelTitle != null && !panelTitle.equals(""));
    this.panelTitle = panelTitle;
  }

  public LabeledPanel(
      final String panelTitle,
      final String confirmButton,
      final String cancelButton,
      final Dimension left,
      final Dimension right) {
    super(confirmButton, cancelButton, left, right, panelTitle != null && !panelTitle.equals(""));
    this.panelTitle = panelTitle;
  }

  @Override
  public void setContent(final VerifiableElement[] elements, final boolean bothDisabledOnClick) {
    super.setContent(elements, bothDisabledOnClick);

    if (this.panelTitle != null && !this.panelTitle.equals("")) {
      this.add(
          JComponentUtilities.createLabel(
              this.panelTitle, SwingConstants.CENTER, Color.black, Color.white),
          BorderLayout.NORTH);
    }
  }

  @Override
  public void actionCancelled() {}

  @Override
  public boolean shouldAddStatusLabel() {
    return false;
  }
}
