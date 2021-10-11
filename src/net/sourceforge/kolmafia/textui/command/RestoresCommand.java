package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.RestoresDatabase;

public class RestoresCommand extends AbstractCommand {
  public RestoresCommand() {
    this.usage = " - List details of restores.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    String[] split = parameters.split(" ");
    String command = split[0];

    if (command.equals("")) {
      command = "available";
    }
    if (!command.equals("all") && !command.equals("available") && !command.equals("obtainable")) {
      KoLmafia.updateDisplay("Valid parameters are all, available or obtainable");
      return;
    }

    String[][] restoreData = RestoresDatabase.getRestoreData(command);

    StringBuilder output = new StringBuilder();

    if (restoreData != null) {
      output.append("<table border=2 cols=7>");
      output.append("<tr>");
      output.append("<th>Restore</th>");
      output.append("<th>Type</th>");
      output.append("<th>HP</th>");
      output.append("<th>MP</th>");
      output.append("<th>Adv cost</th>");
      output.append("<th>Uses left</th>");
      output.append("<th>Notes</th>");
      output.append("</tr>");

      for (String[] restore : restoreData) {
        if (restore[0] != null) {
          output.append("<tr>");

          for (int i = 0; i < 7; i++) {
            output.append("<td>");
            output.append(restore[i]);
            output.append("</td>");
          }
          output.append("</tr>");
        }
      }

      output.append("</table>");
    } else {
      output.append("No restore details");
    }

    RequestLogger.printLine(output.toString());
    RequestLogger.printLine();
  }
}
