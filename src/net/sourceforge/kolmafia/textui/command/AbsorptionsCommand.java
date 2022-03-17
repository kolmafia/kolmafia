package net.sourceforge.kolmafia.textui.command;

import java.util.Map.Entry;
import java.util.Set;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.GreyYouManager;
import net.sourceforge.kolmafia.session.GreyYouManager.Absorption;
import net.sourceforge.kolmafia.session.GreyYouManager.AbsorptionType;

public class AbsorptionsCommand extends AbstractCommand {

  public AbsorptionsCommand() {
    this.usage =
        " [all | needed] [skill | advs | mus | myst | mox | maxhp | maxmp] - Grey You absorptions, either all (default) or 'needed' (not yet unlocked).";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    // absorptions - List all Grey You absorptions
    // absorptions needed - List only not-yet-absorbed
    //
    // The absorptions are ordered by zone

    String[] params = parameters.trim().split("\\s+");

    boolean all = true;
    AbsorptionType type = null;

    for (String keyword : params) {
      switch (keyword) {
        case "all":
          all = true;
          break;
        case "needed":
          all = false;
          break;
        case "skill":
        case "skills":
          type = AbsorptionType.SKILL;
          break;
        case "adventures":
        case "advs":
          type = AbsorptionType.ADVENTURES;
          break;
        case "muscle":
        case "mus":
          type = AbsorptionType.MUSCLE;
          break;
        case "mysticality":
        case "myst":
        case "mys":
          type = AbsorptionType.MYSTICALITY;
          break;
        case "moxie":
        case "mox":
          type = AbsorptionType.MOXIE;
          break;
        case "maxhp":
          type = AbsorptionType.MAX_HP;
          break;
        case "maxmp":
          type = AbsorptionType.MAX_MP;
          break;
        case "":
          break;
        default:
          KoLmafia.updateDisplay(MafiaState.ERROR, "Use command absorptions " + this.usage);
          return;
      }
    }

    StringBuilder output = new StringBuilder();
    output.append("<table border=2 cols=4>");
    output.append("<tr>");
    output.append("<th>Zone</th>");
    output.append("<th>Have</th>");
    output.append("<th>Monster</th>");
    output.append("<th>Reward</th>");
    output.append("</tr>");

    for (Entry<String, Set<Absorption>> entry : GreyYouManager.zoneAbsorptions.entrySet()) {
      String zone = entry.getKey();
      Set<Absorption> absorptions = entry.getValue();

      // Count how many absorptions in this zone
      int count = 0;
      for (Absorption absorption : absorptions) {
        boolean have = absorption.haveAbsorbed();
        if (!all && have) {
          continue;
        }
        if (type != null && type != absorption.getType()) {
          continue;
        }
        count++;
      }

      if (count == 0) {
        continue;
      }

      output.append("<tr>");
      output.append("<td rowspan=");
      output.append(count);
      output.append(">");
      output.append(zone);
      output.append("</td>");

      boolean needRow = false;
      for (Absorption absorption : absorptions) {
        boolean have = absorption.haveAbsorbed();
        if (!all && have) {
          continue;
        }
        if (type != null && type != absorption.getType()) {
          continue;
        }
        if (needRow) {
          output.append("<tr>");
        }
        output.append("<td>");
        output.append(have ? "yes" : "no");
        output.append("</td>");
        output.append("<td>");
        output.append(absorption.getMonsterName());
        output.append("</td>");
        output.append("<td>");
        output.append(absorption);
        output.append("</td>");
        output.append("</tr>");
        needRow = true;
      }
    }

    output.append("</table>");
    RequestLogger.printLine(output.toString());
    RequestLogger.printLine();
  }
}
