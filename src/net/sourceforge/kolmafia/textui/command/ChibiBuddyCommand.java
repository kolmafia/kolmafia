package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChibiBuddyManager;

public class ChibiBuddyCommand extends AbstractCommand {
  public ChibiBuddyCommand() {
    this.usage = "| chat";
  }

  public static final String DOT_SYMBOL = "&#9632;";

  @Override
  public void run(final String cmd, String parameters) {
    if (!ChibiBuddyManager.haveChibiBuddy()) {
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, "You don't own a ChibiBuddy&trade;");
      return;
    }

    if (parameters.equals("")) {
      this.printStatus();
      return;
    }

    String[] split = parameters.split(" +");
    String command = split[0];

    if (command.equals("chat")) {
      if (Preferences.getBoolean("_chibiChanged")) {
        KoLmafia.updateDisplay(
            KoLConstants.MafiaState.ERROR,
            "You've already chatted with your ChibiBuddy&trade; today");
        return;
      }

      ChibiBuddyManager.ensureLiveChibi();
      ChibiBuddyManager.chat();
      return;
    }

    KoLmafia.updateDisplay(MafiaState.ERROR, "What does '" + parameters + "' mean?");
  }

  private String renderStatTable() {
    return "<table>"
        + this.renderStatRow("Fitness")
        + this.renderStatRow("Intelligence")
        + this.renderStatRow("Socialization")
        + this.renderStatRow("Alignment")
        + "</table>";
  }

  private String renderStatRow(final String stat) {
    var value = Preferences.getInteger("chibi" + stat);
    return "<tr><td>"
        + stat
        + "</td><td>"
        + DOT_SYMBOL.repeat(value)
        + "</td><td>["
        + value
        + "/10]</td></tr>";
  }

  private void printStatus() {
    if (!ChibiBuddyManager.haveChibiBuddyOn()) {
      RequestLogger.printLine("Your ChibiBuddy&trade; is currently powered off");
      return;
    }

    var age = ChibiBuddyManager.getAge();
    var daysSinceLastVisit = ChibiBuddyManager.getDaysSinceLastVisit();
    var lastVisitDescription =
        switch (daysSinceLastVisit) {
          case 0 -> "earlier today";
          case 1 -> "yesterday";
          default -> daysSinceLastVisit + " days ago";
        };

    RequestLogger.printHtml(
        "Your ChibiBuddy&trade; <b>"
            + Preferences.getString("chibiName")
            + "</b> is "
            + age
            + " day"
            + (age == 1 ? "" : "s")
            + " old and you last visited them "
            + lastVisitDescription
            + "."
            + KoLConstants.LINE_BREAK
            + this.renderStatTable());
  }
}
