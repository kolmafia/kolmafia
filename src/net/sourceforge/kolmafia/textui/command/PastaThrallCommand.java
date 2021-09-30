package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.PastaThrallData;
import net.sourceforge.kolmafia.RequestLogger;

public class PastaThrallCommand extends AbstractCommand {
  public PastaThrallCommand() {
    this.usage = " - List status of pasta thralls.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    StringBuilder output = new StringBuilder();

    output.append("<table border=2 cols=3>");
    output.append("<tr>");
    output.append("<th rowspan=3>Pasta Thrall</th>");
    output.append("<th>Name</th>");
    output.append("<th>Ability at Level 1</th>");
    output.append("</tr>");
    output.append("<tr>");
    output.append("<th>Level</th>");
    output.append("<th>Ability at Level 5</th>");
    output.append("</tr>");
    output.append("<tr>");
    output.append("<th>Current Modifiers</th>");
    output.append("<th>Ability at Level 10</th>");
    output.append("</tr>");

    for (PastaThrallData thrall : KoLCharacter.pastaThralls) {
      if (thrall.equals(PastaThrallData.NO_THRALL)) {
        continue;
      }

      output.append("<tr>");
      output.append("<td rowspan=3><p>");
      output.append(thrall.getType());
      output.append("</p></td>");
      output.append("<td align=center><p>");
      output.append(thrall.getName());
      output.append("</p></td>");
      output.append("<td><p>");
      output.append(thrall.getLevel1Ability());
      output.append("</p></td>");
      output.append("</tr>");

      output.append("<tr>");
      output.append("<td align=center><p>");
      output.append(thrall.getLevel());
      output.append("</p></td>");
      output.append("<td><p>");
      output.append(thrall.getLevel5Ability());
      output.append("</p></td>");
      output.append("</tr>");

      output.append("<tr>");
      output.append("<td align=center><p>");
      output.append(thrall.getCurrentModifiers());
      output.append("</p></td>");
      output.append("<td><p>");
      output.append(thrall.getLevel10Ability());
      output.append("</p></td>");
      output.append("</tr>");
    }

    output.append("</table>");

    RequestLogger.printLine(output.toString());
    RequestLogger.printLine();

    output.setLength(0);
    PastaThrallData current = KoLCharacter.currentPastaThrall();
    if (current == PastaThrallData.NO_THRALL) {
      output.append("You do not currently have a bound pasta thrall");
    } else {
      output.append("You currently have a bound ");
      output.append(current.getType());
      String name = current.getName();
      if (!name.equals("")) {
        output.append(" named ");
        output.append(name);
      }
    }

    RequestLogger.printLine(output.toString());
    RequestLogger.printLine();
  }
}
