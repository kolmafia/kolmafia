package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.Test;

public class CommandHistoryTest {
  private static final String FULL =
      IntStream.range(0, 20).mapToObj(i -> "echo " + i).collect(Collectors.joining("#"));

  @Test
  public void getEmptyHistory() {
    try (var cleanups = withProperty("commandBufferGCLI", "")) {
      assertThat(CommandHistory.get(), is(empty()));
    }
  }

  @Test
  public void getHistory() {
    try (var cleanups = withProperty("commandBufferGCLI", "echo a#echo b")) {
      assertThat(CommandHistory.get(), contains("echo a", "echo b"));
    }
  }

  @Test
  public void addCommand() {
    try (var cleanups = withProperty("commandBufferGCLI", "echo a")) {
      CommandHistory.add("echo b");
      assertThat(Preferences.getString("commandBufferGCLI"), is("echo b#echo a"));
    }
  }

  @Test
  public void removesDuplicates() {
    try (var cleanups = withProperty("commandBufferGCLI", "echo b#echo a#echo c#echo a")) {
      CommandHistory.add("echo a");
      assertThat(CommandHistory.get(), contains("echo a", "echo b", "echo c"));
    }
  }

  @Test
  public void duplicateKeepsOldest() {
    try (var cleanups = withProperty("commandBufferGCLI", FULL)) {
      CommandHistory.add("echo 5");

      var history = CommandHistory.get();
      assertThat(history, hasSize(20));
      assertThat(history.get(0), is("echo 5"));
      assertThat(history.get(19), is("echo 19"));
    }
  }

  @Test
  public void trimsCommand() {
    try (var cleanups = withProperty("commandBufferGCLI", "echo b#echo a")) {
      CommandHistory.add("  echo a ");
      assertThat(CommandHistory.get(), contains("echo a", "echo b"));
    }
  }

  @Test
  public void skipsBlank() {
    try (var cleanups = withProperty("commandBufferGCLI", "echo a")) {
      CommandHistory.add("   ");
      assertThat(Preferences.getString("commandBufferGCLI"), is("echo a"));
    }
  }

  @Test
  public void dropsOldest() {
    try (var cleanups = withProperty("commandBufferGCLI", FULL)) {
      CommandHistory.add("echo new");

      var history = CommandHistory.get();
      assertThat(history, hasSize(20));
      assertThat(history.get(0), is("echo new"));
      assertThat(history.get(19), is("echo 18"));
    }
  }

  @Test
  public void usesLimit() {
    var cleanups =
        new Cleanups(
            withProperty("commandBufferLimit", 3),
            withProperty("commandBufferGCLI", "echo d#echo c#echo b#echo a"));
    try (cleanups) {
      CommandHistory.add("echo e");

      assertThat(CommandHistory.get(), contains("echo e", "echo d", "echo c"));
    }
  }

  @Test
  public void keepsOneAtZeroLimit() {
    var cleanups =
        new Cleanups(
            withProperty("commandBufferLimit", 0), withProperty("commandBufferGCLI", "echo a"));
    try (cleanups) {
      CommandHistory.add("echo b");

      assertThat(CommandHistory.get(), contains("echo b"));
    }
  }

  @Test
  public void encodesDelimiter() {
    try (var cleanups = withProperty("commandBufferGCLI", "echo a")) {
      CommandHistory.add("echo 50% #1");

      assertThat(Preferences.getString("commandBufferGCLI"), is("echo 50%25 %231#echo a"));
      assertThat(CommandHistory.get(), contains("echo 50% #1", "echo a"));
    }
  }
}
