package net.sourceforge.kolmafia.swingui.widget;

import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import net.sourceforge.kolmafia.chat.ChatBuffer;
import net.sourceforge.kolmafia.swingui.listener.StickyListener;

/** Shows a {@link ChatBuffer} in a {@link JEditorPane}, keeping the reader's scroll position. */
public class ChatBufferDisplay implements ChatBuffer.Display {
  private final JEditorPane displayPane;
  private final StickyListener stickyListener;

  private ChatBufferDisplay(final JEditorPane displayPane, final StickyListener stickyListener) {
    this.displayPane = displayPane;
    this.stickyListener = stickyListener;
  }

  /** Adds a chat display used to display the chat messages currently being stored in the buffer. */
  public static JScrollPane addDisplay(final ChatBuffer buffer, final JEditorPane displayPane) {
    return ChatBufferDisplay.addDisplay(buffer, displayPane, false);
  }

  public static JScrollPane addStatusLine(final ChatBuffer buffer, final JEditorPane displayPane) {
    return ChatBufferDisplay.addDisplay(buffer, displayPane, true);
  }

  private static JScrollPane addDisplay(
      final ChatBuffer buffer, final JEditorPane displayPane, final boolean statusLine) {
    if (displayPane == null) {
      return null;
    }

    displayPane.setContentType("text/html");
    displayPane.setEditable(false);

    JScrollPane scroller =
        new JScrollPane(
            displayPane,
            statusLine
                ? ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
                : ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    var stickyListener = new StickyListener(scroller.getVerticalScrollBar());
    displayPane.putClientProperty(StickyListener.class, stickyListener);
    displayPane.setCaret(new ChatCaret(stickyListener));

    var display = new ChatBufferDisplay(displayPane, stickyListener);
    // The buffer only holds weak references to its displays; this listener keeps the display alive
    // as long as the pane.
    displayPane.addHierarchyListener(
        e -> {
          if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0
              && displayPane.isDisplayable()) {
            SwingUtilities.invokeLater(() -> buffer.markStale(display));
          }
        });

    buffer.addDisplay(display, statusLine);

    return scroller;
  }

  @Override
  public boolean isDisplayable() {
    return this.displayPane.isDisplayable();
  }

  @Override
  public void setText(final String html) {
    this.displayPane.setText(html);
  }

  @Override
  public boolean update(final int removals, final String added) {
    return ChatBufferDisplay.update(this.displayPane, this.stickyListener, removals, added);
  }

  @Override
  public void flushed() {
    // Non-ASCII text sets "multiByte", which switches to a much slower bidi-aware layout.
    this.displayPane.getDocument().putProperty("multiByte", Boolean.FALSE);
  }

  private static boolean update(
      final JEditorPane displayPane,
      final StickyListener stickyListener,
      final int removals,
      final String added) {
    HTMLDocument currentHTML = (HTMLDocument) displayPane.getDocument();
    Element body =
        currentHTML.getElement(
            currentHTML.getDefaultRootElement(), StyleConstants.NameAttribute, HTML.Tag.BODY);

    if (body == null) {
      return false;
    }

    List<Element> entryElements = new ArrayList<>();

    for (int i = 0; i < body.getElementCount() && entryElements.size() <= removals; i++) {
      Element child = body.getElement(i);

      if (child.getAttributes().getAttribute(StyleConstants.NameAttribute) == HTML.Tag.DIV) {
        entryElements.add(child);
      }
    }

    if (entryElements.size() <= removals) {
      return false;
    }

    int removedHeight =
        removals > 0 && stickyListener.keepsPosition()
            ? ChatBufferDisplay.top(displayPane, entryElements.get(removals))
                - ChatBufferDisplay.top(displayPane, entryElements.get(0))
            : 0;

    try {
      for (int i = 0; i < removals; i++) {
        currentHTML.removeElement(entryElements.get(i));
      }

      if (!added.isEmpty()) {
        currentHTML.insertBeforeEnd(body, added);
      }
    } catch (BadLocationException | IOException e) {
      return false;
    }

    if (removedHeight > 0) {
      stickyListener.contentRemovedAbove(removedHeight);
    }

    return true;
  }

  private static int top(final JEditorPane displayPane, final Element element) {
    try {
      var bounds = displayPane.modelToView2D(element.getStartOffset());
      return bounds == null ? 0 : (int) bounds.getY();
    } catch (BadLocationException e) {
      return 0;
    }
  }
}
