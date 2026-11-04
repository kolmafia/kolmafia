package net.sourceforge.kolmafia.utilities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import internal.helpers.Cleanups;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.IntStream;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.LockableListModel.ListElementFilter;
import net.sourceforge.kolmafia.ui.UserInterface;
import net.sourceforge.kolmafia.ui.UserInterfaceManager;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InputFieldUtilitiesTest {
  /** Records what the engine asked and answers with a fixed reply. */
  private static class RecordingUserInterface implements UserInterface {
    final List<String> asked = new ArrayList<>();
    final String reply;

    RecordingUserInterface(final String reply) {
      this.reply = reply;
    }

    @Override
    public void alert(final String message) {
      this.asked.add("alert: " + message);
    }

    @Override
    public boolean confirm(final String message) {
      this.asked.add("confirm: " + message);
      return "y".equals(this.reply);
    }

    @Override
    public String input(final String message) {
      this.asked.add("input: " + message);
      return this.reply;
    }

    @Override
    public String input(final String message, final String initial) {
      this.asked.add("input: " + message + " [" + initial + "]");
      return this.reply;
    }

    @Override
    public <T> T input(
        final String message,
        final LockableListModel<T> inputs,
        final BiFunction<Object, Boolean, String> toHTMLFunction,
        final T initial) {
      this.asked.add("list: " + message + " (" + inputs.size() + ")");
      return initial;
    }

    @Override
    public <T> T input(final String message, final T[] inputs, final T initial) {
      this.asked.add("choice: " + message + " (" + inputs.length + ")");
      return initial;
    }

    @Override
    public <T> List<T> multiple(
        final String message, final LockableListModel<T> inputs, final ListElementFilter filter) {
      this.asked.add("multiple: " + message);
      return List.of();
    }
  }

  private static Cleanups withUserInterface(final UserInterface userInterface) {
    UserInterfaceManager.setUserInterface(userInterface);
    return new Cleanups(() -> UserInterfaceManager.setUserInterface(null));
  }

  @Nested
  class Prompts {
    @Test
    void asksTheInstalledUserInterface() {
      var ui = new RecordingUserInterface("y");

      try (var cleanups = withUserInterface(ui)) {
        InputFieldUtilities.alert("Hello");
        assertThat(InputFieldUtilities.confirm("Sure?"), is(true));
        assertThat(InputFieldUtilities.input("Name?"), is("y"));
        assertThat(InputFieldUtilities.input("Count?", "3"), is("y"));
        assertThat(InputFieldUtilities.multiple("Which?", new LockableListModel<>()), empty());

        assertThat(
            ui.asked,
            contains(
                "alert: Hello",
                "confirm: Sure?",
                "input: Name?",
                "input: Count? [3]",
                "multiple: Which?"));
      }
    }

    @Test
    void choosesFromShortArraysWithASimpleChoice() {
      var ui = new RecordingUserInterface("");

      try (var cleanups = withUserInterface(ui)) {
        String[] stats = {"Muscle", "Mysticality", "Moxie"};

        assertThat(InputFieldUtilities.input("Stat?", stats, "Moxie"), is("Moxie"));
        assertThat(ui.asked, contains("choice: Stat? (3)"));
      }
    }

    @Test
    void choosesFromLongArraysWithAList() {
      var ui = new RecordingUserInterface("");

      try (var cleanups = withUserInterface(ui)) {
        Integer[] numbers = IntStream.rangeClosed(1, 13).boxed().toArray(Integer[]::new);

        assertThat(InputFieldUtilities.input("Number?", numbers, 7), is(7));
        assertThat(ui.asked, contains("list: Number? (13)"));
      }
    }

    @Test
    void doesNotAskWithoutChoices() {
      var ui = new RecordingUserInterface("");

      try (var cleanups = withUserInterface(ui)) {
        assertThat(InputFieldUtilities.input("Stat?", new String[0]), nullValue());
        assertThat(InputFieldUtilities.input("Stat?", (String[]) null), nullValue());
        assertThat(ui.asked, empty());
      }
    }
  }

  @Nested
  class GetQuantity {
    @ParameterizedTest
    @CsvSource({"5, 5", "20, 10", "*, 10", "-3, 13", "'1,000', 10", "0, 0"})
    void interpretsTheReply(final String reply, final int quantity) {
      try (var cleanups = withUserInterface(new RecordingUserInterface(reply))) {
        assertThat(InputFieldUtilities.getQuantity("How many?", 10), is(quantity));
      }
    }

    @Test
    void returnsNullWhenCancelled() {
      try (var cleanups = withUserInterface(new RecordingUserInterface(null))) {
        assertThat(InputFieldUtilities.getQuantity("How many?", 10), nullValue());
      }
    }

    @Test
    void doesNotAskWhenOnlyOneIsPossible() {
      var ui = new RecordingUserInterface("5");

      try (var cleanups = withUserInterface(ui)) {
        assertThat(InputFieldUtilities.getQuantity("How many?", 1), is(1));
        assertThat(ui.asked, empty());
      }
    }

    @Test
    void offersTheDefaultCappedAtTheMaximum() {
      var ui = new RecordingUserInterface("2");

      try (var cleanups = withUserInterface(ui)) {
        assertThat(InputFieldUtilities.getQuantity("How many?", 4, 9), is(2));
        assertThat(ui.asked, contains("input: How many? [4]"));
      }
    }
  }
}
