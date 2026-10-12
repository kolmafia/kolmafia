package net.sourceforge.kolmafia.swingui.widget;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class FindBarTest {
  private RequestPane pane;
  private FindBar findBar;
  private boolean closed;

  private void onEdt(Executable action) throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          try {
            action.execute();
          } catch (Throwable e) {
            throw new RuntimeException(e);
          }
        });
  }

  private int highlightCount() {
    return pane.getHighlighter().getHighlights().length;
  }

  private String text() throws BadLocationException {
    return pane.getDocument().getText(0, pane.getDocument().getLength());
  }

  @BeforeEach
  void createFindBar() throws Exception {
    closed = false;
    onEdt(
        () -> {
          pane = new RequestPane();
          pane.setText("<html><body>Apple banana APPLE cherry apple</body></html>");
          findBar = new FindBar(pane, () -> closed = true);
          findBar.open();
        });
  }

  @Test
  void findsMatchesIgnoringCase() throws Exception {
    onEdt(() -> findBar.find("apple"));

    assertThat(findBar.matchCount(), is(3));
    assertThat(findBar.currentIndex(), is(0));
    assertThat(highlightCount(), is(1));
  }

  @Test
  void nextAndPreviousWrapAround() throws Exception {
    onEdt(() -> findBar.find("apple"));

    onEdt(findBar::previous);
    assertThat(findBar.currentIndex(), is(2));

    onEdt(findBar::next);
    assertThat(findBar.currentIndex(), is(0));

    onEdt(findBar::next);
    assertThat(findBar.currentIndex(), is(1));
  }

  @Test
  void noMatches() throws Exception {
    onEdt(() -> findBar.find("durian"));

    assertThat(findBar.matchCount(), is(0));
    assertThat(findBar.currentIndex(), is(-1));
  }

  @Test
  void closingClearsHighlights() throws Exception {
    onEdt(() -> findBar.find("apple"));
    onEdt(findBar::close);

    assertThat(highlightCount(), is(0));
    assertThat(findBar.isVisible(), is(false));
    assertThat(closed, is(true));
  }

  @Test
  void picksUpNewOutputWithoutLosingPlace() throws Exception {
    onEdt(() -> findBar.find("apple"));
    onEdt(findBar::next);

    onEdt(() -> pane.getDocument().insertString(pane.getDocument().getLength(), " apple", null));

    assertThat(findBar.matchCount(), is(4));
    assertThat(findBar.currentIndex(), is(1));
  }

  @Test
  void matchesStayOnTheirTextInTheSameTurnAsAnEdit() throws Exception {
    onEdt(() -> findBar.find("apple"));
    onEdt(findBar::next);

    var matched = new String[1];
    onEdt(
        () -> {
          pane.getDocument().remove(1, "Apple banana ".length());
          matched[0] = text().substring(findBar.currentOffset(), findBar.currentOffset() + 5);
        });

    assertThat(matched[0], is("APPLE"));
  }

  @Test
  void followsCurrentMatchWhenOutputIsRemovedAbove() throws Exception {
    onEdt(() -> findBar.find("apple"));
    onEdt(findBar::next);

    onEdt(() -> pane.getDocument().remove(1, "Apple banana ".length()));

    assertThat(findBar.matchCount(), is(2));
    assertThat(findBar.currentIndex(), is(0));
    assertThat(text().substring(findBar.currentOffset()).startsWith("APPLE"), is(true));
  }

  @Test
  void findsMatchesFormedAcrossAnEdit() throws Exception {
    onEdt(() -> findBar.find("apple"));

    onEdt(() -> pane.getDocument().insertString(pane.getDocument().getLength(), " app", null));
    onEdt(() -> pane.getDocument().insertString(pane.getDocument().getLength(), "le", null));

    assertThat(findBar.matchCount(), is(4));
  }

  @Test
  void dropsMatchesBrokenByAnEdit() throws Exception {
    onEdt(() -> findBar.find("apple"));

    onEdt(() -> pane.getDocument().insertString(text().lastIndexOf("apple") + 2, "x", null));

    assertThat(findBar.matchCount(), is(2));
  }

  @Test
  void rescansReplacedOutput() throws Exception {
    onEdt(() -> findBar.find("apple"));

    onEdt(() -> pane.setText("<html><body>apple apple</body></html>"));

    assertThat(findBar.matchCount(), is(2));
  }

  @Test
  void ignoresOutputWhileClosed() throws Exception {
    onEdt(() -> findBar.find("apple"));
    onEdt(findBar::close);

    onEdt(() -> pane.getDocument().insertString(pane.getDocument().getLength(), " apple", null));
    onEdt(findBar::open);

    assertThat(findBar.matchCount(), is(4));
  }

  @Nested
  class Shortcut {
    private final JTextField entry = new JTextField();

    @BeforeEach
    void installShortcut() throws Exception {
      onEdt(
          () -> {
            var root = new JPanel();
            root.add(entry);
            root.add(pane);
            findBar.installShortcut(root);
            findBar.close();
          });
    }

    private void pressShortcut(JComponent target) throws Exception {
      onEdt(
          () ->
              SwingUtilities.processKeyBindings(
                  new KeyEvent(
                      target,
                      KeyEvent.KEY_PRESSED,
                      0,
                      Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx(),
                      KeyEvent.VK_F,
                      KeyEvent.CHAR_UNDEFINED)));
    }

    @Test
    void menuShortcutOpensFromTheEntryField() throws Exception {
      pressShortcut(entry);

      assertThat(findBar.isVisible(), is(true));
    }

    @Test
    void menuShortcutOpensFromTheOutput() throws Exception {
      pressShortcut(pane);

      assertThat(findBar.isVisible(), is(true));
    }
  }
}
