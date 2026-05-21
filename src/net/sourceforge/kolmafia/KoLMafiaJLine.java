package net.sourceforge.kolmafia;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.jline.reader.EndOfFileException;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

public class KoLMafiaJLine {
  private static PrintStream originalOut;
  private static PrintStream originalErr;

  private static LineReader jlineReader;
  private static Terminal jlineTerminal;
  private static Instant lastNoUserHistory;

  private static int interruptCount = 0;
  private static long lastInterruptTime = 0L;
  private static final long INTERRUPT_RESET_SECS = 5;
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

  private KoLMafiaJLine() {}

  public static boolean isJLineEnabled() {
    return jlineReader != null;
  }

  public static void initialize() {
    originalOut = System.out;
    originalErr = System.err;

    if (Preferences.getBoolean("disableJLine")) {
      return;
    }

    createJline();
  }

  private static void createJline() {
    try {
      jlineTerminal =
          TerminalBuilder.builder()
              .system(true)
              .jansi(true)
              .signalHandler(Terminal.SignalHandler.SIG_DFL)
              .build();

      updateAttachedAccount();

      // Intercept System.out and System.err, allows the program to continue using
      // System.out.println
      System.setOut(new PrintStream(new LineInterceptorStream(originalOut, false), true));
      System.setErr(new PrintStream(new LineInterceptorStream(originalErr, true), true));
    } catch (Throwable throwable) {
      StaticEntity.printStackTrace(throwable, "Error creating JLine");
    }
  }

  public static String readLine(String prompt) {
    return readLine(prompt, null);
  }

  public static String readLine(String prompt, Character mask) {
    if (jlineReader == null) {
      return null;
    }

    try {
      return jlineReader.readLine(prompt, mask);
    } catch (UserInterruptException e) {
      return handleInterrupt();
    } catch (EndOfFileException e) {
      return null;
    }
  }

  private static String handleInterrupt() {
    long now = System.currentTimeMillis();

    if (now - lastInterruptTime > INTERRUPT_RESET_SECS * 1000) {
      interruptCount = 0;
    }

    lastInterruptTime = now;

    switch (++interruptCount) {
      case 1 -> {
        System.err.println(
            "Killing running scripts... Ctrl + C again in the next "
                + INTERRUPT_RESET_SECS
                + " seconds to request shutdown.");
        return "abort";
      }

      case 2 -> {
        System.err.println(
            "Asking KoLMafia to shut down... Ctrl + C again to terminate the process.");
        KoLmafia.quit();
        return "abort";
      }

      default -> {
        System.err.println("Terminating the process.");
        System.exit(1);
        return null;
      }
    }
  }

  public static void addCommandToHistory(String command) {
    if (jlineReader == null || command == null || command.isBlank()) {
      return;
    }

    synchronized (jlineTerminal) {
      History history = jlineReader.getHistory();

      if (history == null || (!history.isEmpty() && history.get(history.last()).equals(command))) {
        return;
      }

      history.add(command);
    }
  }

  private static void createReader(DefaultHistory history, Path historyFile) {
    jlineReader =
        LineReaderBuilder.builder()
            .terminal(jlineTerminal)
            .history(history)
            .variable(LineReader.HISTORY_FILE, historyFile)
            .variable(LineReader.HISTORY_SIZE, 1000)
            .variable(LineReader.HISTORY_FILE_SIZE, 1000)
            .option(LineReader.Option.AUTO_FRESH_LINE, true)
            .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
            .option(LineReader.Option.HISTORY_IGNORE_DUPS, true)
            // Prevents jline from prefixing characters for mutliline commands (eg, via copy/paste)
            .variable(LineReader.SECONDARY_PROMPT_PATTERN, "")
            // If multi-line commands are undesirable, it needs to be handled.
            .build();

    var main = jlineReader.getKeyMaps().get(LineReader.MAIN);

    // Allows using Ctrl + Up/Down to go up and down history.
    // Useful for multi-line commands
    main.bind(new Reference(LineReader.UP_HISTORY), "\u001b[1;5A");
    main.bind(new Reference(LineReader.DOWN_HISTORY), "\u001b[1;5B");
  }

