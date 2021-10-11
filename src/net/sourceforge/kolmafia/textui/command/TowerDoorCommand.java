package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.TowerDoorManager;
import net.sourceforge.kolmafia.session.TowerDoorManager.Lock;

public class TowerDoorCommand extends AbstractCommand {
  public TowerDoorCommand() {
    this.usage = " [needed] - List status of the Tower Door in the Sorceress's Lair.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    boolean needed = parameters.trim().equals("needed");
    StringBuilder output = new StringBuilder();

    output.append("<table border=2 cols=4>");
    output.append("<tr>");
    output.append("<th>Lock</th>");
    output.append("<th>Key</th>");
    output.append("<th>Have/Used</th>");
    output.append("<th>Location</th>");
    output.append("<tr>");
    output.append("<th colspan=4>Enchantments</th>");
    output.append("</tr>");

    for (Lock lock : TowerDoorManager.getLocks()) {
      if (lock.isDoorknob()) {
        continue;
      }

      boolean have = lock.haveKey();
      boolean used = lock.usedKey();
      if (needed && (have || used)) {
        continue;
      }

      AdventureResult key = lock.getKey();

      output.append("<tr>");

      output.append("<td>");
      output.append(lock.getName());
      output.append("</td>");

      output.append("<td>");
      output.append(key.getName());
      output.append("</td>");

      output.append("<td>");
      output.append(have ? "yes" : "no");
      output.append("/");
      output.append(used ? "yes" : "no");
      output.append("</td>");

      output.append("<td>");
      output.append(lock.getLocation());
      output.append("</td>");

      output.append("</tr>");
      output.append("<tr>");

      output.append("<td colspan=4 style=\"word-wrap:break-word\">");
      output.append(lock.keyEnchantments());
      output.append("</td>");

      output.append("</tr>");
    }

    output.append("</table>");

    RequestLogger.printLine(output.toString());
    RequestLogger.printLine();
  }
}
