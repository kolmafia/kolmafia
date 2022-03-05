package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.session.GreyYouManager;
import net.sourceforge.kolmafia.session.GreyYouManager.GooSkill;

public class GooSkillsCommand extends AbstractCommand {

  public GooSkillsCommand() {
    this.usage =
        " [all | needed] [id | name | monster | type | zone] - Grey You skills, either all (default) or 'needed' (not yet unlocked).";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    // gooskills - List all Grey You skills
    // gooskills needed - List only not-yet-known skills
    //
    // The skills are ordered by skill ID.
    // You can specify a sorting order: skill id, skill name, monster name, zone "level", etc.

    String[] params = parameters.trim().split("\\s+");

    boolean all = true;
    String order = "type";

    for (String keyword : params) {
      switch (keyword) {
        case "all":
          all = true;
          break;
        case "needed":
          all = false;
          break;
        case "id":
        case "name":
        case "monster":
        case "type":
        case "zone":
          order = keyword;
          break;
        case "":
          break;
        default:
          KoLmafia.updateDisplay(MafiaState.ERROR, "Use command gooskills " + this.usage);
          return;
      }
    }

    GooSkill[] skills = GreyYouManager.sortGooSkills(order);

    StringBuilder output = new StringBuilder();

    output.append("<table border=2 cols=3>");
    output.append("<tr>");
    output.append("<th rowspan=2>Name</th>");
    output.append("<th>Type</th>");
    output.append("<th>Source</th>");
    output.append("</tr>");

    output.append("<tr>");
    output.append("<th>Known</th>");
    output.append("<th>Effect</th>");
    output.append("</tr>");

    for (GooSkill skill : skills) {
      boolean known = skill.haveAbsorbed();
      if (!all && known) {
        continue;
      }

      output.append("<tr>");
      output.append("<td rowspan=2>");
      output.append(skill.getName());
      output.append("</td>");
      output.append("<td>");
      output.append(skill.getSkillTypeName());
      if (skill.getSkillType() != SkillDatabase.PASSIVE) {
        output.append(" (");
        output.append(String.valueOf(skill.getMPCost()));
        output.append(" MP)");
      }
      output.append("</td>");
      output.append("<td>");
      output.append(skill.getMonsterName());
      if (!skill.getMonsterZone().equals("")) {
        output.append(" (");
        output.append(skill.getMonsterZone());
        output.append(")");
      }
      output.append("</td>");
      output.append("</tr>");

      output.append("<tr>");
      output.append("<td>");
      output.append(known ? "yes" : "no");
      output.append("</td>");
      output.append("<td>");
      output.append(skill.getEvaluatedEnchantments());
      output.append("</td>");
      output.append("</tr>");
    }

    output.append("</table>");

    RequestLogger.printLine(output.toString());
    RequestLogger.printLine();
  }
}
