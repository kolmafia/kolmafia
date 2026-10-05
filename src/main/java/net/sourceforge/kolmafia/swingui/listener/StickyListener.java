package net.sourceforge.kolmafia.swingui.listener;

import java.awt.EventQueue;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.InputEvent;
import javax.swing.JScrollBar;

public class StickyListener implements AdjustmentListener {
  private final JScrollBar bar;
  private boolean sticky = true;
  private boolean userScrolling = false;
  private int previousValue = -1;

  public StickyListener(final JScrollBar bar) {
    this.bar = bar;
    bar.addAdjustmentListener(this);
  }

  public static boolean isUserEvent() {
    return EventQueue.getCurrentEvent() instanceof InputEvent;
  }

  public boolean isSticky() {
    return this.sticky;
  }

  public boolean keepsPosition() {
    return !this.sticky && !this.bar.getValueIsAdjusting();
  }

  public void contentRemovedAbove(final int height) {
    this.bar.setValue(Math.max(0, this.bar.getValue() - height));
  }

  public void scrollAsUser(final Runnable scroll) {
    this.userScrolling = true;
    try {
      scroll.run();
    } finally {
      this.userScrolling = false;
    }
  }

  @Override
  public void adjustmentValueChanged(AdjustmentEvent event) {
    var model = this.bar.getModel();
    int value = model.getValue();
    int extent = model.getExtent();
    int maximum = model.getMaximum();

    var userScrolled =
        value != this.previousValue && (this.userScrolling || StickyListener.isUserEvent());

    this.previousValue = value;

    if (userScrolled) {
      this.sticky = value + extent >= maximum;
      return;
    }

    if (extent >= maximum) {
      this.sticky = true;
      return;
    }

    if (this.sticky && value + extent < maximum) {
      this.bar.setValue(maximum - extent);
    }
  }
}
