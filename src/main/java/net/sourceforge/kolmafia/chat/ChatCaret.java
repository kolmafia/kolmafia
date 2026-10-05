package net.sourceforge.kolmafia.chat;

import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Position;
import net.sourceforge.kolmafia.swingui.listener.StickyListener;

public class ChatCaret extends DefaultCaret {
  private final StickyListener stickyListener;
  private boolean selecting = false;
  private boolean movedByUser = false;

  public ChatCaret(final StickyListener stickyListener) {
    this.stickyListener = stickyListener;
  }

  @Override
  public void mousePressed(MouseEvent e) {
    this.selecting = true;
    super.mousePressed(e);
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    super.mouseReleased(e);
    this.selecting = false;
  }

  @Override
  public void focusGained(FocusEvent e) {
    super.focusGained(e);
    this.setVisible(false);
  }

  @Override
  public void setDot(int dot, Position.Bias dotBias) {
    this.movedByUser = this.isUserMove();
    super.setDot(dot, dotBias);
  }

  @Override
  public void moveDot(int dot, Position.Bias dotBias) {
    this.movedByUser = this.isUserMove();
    super.moveDot(dot, dotBias);
  }

  private boolean isUserMove() {
    return this.selecting || StickyListener.isUserEvent();
  }

  @Override
  protected void adjustVisibility(Rectangle nloc) {
    if (this.movedByUser) {
      this.stickyListener.scrollAsUser(() -> super.adjustVisibility(nloc));
    }
  }
}