  public static void updateAttachedAccount() {
    if (jlineTerminal == null) {
      return;
    }

    synchronized (jlineTerminal) {
      History oldHistory = null;
      Instant copyHistoryFrom = lastNoUserHistory;

      if (jlineReader != null) {
        try {
          jlineReader.getHistory().save();
          oldHistory = jlineReader.getHistory();
        } catch (IOException e) {
          StaticEntity.printStackTrace(e, "Error saving jline history");
        }
      }

      String name = KoLCharacter.getUserName().trim().toLowerCase(Locale.ENGLISH);

      if (name.isEmpty()) {
        name = "no-user";

        if (lastNoUserHistory == null) {
          lastNoUserHistory = Instant.now();
        }
      } else {
        lastNoUserHistory = null;
      }

      DefaultHistory newHistory = new DefaultHistory();
      Path historyFile = Paths.get("history/" + name + "-terminal.txt");

      try {
        if (historyFile.getParent() != null) {
          historyFile.getParent().toFile().mkdirs();
        }
      } catch (Exception ignored) {
      }

      if (jlineReader == null) {
        createReader(newHistory, historyFile);
      } else {
        jlineReader.variable(LineReader.HISTORY_FILE, historyFile);
        ((LineReaderImpl) jlineReader).setHistory(newHistory);
        newHistory.attach(jlineReader);
      }

      copyHistory(oldHistory, newHistory, copyHistoryFrom);
    }
  }

  /**
   * Used to attribute commands run before a user was established, to a user
   *
   * @param oldHistory
   * @param newHistory
   * @param from Establishes this time onwards, to be attributed to the user
   */
  private static void copyHistory(History oldHistory, DefaultHistory newHistory, Instant from) {
    if (oldHistory == null || from == null) {
      return;
    }

    boolean added = false;

    for (History.Entry entry : oldHistory) {
      if (entry.time().isBefore(from)) {
        continue;
      }

      if (!newHistory.isEmpty() && newHistory.get(newHistory.last()).equals(entry.line())) {
        continue;
      }

      newHistory.add(entry.time(), entry.line());
      added = true;
    }

    if (added) {
      try {
        newHistory.save();
      } catch (IOException e) {
        StaticEntity.printStackTrace(e, "Error saving merged history");
      }
    }
  }

  /** Intercepts bytes and sends to JLine to be printed in a nicer manner */
  private static class LineInterceptorStream extends OutputStream {
    private final PrintStream original;
    private final boolean errorStream;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    LineInterceptorStream(PrintStream original, boolean errorStream) {
      this.original = original;
      this.errorStream = errorStream;
    }

    @Override
    public synchronized void write(int b) {
      if (b == '\n') {
        String line = buffer.toString(StandardCharsets.UTF_8);
        buffer.reset();

        line = line.replace("\r", "");

        print(original, line, errorStream);
      } else {
        buffer.write(b);
      }
    }

    @Override
    public synchronized void flush() {
      original.flush();
    }

    static synchronized void print(PrintStream stream, String line, boolean error) {
      // Gives the timestamp a gray color, unless it's an error
      AttributedString timestamp =
          new AttributedString(
              "[" + LocalTime.now().format(TIME_FORMAT) + "] ",
              AttributedStyle.DEFAULT.foreground(
                  error ? AttributedStyle.RED : AttributedStyle.BRIGHT));

      AttributedString as;

      if (error) {
        as = new AttributedString(line, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
      } else {
        as = new AttributedString(line);
      }

      jlineReader.printAbove(AttributedString.join(AttributedString.EMPTY, timestamp, as));
    }
  }
}
