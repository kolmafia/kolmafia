package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.BanishManager;

public class BanishesCommand extends AbstractCommand {
  public BanishesCommand() {
    this.usage = " - List status of banishes.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    String[][] banishData = BanishManager.getBanishData();

    StringBuilder output = new StringBuilder();

    if (banishData != null) {
      output.append("<table border=2 cols=4>");
      output.append("<tr>");
      output.append("<th>Monsters Banished</th>");
      output.append("<th>Banished By</th>");
      output.append("<th>On Turn</th>");
      output.append("<th>Turns Left</th>");
      output.append("</tr>");

      for (String[] banish : banishData) {
        output.append("<tr>");

        for (int i = 0; i < 4; i++) {
          output.append("<td>");
          output.append(banish[i]);
          output.append("</td>");
        }
        output.append("</tr>");
      }

      output.append("</table>");
    } else {
      output.append("No current banishes");
    }

    RequestLogger.printLine(output.toString());
    RequestLogger.printLine();
  }
}
