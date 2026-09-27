package net.sourceforge.kolmafia.swingui.listener;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.JEditorPane;
import javax.swing.JScrollBar;
import net.java.dev.spellcast.utilities.ChatBuffer;

public class StickyListener implements AdjustmentListener {
  private final ChatBuffer buffer;
  private final JEditorPane editor;
  private final int tolerance;
  private boolean currentlySticky;

  public StickyListener(ChatBuffer buffer, JEditorPane editor, int tolerance) {
    this.buffer = buffer;
    this.editor = editor;
    this.tolerance = tolerance;
    this.currentlySticky = true;
  }

  @Override
  public void adjustmentValueChanged(AdjustmentEvent event) {
    JScrollBar bar = (JScrollBar) event.getSource();

    int value = event.getValue();
    int knob = bar.getVisibleAmount();
    int max = bar.getMaximum();

    boolean shouldBeSticky = value + knob > max - tolerance;

    if (this.currentlySticky != shouldBeSticky) {
      this.currentlySticky = shouldBeSticky;
      buffer.setSticky(this.editor, shouldBeSticky);
    }
  }
}
