package net.sourceforge.kolmafia.chat;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import internal.helpers.Cleanups;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import net.sourceforge.kolmafia.swingui.listener.StickyListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ChatBufferTest {
  private static class TestPane extends JEditorPane {
    private boolean displayable = true;

    @Override
    public boolean isDisplayable() {
      return this.displayable;
    }
  }

  private Cleanups cleanups;

  @BeforeEach
  void pinBufferLength() {
    cleanups = withProperty("outputBufferLength", 50000);
  }

  @AfterEach
  void restoreBufferLength() {
    cleanups.close();
  }

  private static void flush() throws Exception {
    SwingUtilities.invokeAndWait(() -> {});
    SwingUtilities.invokeAndWait(() -> {});
  }

  private static String text(JEditorPane pane) throws BadLocationException {
    return pane.getDocument().getText(0, pane.getDocument().getLength());
  }

  private static String freshText(ChatBuffer buffer) throws BadLocationException {
    return text(new JEditorPane("text/html", buffer.getHTMLContent()));
  }

  private static String line(int i) {
    return "Line " + i + " " + "x".repeat(390) + "<br>";
  }

  @Nested
  class BalanceTags {
    @Test
    void closesUnclosedTags() {
      assertThat(
          ChatBuffer.balanceTags("<font color=red><b>hi"), is("<font color=red><b>hi</b></font>"));
    }

    @Test
    void closesTagsLeftOpenInsideAClosedTag() {
      assertThat(ChatBuffer.balanceTags("<font><b>hi</font>"), is("<font><b>hi</b></font>"));
    }

    @Test
    void ignoresLineBreaks() {
      assertThat(ChatBuffer.balanceTags("a<br>b<br>"), is("a<br>b<br>"));
    }

    @Test
    void stripsComments() {
      assertThat(ChatBuffer.balanceTags("a<!-- <b> -->b"), is("ab"));
    }
  }

  @Nested
  class Content {
    @Test
    void wrapsEachEntryInADiv() throws Exception {
      var buffer = new ChatBuffer("test");
      buffer.append("one<br>");
      buffer.append("  two<br>  ");
      flush();

      assertThat(buffer.getContent(), is("one<br>two<br>"));
      assertThat(
          buffer.getHTMLContent(),
          containsString("<body><div>one<br></div><div>two<br></div></body>"));
    }

    @Test
    void ignoresBlankAppends() throws Exception {
      var buffer = new ChatBuffer("test");
      buffer.append("   ");
      flush();

      assertThat(buffer.getContent(), is(""));
    }

    @Test
    void trimsOldestEntriesWhenFull() throws Exception {
      var buffer = new ChatBuffer("test");
      for (int i = 0; i < 150; i++) {
        buffer.append(line(i));
      }

      flush();

      String content = buffer.getContent();
      assertThat(content.length(), lessThan(50000));
      assertThat(content, not(containsString("Line 0 ")));
      assertThat(content, containsString("Line 149 "));
    }

    @Test
    void clearRemovesEverything() throws Exception {
      var buffer = new ChatBuffer("test");
      buffer.append("one<br>");
      buffer.clear();
      flush();

      assertThat(buffer.getContent(), is(""));
    }

    @Test
    void keepsALargeEntryAndRecentHistoryWhenMoreOutputFollows() throws Exception {
      var buffer = new ChatBuffer("test");
      buffer.append("earlier<br>");
      buffer.append("big " + "y".repeat(60000) + "<br>");
      buffer.append("Returned: void<br>");
      flush();

      assertThat(buffer.getContent(), containsString("earlier"));
      assertThat(buffer.getContent(), containsString("big "));
      assertThat(buffer.getContent(), containsString("Returned: void"));
    }

    @Test
    void eventuallyTrimsALargeEntry() throws Exception {
      var buffer = new ChatBuffer("test");
      buffer.append("big " + "y".repeat(60000) + "<br>");
      for (int i = 0; i < 100; i++) {
        buffer.append("line " + i + "<br>");
      }
      flush();

      assertThat(buffer.getContent(), not(containsString("big ")));
      assertThat(buffer.getContent(), containsString("line 99"));
    }

    @Test
    void capsRetainedContentWhenEntriesAreLarge() throws Exception {
      var buffer = new ChatBuffer("test");
      for (int i = 0; i < 100; i++) {
        buffer.append("big " + i + " " + "y".repeat(5000) + "<br>");
      }
      flush();

      assertThat(buffer.getContent().length(), lessThan(200000));
      assertThat(buffer.getContent(), containsString("big 99 "));
    }

    @Test
    void trimsAtTheConfiguredLength() throws Exception {
      try (var ignored = withProperty("outputBufferLength", 200000)) {
        var buffer = new ChatBuffer("test");
        for (int i = 0; i < 150; i++) {
          buffer.append(line(i));
        }
        flush();

        assertThat(buffer.getContent(), containsString("Line 0 "));
      }
    }

    @Test
    void trimsBelowTheDefaultWhenConfiguredLower() throws Exception {
      try (var ignored = withProperty("outputBufferLength", 20000)) {
        var buffer = new ChatBuffer("test");
        for (int i = 0; i < 3000; i++) {
          buffer.append("line " + i + "<br>");
        }
        flush();

        assertThat(buffer.getContent().length(), lessThan(20000));
        assertThat(buffer.getContent(), containsString("line 2999<br>"));
      }
    }

    @Test
    void returnsTheRawAppendedContent() throws Exception {
      var buffer = new ChatBuffer("test");
      buffer.append("foo <img src=x.gif><br>");
      flush();

      assertThat(buffer.getContent(), is("foo <img src=x.gif><br>"));
    }
  }

  @Nested
  class Display {
    private final ChatBuffer buffer = new ChatBuffer("test");
    private final TestPane pane = new TestPane();
    private JScrollPane scroller;

    @BeforeEach
    void addDisplay() {
      scroller = buffer.addDisplay(pane);
      scroller.setSize(600, 400);
    }

    @Test
    void showsEntriesAppendedBeforeTheDisplayWasAdded() throws Exception {
      var earlier = new ChatBuffer("test");
      var earlierPane = new TestPane();
      earlier.append("before<br>");
      earlier.addDisplay(earlierPane);
      earlier.append("after<br>");
      flush();

      assertThat(text(earlierPane), containsString("before"));
      assertThat(text(earlierPane), containsString("after"));
    }

    @Test
    void coalescesAppendsIntoOneInsert() throws Exception {
      flush();

      var inserts = new AtomicInteger();
      pane.getDocument()
          .addDocumentListener(
              new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                  inserts.incrementAndGet();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {}

                @Override
                public void changedUpdate(DocumentEvent e) {}
              });

      SwingUtilities.invokeAndWait(
          () -> {
            buffer.append("one<br>");
            buffer.append("two<br>");
            buffer.append("three<br>");
          });
      flush();

      assertThat(inserts.get(), is(1));
      assertThat(text(pane), containsString("three"));
    }

    @Test
    void trimmingMatchesAFreshRender() throws Exception {
      for (int i = 0; i < 150; i++) {
        buffer.append(line(i));
        flush();
      }

      assertThat(text(pane), equalTo(freshText(buffer)));
      assertThat(text(pane).trim(), startsWith("Line "));
      assertThat(text(pane), not(containsString("Line 0 ")));
    }

    @Test
    void appendedEntriesRenderAtTheSameHeightAsAFreshRender() throws Exception {
      buffer.append("<font color=red>Something went wrong</font><br>");
      flush();
      buffer.append("You acquire an item: <b>seal-clubbing club</b><br>");
      flush();
      buffer.append("<br>");
      flush();
      buffer.append(" <font color=olive>&gt; adv1 the haunted kitchen</font><br>");
      flush();

      var heights = new int[2];
      SwingUtilities.invokeAndWait(
          () -> {
            var fresh = new JEditorPane("text/html", buffer.getHTMLContent());
            pane.setSize(600, 10);
            fresh.setSize(600, 10);
            heights[0] = pane.getPreferredSize().height;
            heights[1] = fresh.getPreferredSize().height;
          });

      assertThat(heights[0], is(heights[1]));
    }

    @Test
    void entriesFollowingATableAreNotPlacedInsideIt() throws Exception {
      buffer.append("<table><tr><td>cell</td></tr></table>");
      buffer.append("after<br>");
      flush();

      assertThat(text(pane), equalTo(freshText(buffer)));
    }

    @Test
    void resetReplacesDisplayedContent() throws Exception {
      buffer.append("one<br>");
      flush();
      buffer.clear();
      buffer.append("two<br>");
      flush();

      assertThat(text(pane), not(containsString("one")));
      assertThat(text(pane), containsString("two"));
    }

    @Test
    void hiddenPanesCatchUpWhenShown() throws Exception {
      var hidden = new TestPane();
      hidden.displayable = false;
      buffer.addDisplay(hidden);
      buffer.append("one<br>");
      flush();

      assertThat(text(hidden), not(containsString("one")));

      hidden.displayable = true;
      hidden.dispatchEvent(
          new HierarchyEvent(
              hidden,
              HierarchyEvent.HIERARCHY_CHANGED,
              hidden,
              null,
              HierarchyEvent.DISPLAYABILITY_CHANGED));
      flush();

      assertThat(text(hidden), containsString("one"));
    }

    @Test
    void appendingNullRerendersDisplays() throws Exception {
      buffer.append("one<br>");
      flush();
      SwingUtilities.invokeAndWait(() -> pane.setText("stale"));

      buffer.append(null);
      flush();

      assertThat(text(pane), containsString("one"));
      assertThat(text(pane), not(containsString("stale")));
    }

    @Test
    void stickyPanesFollowOutput() throws Exception {
      appendLines(0, 120);

      var bar = scroller.getVerticalScrollBar();
      assertThat(bar.getValue() + bar.getVisibleAmount(), is(bar.getMaximum()));
    }

    @Test
    void selectionsSurviveTrimming() throws Exception {
      appendLines(0, 120);
      var start = text(pane).indexOf("Line 100 ");
      SwingUtilities.invokeAndWait(
          () -> {
            scrollAsUser(pane.getHeight() / 2);
            pane.select(start, start + 8);
          });
      flush();

      appendLines(120, 160);

      assertThat(text(pane), not(containsString("Line 0 ")));
      assertThat(pane.getSelectedText(), is("Line 100"));
    }

    @Test
    void unstickyPanesKeepTheirScrollPositionWhenTrimmed() throws Exception {
      appendLines(0, 120);
      SwingUtilities.invokeAndWait(() -> scrollAsUser(pane.getHeight() / 2));
      flush();
      var before = topLine();

      appendLines(120, 160);

      assertThat(text(pane), not(containsString("Line 0 ")));
      assertThat(topLine(), is(before));
    }

    @Test
    void scrollingUpWhileOutputStreamsUnsticks() throws Exception {
      appendLines(0, 60);

      SwingUtilities.invokeAndWait(
          () -> {
            buffer.append(line(60));
            scrollAsUser(0);
          });
      flush();
      appendLines(61, 80);

      assertThat(scroller.getVerticalScrollBar().getValue(), is(0));
    }

    @Test
    void caretMovesFromInputScrollTheView() throws Exception {
      appendLines(0, 60);
      pane.addMouseMotionListener(
          new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
              pane.setCaretPosition(0);
            }
          });

      Toolkit.getDefaultToolkit()
          .getSystemEventQueue()
          .postEvent(new MouseEvent(pane, MouseEvent.MOUSE_MOVED, 0, 0, 0, 0, 0, false));
      flush();
      flush();

      var caretVisible = new boolean[1];
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              var caret = pane.modelToView2D(0).getBounds();
              var view = scroller.getViewport().getViewRect();
              caretVisible[0] = caret.y >= view.y && caret.y + caret.height <= view.y + view.height;
            } catch (BadLocationException e) {
              caretVisible[0] = false;
            }
          });

      assertThat(caretVisible[0], is(true));
    }

    @Test
    void caretMovesFromTrimmingDoNotScrollTheView() throws Exception {
      appendLines(0, 120);
      var position = text(pane).indexOf("Line 100 ");
      SwingUtilities.invokeAndWait(() -> pane.setCaretPosition(position));
      flush();

      appendLines(120, 160);

      var bar = scroller.getVerticalScrollBar();
      assertThat(bar.getValue() + bar.getVisibleAmount(), is(bar.getMaximum()));
    }

    private void appendLines(int from, int to) throws Exception {
      for (int i = from; i < to; i++) {
        buffer.append(line(i));
        flush();
        SwingUtilities.invokeAndWait(this::layout);
        flush();
      }
    }

    @Test
    void statusLinesShowOnlyTheLatestEntriesWithoutAScrollbar() throws Exception {
      var statusLine = new TestPane();
      var statusScroller = buffer.addStatusLine(statusLine);
      for (int i = 0; i < 50; i++) {
        buffer.append("Entry " + i + "<br>");
      }
      flush();

      assertThat(text(statusLine), containsString("Entry 49"));
      assertThat(text(statusLine), containsString("Entry 40"));
      assertThat(text(statusLine), not(containsString("Entry 39")));
      assertThat(
          statusScroller.getVerticalScrollBarPolicy(),
          is(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER));
    }

    @Test
    void caretStaysHiddenWhenFocused() throws Exception {
      SwingUtilities.invokeAndWait(
          () ->
              ((DefaultCaret) pane.getCaret())
                  .focusGained(new FocusEvent(pane, FocusEvent.FOCUS_GAINED)));

      assertThat(pane.getCaret().isVisible(), is(false));
    }

    private void scrollAsUser(int y) {
      ((StickyListener) pane.getClientProperty(StickyListener.class))
          .scrollAsUser(() -> scroller.getViewport().setViewPosition(new Point(0, y)));
    }

    private void layout() {
      scroller.doLayout();
      scroller.getViewport().doLayout();
      pane.setSize(pane.getWidth(), pane.getPreferredSize().height);
    }

    private String topLine() throws Exception {
      var line = new String[1];
      SwingUtilities.invokeAndWait(
          () -> {
            var offset = pane.viewToModel2D(scroller.getViewport().getViewPosition());
            try {
              line[0] = pane.getDocument().getText(offset, 8);
            } catch (BadLocationException e) {
              line[0] = null;
            }
          });
      return line[0];
    }

    @Test
    void trimsDisplayedEntriesWithoutRerendering() throws Exception {
      for (int i = 0; i < 100; i++) {
        buffer.append(line(i));
      }
      flush();
      var document = pane.getDocument();

      SwingUtilities.invokeAndWait(
          () -> {
            for (int i = 100; i < 140; i++) {
              buffer.append(line(i));
            }
          });
      flush();

      assertThat(pane.getDocument(), is(document));
      assertThat(text(pane), not(containsString("Line 0 ")));
      assertThat(text(pane), equalTo(freshText(buffer)));
    }

    @Test
    void rerendersWhenTrimmingEverythingDisplayed() throws Exception {
      for (int i = 0; i < 110; i++) {
        buffer.append(line(i));
      }
      flush();

      SwingUtilities.invokeAndWait(
          () -> {
            for (int i = 110; i < 310; i++) {
              buffer.append(line(i));
            }
          });
      flush();

      assertThat(text(pane), not(containsString("Line 109 ")));
      assertThat(text(pane), equalTo(freshText(buffer)));
    }

    @Test
    void showsAppendsFromOtherThreads() throws Exception {

      var threads = new ArrayList<Thread>();
      for (int t = 0; t < 4; t++) {
        int id = t;
        threads.add(
            Thread.ofPlatform()
                .start(
                    () -> {
                      for (int i = 0; i < 100; i++) {
                        buffer.append("t" + id + "-" + i + "<br>");
                      }
                    }));
      }
      for (var thread : threads) {
        thread.join();
      }
      flush();

      assertThat(text(pane), equalTo(freshText(buffer)));
      assertThat(buffer.getContent().split("<br>").length, is(400));
    }
  }

  @Test
  void logsTheRawAppendedContent(@TempDir Path dir) throws Exception {
    var buffer = new ChatBuffer("test");
    var log = dir.resolve("log.html").toFile();
    buffer.setLogFile(log);
    buffer.append("<b>unclosed");
    buffer.dispose();
    ChatBuffer.ACTIVE_LOG_FILES.remove(log.getPath());

    assertThat(Files.readString(log.toPath()), containsString("<b>unclosed\n"));
  }
}
