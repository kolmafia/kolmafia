package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    var choices = request.choices();
    var initial = request.initialChoice();
    int initialIndex = 0;
    RequestLogger.printLine(request.message());

    for (int i = 0; i < choices.size(); ++i) {
      T o = choices.get(i);
      RequestLogger.printLine("  " + (i + 1) + ": " + o);

      if (initial != null && initial.equals(o)) {
        initialIndex = i;
      }
    }

    RequestLogger.printLine(
        "(Enter a number from 1 to "
            + choices.size()
            + ", leave blank to use "
            + (initialIndex + 1)
            + ")");

    String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");

    if (reply.equals("")) {
      return initial;
    }

    int selectedIndex = StringUtilities.parseInt(reply) - 1;

    if (selectedIndex > -1 && selectedIndex < choices.size()) {
      return choices.get(selectedIndex);
    }

    return null;
  }

  private static <T> List<T> multiple(final UserPrompt.Multiple<T> request) {
    var filter = request.filter();
    RequestLogger.printLine(request.message());
    List<T> visibleInputs = new ArrayList<>();

    for (T o : request.choices()) {
      if (filter == null || filter.isVisible(o)) {
        visibleInputs.add(o);
        RequestLogger.printLine("  " + (visibleInputs.size()) + ": " + o);
      }
    }

    RequestLogger.printLine(
        "(Enter a comma-delimited list of values from 1 to " + visibleInputs.size() + ")");

    String reply = KoLmafiaCLI.DEFAULT_SHELL.getNextLine(" > ");

    String[] replyList = StringUtilities.splitByComma(reply);

    Set<T> selectedValues = new HashSet<>();

    for (int i = 0; i < replyList.length; ++i) {
      int selectedIndex = StringUtilities.parseInt(replyList[i]) - 1;

      if (selectedIndex > -1 && selectedIndex < visibleInputs.size()) {
        selectedValues.add(visibleInputs.get(selectedIndex));
      }
    }

    return new ArrayList<>(selectedValues);
  }
}
