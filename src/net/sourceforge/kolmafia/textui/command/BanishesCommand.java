package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.BanishManager;

public class BanishesCommand extends AbstractCommand {
  public BanishesCommand() {
    this.usage = " - List status of banishes.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    String[][] banishedMonsterData = BanishManager.getBanishedMonsterData();
    boolean hasMonsterData = banishedMonsterData.length > 0;
    String[][] banishedPhylaData = BanishManager.getBanishedPhylaData();
    boolean hasPhylaData = banishedPhylaData.length > 0;

    StringBuilder output = new StringBuilder();

    if (hasMonsterData || hasPhylaData) {
      output.append("<table border=2 cols=4>");
      if (hasMonsterData) {
        output
            .append("<tr>")
            .append("<th>Monsters Banished</th>")
            .append("<th>Banished By</th>")
            .append("<th>On Turn</th>")
            .append("<th>Turns Left</th>")
            .append("</tr>");

        for (String[] banish : banishedMonsterData) {
          output.append("<tr>");

          for (int i = 0; i < 4; i++) {
            output.append("<td>").append(banish[i]).append("</td>");
          }
          output.append("</tr>");
        }
      }
      if (hasPhylaData) {
        output
            .append("<tr>")
            .append("<th>Phyla Banished</th>")
            .append("<th>Banished By</th>")
            .append("<th>On Turn</th>")
            .append("<th>Turns Left</th>")
            .append("</tr>");

        for (String[] banish : banishedPhylaData) {
          output.append("<tr>");

          for (int i = 0; i < 4; i++) {
            output.append("<td>").append(banish[i]).append("</td>");
          }
          output.append("</tr>");
        }
      }

      output.append("</table>");
    } else {
      output.append("No current banishes");
    }

    RequestLogger.printLine(output.toString());
    RequestLogger.printLine();
  }
}
