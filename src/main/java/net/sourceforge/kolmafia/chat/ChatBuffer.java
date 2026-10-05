/*
 * Copyright (c) 2003, Spellcast development team. Modified by the KoLmafia development team.
 * SPDX-License-Identifier: BSD-3-Clause
 * See src/main/resources/licenses/spellcast-license.txt for the full license text.
 */

package net.sourceforge.kolmafia.chat;

import java.awt.event.HierarchyEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.listener.StickyListener;

/**
 * A multi-purpose message buffer which stores all sorts of the messages that can either be
 * displayed or serialized in HTML form. In essence, this shifts the functionality of processing a
 * chat message from the <code>ChatPanel</code> to an object external to it, which allows more
 * object-oriented design and less complications.
 */
public class ChatBuffer {
  private static final Pattern TAG_PATTERN = Pattern.compile("<\\s*([^\\s>]+)(.*?)>");
  private static final Pattern COMMENT_PATTERN = Pattern.compile("<!--(.*?)-->");

  private final String title;

  private final Deque<String> entries = new ArrayDeque<>();
  private int contentLength = 0;

  private final Set<JEditorPane> displayPanes = ChatBuffer.weakSet();
  private final Set<JEditorPane> stalePanes = ChatBuffer.weakSet();

  private int pendingCount = 0;
  private int pendingRemovals = 0;
  private boolean flushScheduled = false;

  private File logFile;
  private PrintWriter logWriter;

  protected static final HashMap<String, PrintWriter> ACTIVE_LOG_FILES = new HashMap<>();

  private static final int MINIMUM_LENGTH = 10000;
  private static final int MINIMUM_ENTRIES = 100;
  private static final int MAXIMUM_RETAINED_LENGTH = 200000;

  /**
   * Constructs a new <code>ChatBuffer</code>. However, note that this does not automatically
   * translate into the messages being displayed; until a chat display is set, this buffer merely
   * stores the message content to be displayed.
   */
  public ChatBuffer(final String title) {
    this.title = title;
  }

  /** Adds a chat display used to display the chat messages currently being stored in the buffer. */
  public JScrollPane addDisplay(final JEditorPane displayPane) {
    if (displayPane == null) {
      return null;
    }

    displayPane.setContentType("text/html");
    displayPane.setEditable(false);
    displayPane.addHierarchyListener(
        e -> {
          if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0
              && displayPane.isDisplayable()) {
            SwingUtilities.invokeLater(() -> this.markStale(displayPane));
          }
        });

    JScrollPane scroller =
        new JScrollPane(
            displayPane,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    var stickyListener = new StickyListener(scroller.getVerticalScrollBar());
    displayPane.putClientProperty(StickyListener.class, stickyListener);
    displayPane.setCaret(new ChatCaret(stickyListener));

    SwingUtilities.invokeLater(
        () -> {
          this.displayPanes.add(displayPane);
          this.markStale(displayPane);
        });

    return scroller;
  }

  /** Sets the log file used to actively record messages that are being stored in the buffer. */
  public void setLogFile(final File f) {
    if (f == null || this.title == null) {
      return;
    }

    String filename = f.getPath();

    if (filename == null || this.title == null) {
      return;
    }

    if (ChatBuffer.ACTIVE_LOG_FILES.containsKey(filename)) {
      this.logFile = f;
      this.logWriter = ChatBuffer.ACTIVE_LOG_FILES.get(filename);
    } else {
      boolean shouldAppend = f.exists();
      this.logFile = f;
      this.logWriter =
          new PrintWriter(
              DataUtilities.getOutputStream(f, shouldAppend), true, StandardCharsets.UTF_8);

      ChatBuffer.ACTIVE_LOG_FILES.put(filename, this.logWriter);

      if (!shouldAppend) {
        this.logWriter.println("<html><head>");
        this.logWriter.println("<title>");
        this.logWriter.println(this.title);
        this.logWriter.println("</title>");
        this.logWriter.println("<style>");
        this.logWriter.println(this.getStyle());
        this.logWriter.println("</style>");
        this.logWriter.println("<body>");
      }
    }
  }

  /** Closes the buffer. */
  public void dispose() {
    SwingUtilities.invokeLater(
        () -> {
          this.displayPanes.clear();
          this.stalePanes.clear();
        });

    if (this.logWriter != null) {
      this.logWriter.close();
    }

    this.clear();
  }

  private static void printHTML(final HTMLDocument doc) {
    HTMLEditorKit kit = new HTMLEditorKit();
    StringWriter writer = new StringWriter();
    try {
      kit.write(writer, doc, 0, doc.getLength());
    } catch (Exception e) {
    }
    String s = writer.toString();
    System.out.println("HTML = \"" + s + "\"");
  }

  /** Clears the current buffer content. */
  public void clear() {
    SwingUtilities.invokeLater(
        () -> {
          this.entries.clear();
          this.contentLength = 0;

          this.requestReset();
        });
  }

  public File getLogFile() {
    return this.logFile;
  }

  /** Appends the given contents to the chat buffer. */
  public void append(String newContents) {
    if (newContents == null) {
      SwingUtilities.invokeLater(this::requestReset);
      return;
    }

    String entry = newContents.trim();

    if (entry.length() == 0) {
      return;
    }

    if (this.logWriter != null) {
      this.logWriter.println(entry);
    }

    SwingUtilities.invokeLater(() -> this.addEntry(entry));
  }

