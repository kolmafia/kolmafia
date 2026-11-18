package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.List;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.utilities.StringUtilities;

class UserPromptConsole {
  private UserPromptConsole() {}

  @SuppressWarnings("unchecked")
  static <R> R ask(final UserPrompt.Request<R> request) {
    return (R)
        switch (request) {
          case UserPrompt.Alert alert -> alert(alert);
          case UserPrompt.Confirm confirm -> confirm(confirm);
          case UserPrompt.Text text -> text(text);
          case UserPrompt.Select<?> select -> select(select);
          case UserPrompt.Multiple<?> multiple -> multiple(multiple);
        };
  }

  private static Void alert(final UserPrompt.Alert request) {
    RequestLogger.printLine(request.message());
    KoLmafiaCLI.DEFAULT_SHELL.getNextLine("Press enter to continue...");
    return null;
  }

  private static Boolean confirm(final UserPrompt.Confirm request) {
    RequestLogger.printLine(request.message());
    RequestLogger.printLine("(Y/N, leave blank to choose N)");

    return KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ").equalsIgnoreCase("y");
  }

  private static String text(final UserPrompt.Text request) {
    var initial = request.initial();
    RequestLogger.printLine(request.message());

    if (initial == null) {
      return KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");
    }

    RequestLogger.printLine("(leave blank to use " + initial + ")");

    var reply =
        KoLmafiaCLI.DEFAULT_SHELL.getNextLine(
            request.message() + "\n(Leave blank to use " + initial + ")\n > ");

    return reply.equals("") ? initial : reply;
  }

  private static <T> T select(final UserPrompt.Select<T> request) {
    var options = request.options();
    var initial = request.initialChoice();
    RequestLogger.printLine(request.message());
    printOptions(options);

    RequestLogger.printLine(
        "(Enter a number from 1 to "
            + options.size()
            + ", leave blank to use "
            + (Math.max(0, request.choices().indexOf(initial)) + 1)
            + ")");

    var reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");

    return reply.equals("") ? initial : request.decode(toZeroBased(reply));
  }

  private static <T> List<T> multiple(final UserPrompt.Multiple<T> request) {
    var options = request.options();
    RequestLogger.printLine(request.message());
    printOptions(options);

    RequestLogger.printLine(
        "(Enter a comma-delimited list of values from 1 to " + options.size() + ")");

    var reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");
    var indices =
        Arrays.stream(StringUtilities.splitByComma(reply))
            .map(UserPromptConsole::toZeroBased)
            .toList();

    return request.decode(String.join(",", indices));
  }

  private static void printOptions(final List<String> options) {
    for (int i = 0; i < options.size(); ++i) {
      RequestLogger.printLine("  " + (i + 1) + ": " + options.get(i));
    }
  }

  private static String toZeroBased(final String index) {
    return String.valueOf(StringUtilities.parseInt(index) - 1);
  }
}
