package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.session.JourneyManager;

public class JourneyCommand extends AbstractCommand {

  public JourneyCommand() {
    this.usage =
        " zones [SC | TT | PA | SA | AT | DB]| find [all | SC | TT | PA | SA | AT | DB] <skill> - Journeyman skill utility.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    // If you are currently a Journeyman:
    //
    // journey zones - show skill table for current class, indicating skills you already have
    // learned
    // journey find <skill> - show where to find the specified skill
    //
    // Whether or not you are a Journeyman:
    //
    // journey zones <class> - show skill table for specified class
    // journey find <class> <skill> - show where to find specified skill
    // journey find all <skill> - show where to find skill for all classes

    String[] params = parameters.trim().split("\\s+");
    if (params.length < 1) {
      RequestLogger.printLine("Usage: journey" + this.usage);
      return;
    }

    String subcmd = params[0];

    if (subcmd.equals("zones")) {
      zonesCommand(params);
      return;
    }

    if (subcmd.equals("find")) {
      findCommand(params);
      return;
    }

    RequestLogger.printLine("Usage: journey" + this.usage);
  }

  private AscensionClass parseClass(String[] params) {
    switch (params[1].toLowerCase().trim()) {
      case "sc":
        return AscensionClass.SEAL_CLUBBER;
      case "tt":
        return AscensionClass.TURTLE_TAMER;
      case "pa":
        return AscensionClass.PASTAMANCER;
      case "sa":
        return AscensionClass.SAUCEROR;
      case "db":
        return AscensionClass.DISCO_BANDIT;
      case "at":
        return AscensionClass.ACCORDION_THIEF;
    }

    return null;
  }

  private boolean unreachableZone(KoLAdventure zone) {
    switch (zone.getAdventureName()) {
      case "Thugnderdome":
        return !KoLCharacter.inMoxieSign();
      case "The Spooky Gravy Burrow":
        return !KoLCharacter.inMuscleSign();
      case "Outskirts of Camp Logging Camp":
      case "Camp Logging Camp":
        return !KoLCharacter.inMysticalitySign();
    }
    return false;
  }

  private void zonesCommand(String[] params) {
    // Parse command
    boolean me = false;
    AscensionClass aclass = null;

    if (params.length < 2) {
      if (KoLCharacter.getPath() != Path.JOURNEYMAN) {
        RequestLogger.printLine("Specify a class: SC, TT, PA, SA, DB, AT.");
        return;
      }
      me = true;
      aclass = KoLCharacter.getAscensionClass();
    } else {
      aclass = parseClass(params);
      if (aclass == null) {
        RequestLogger.printLine("I don't know what '" + params[1] + " is.");
        return;
      }
    }

    StringBuilder output = new StringBuilder();
    output.append("<table border=2 cols=6>");
    output.append("<tr>");
    output.append("<th rowspan=2>Zone</th>");
    output.append("<th>Skill 1 (4 turns)</th>");
    output.append("<th>Skill 2 (8 turns)</th>");
    output.append("<th>Skill 3 (12 turns)</th>");
    output.append("</tr><tr>");
    output.append("<th>Skill 4 (16 turns)</th>");
    output.append("<th>Skill 5 (20 turns)</th>");
    output.append("<th>Skill 6 (24 turns)</th>");
    output.append("</tr>");

    for (KoLAdventure zone : JourneyManager.journeymanData.keySet()) {
      boolean unreachable = me && unreachableZone(zone);
      if (unreachable) {
        // Don't show zones sign doesn't have
        continue;
      }

      output.append("<tr>");

      boolean accessible = !me || zone.isAccessible();

      output.append("<td rowspan=2>");
      if (!accessible) {
        output.append("<font color=\"grey\">");
      }
      output.append(zone.getAdventureName());
      if (!accessible) {
        output.append("</font>");
      }
      output.append("</td>");

      String[] skills = JourneyManager.journeymanData.get(zone).get(aclass);
      for (int i = 0; i < 6; ++i) {
        String skillName = skills[i];
        boolean known = me && KoLCharacter.hasSkill(skillName);
        output.append("<td>");
        if (known) {
          output.append("<s>");
        }
        output.append(skillName);
        if (known) {
          output.append("</s>");
        }
        output.append("</td>");
        if (i == 2) {
          output.append("</tr><tr>");
        }
      }

      output.append("</tr>");
    }

    output.append("</table>");
    RequestLogger.printLine(output.toString());
    RequestLogger.printLine("");
  }

  private void findCommand(String[] params) {
    boolean me = false;
    boolean all = false;
    AscensionClass aclass = null;
  }
}