  private void addEntry(final String entry) {
    this.entries.addLast(entry);
    this.contentLength += entry.length();

    this.pendingCount++;

    var maximumLength = ChatBuffer.maximumLength();

    if (this.contentLength >= maximumLength) {
      this.trim(maximumLength);
    }

    this.scheduleFlush();
  }

  /** Returns the styling used by this buffer. */
  public String getStyle() {
    return "body { font-family: sans-serif; }";
  }

  /** Returns all the content stored within this chat buffer. */
  public String getContent() {
    return String.join("", this.entries);
  }

  /** Returns all the styled content stored within this chat buffer. */
  public String getHTMLContent() {
    StringBuffer htmlContent = new StringBuffer();

    htmlContent.append("<html><head><style>");
    htmlContent.append(this.getStyle());
    htmlContent.append("</style></head><body>");

    htmlContent.append(ChatBuffer.wrapEntries(this.entries));

    htmlContent.append("</body></html>");

    return htmlContent.toString();
  }

  private static String wrapEntries(final Iterable<String> entries) {
    StringBuilder html = new StringBuilder();

    for (String entry : entries) {
      html.append("<div>").append(ChatBuffer.balanceTags(entry)).append("</div>");
    }

    return html.toString();
  }

  private static Set<JEditorPane> weakSet() {
    return Collections.newSetFromMap(new WeakHashMap<>());
  }

  private void markStale(final JEditorPane displayPane) {
    this.stalePanes.add(displayPane);
    this.scheduleFlush();
  }

  private void requestReset() {
    this.pendingCount = 0;
    this.pendingRemovals = 0;
    this.stalePanes.addAll(this.displayPanes);
    this.scheduleFlush();
  }

  private static int maximumLength() {
    return Math.max(Preferences.getInteger("outputBufferLength"), ChatBuffer.MINIMUM_LENGTH);
  }

  private void trim(final int maximumLength) {
    var trimToLength = maximumLength / 10 * 9;
    var maximumRetainedLength = Math.max(maximumLength, ChatBuffer.MAXIMUM_RETAINED_LENGTH);

    while (this.entries.size() > 1
        && this.contentLength > trimToLength
        && (this.entries.size() > ChatBuffer.MINIMUM_ENTRIES
            || this.contentLength > maximumRetainedLength)) {
      if (this.pendingCount == this.entries.size()) {
        this.pendingCount--;
      } else {
        this.pendingRemovals++;
      }

      this.contentLength -= this.entries.removeFirst().length();
    }
  }

  private void scheduleFlush() {
    if (this.flushScheduled) {
      return;
    }

    this.flushScheduled = true;
    SwingUtilities.invokeLater(this::flush);
  }

  private void flush() {
    int removals = this.pendingRemovals;
    String added =
        ChatBuffer.wrapEntries(
            this.entries.stream().skip(this.entries.size() - this.pendingCount).toList());
    String htmlContent = null;

    this.pendingRemovals = 0;
    this.pendingCount = 0;
    this.flushScheduled = false;

    for (JEditorPane displayPane : this.displayPanes) {
      if (!displayPane.isDisplayable()) {
        continue;
      }

      if (this.stalePanes.remove(displayPane)
          || !ChatBuffer.update(
              displayPane,
              (StickyListener) displayPane.getClientProperty(StickyListener.class),
              removals,
              added)) {
        if (htmlContent == null) {
          htmlContent = this.getHTMLContent();
        }

        displayPane.setText(htmlContent);
      }

      // Non-ASCII text sets "multiByte", which switches to a much slower bidi-aware layout.
      displayPane.getDocument().putProperty("multiByte", Boolean.FALSE);
    }
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
            ? ChatBuffer.top(displayPane, entryElements.get(removals))
                - ChatBuffer.top(displayPane, entryElements.get(0))
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

    // ChatBuffer.printHTML( currentHTML );

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

  static String balanceTags(final String newContent) {
    // Check for imbalanced HTML here

    Stack<String> openTags = new Stack<>();
    Set<String> skippedTags = new HashSet<>();
    StringBuffer buffer = new StringBuffer();

    String noCommentsContent = COMMENT_PATTERN.matcher(newContent).replaceAll("");

    Matcher tagMatcher = TAG_PATTERN.matcher(noCommentsContent);

    while (tagMatcher.find()) {
      String tagName = tagMatcher.group(1);
      StringBuffer replacement = new StringBuffer();

      if (tagName.startsWith("/")) {
        String closeTag = tagName.substring(1);

        if (skippedTags.contains(closeTag)) {
          skippedTags.remove(closeTag);
        } else {
          while (!openTags.isEmpty()) {
            String openTag = openTags.pop();
            replacement.append("</");
            replacement.append(openTag);
            replacement.append(">");

            if (openTag.equalsIgnoreCase(closeTag)) {
              break;
            } else if (skippedTags.contains(closeTag)) {
              skippedTags.remove(closeTag);
              break;
            } else {
              skippedTags.add(closeTag);
            }
          }
        }
      } else {
        if (!tagName.equalsIgnoreCase("br")) {
          openTags.push(tagName);
        }

        replacement.append("<$1$2>");
      }

      tagMatcher.appendReplacement(buffer, replacement.toString());
    }

    tagMatcher.appendTail(buffer);

    while (!openTags.isEmpty()) {
      String openTag = openTags.pop();
      buffer.append("</");
      buffer.append(openTag);
      buffer.append(">");
    }

    return buffer.toString();
  }
}
