/*
 * Copyright (c) 2003, Spellcast development team. Modified by the KoLmafia development team.
 * SPDX-License-Identifier: BSD-3-Clause
 * See src/main/resources/licenses/spellcast-license.txt for the full license text.
 */

package net.sourceforge.kolmafia.chat;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.ui.UserInterfaceManager;

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

  private final Set<Display> displayPanes = ChatBuffer.weakSet();
  private final Set<Display> stalePanes = ChatBuffer.weakSet();
  private final Set<Display> statusLines = ChatBuffer.weakSet();

  private int pendingCount = 0;
  private int pendingRemovals = 0;
  private boolean flushScheduled = false;

  private File logFile;
  private PrintWriter logWriter;

  protected static final HashMap<String, PrintWriter> ACTIVE_LOG_FILES = new HashMap<>();

  private static final int MINIMUM_LENGTH = 10000;
  private static final int MINIMUM_ENTRIES = 100;
  private static final int MAXIMUM_RETAINED_LENGTH = 200000;
  private static final int STATUS_LINE_ENTRIES = 10;

  /**
   * Constructs a new <code>ChatBuffer</code>. However, note that this does not automatically
   * translate into the messages being displayed; until a chat display is set, this buffer merely
   * stores the message content to be displayed.
   */
  public ChatBuffer(final String title) {
    this.title = title;
  }

  /**
   * Where a chat buffer is shown. The buffer calls these on the user interface's thread when it
   * flushes new content.
   */
  public interface Display {
    boolean isDisplayable();

    /** Replaces everything shown with the given HTML. */
    void setText(String html);

    /**
     * Removes the oldest entries and appends the newly added ones, keeping the reader's place.
     * Returns false if the display has to be redrawn with setText instead.
     */
    boolean update(int removals, String added);

    void runPostFlushActions();
  }

  /** Adds a display used to display the chat messages currently being stored in the buffer. */
  public void addDisplay(final Display displayPane, final boolean statusLine) {
    UserInterfaceManager.get()
        .invokeLater(
            () -> {
              this.displayPanes.add(displayPane);

              if (statusLine) {
                this.statusLines.add(displayPane);
              }

              this.markStale(displayPane);
            });
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
    UserInterfaceManager.get()
        .invokeLater(
            () -> {
              this.displayPanes.clear();
              this.stalePanes.clear();
            });

    if (this.logWriter != null) {
      this.logWriter.close();
    }

    this.clear();
  }

  /** Clears the current buffer content. */
  public void clear() {
    UserInterfaceManager.get()
        .invokeLater(
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
      UserInterfaceManager.get().invokeLater(this::requestReset);
      return;
    }

    String entry = newContents.trim();

    if (entry.isEmpty()) {
      return;
    }

    if (this.logWriter != null) {
      this.logWriter.println(entry);
    }

    UserInterfaceManager.get().invokeLater(() -> this.addEntry(entry));
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
    return this.getHTMLContent(this.entries.size());
  }

  private String getHTMLContent(final int latestEntries) {

    String htmlContent =
        "<html><head><style>"
            + this.getStyle()
            + "</style></head><body>"
            + ChatBuffer.wrapEntries(
                this.entries.stream()
                    .skip(Math.max(0, this.entries.size() - latestEntries))
                    .toList())
            + "</body></html>";

    return htmlContent;
  }

  private static String wrapEntries(final Iterable<String> entries) {
    StringBuilder html = new StringBuilder();

    for (String entry : entries) {
      html.append("<div>").append(ChatBuffer.balanceTags(entry)).append("</div>");
    }

    return html.toString();
  }

  private static Set<Display> weakSet() {
    return Collections.newSetFromMap(new WeakHashMap<>());
  }

  /** Redraws the whole buffer in the given display at the next flush. */
  public void markStale(final Display displayPane) {
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
    UserInterfaceManager.get().invokeLater(this::flush);
  }

  private void flush() {
    int removals = this.pendingRemovals;
    String added =
        ChatBuffer.wrapEntries(
            this.entries.stream().skip(this.entries.size() - this.pendingCount).toList());
    String htmlContent = null;
    String statusLineContent = null;

    this.pendingRemovals = 0;
    this.pendingCount = 0;
    this.flushScheduled = false;

    for (Display displayPane : this.displayPanes) {
      if (!displayPane.isDisplayable()) {
        continue;
      }

      var stale = this.stalePanes.remove(displayPane);

      if (this.statusLines.contains(displayPane)) {
        if (statusLineContent == null) {
          statusLineContent = this.getHTMLContent(ChatBuffer.STATUS_LINE_ENTRIES);
        }

        displayPane.setText(statusLineContent);
      } else if (stale || !displayPane.update(removals, added)) {
        if (htmlContent == null) {
          htmlContent = this.getHTMLContent();
        }

        displayPane.setText(htmlContent);
      }

      displayPane.runPostFlushActions();
    }
  }

  static String balanceTags(final String newContent) {
    // Check for imbalanced HTML here

    Stack<String> openTags = new Stack<>();
    Set<String> skippedTags = new HashSet<>();
    StringBuilder buffer = new StringBuilder();

    String noCommentsContent = COMMENT_PATTERN.matcher(newContent).replaceAll("");

    Matcher tagMatcher = TAG_PATTERN.matcher(noCommentsContent);

    while (tagMatcher.find()) {
      String tagName = tagMatcher.group(1);
      StringBuilder replacement = new StringBuilder();

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
