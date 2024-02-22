package net.sourceforge.kolmafia.textui.command;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.ZodiacZone;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.session.JourneyManager;

public class JourneyCommand extends AbstractCommand {

  public JourneyCommand() {
    this.usage =
        " zones [SC | TT | PM | S | AT | DB]| find [all | SC | TT | PM | S | AT | DB] <skill> - Journeyman skill utility.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    // If you are currently a Journeyman:
    //
    // journey zones - show skill table for current class, indicating already learned skills
    // journey find <skill> - show where to find the specified skill
    //
    // Whether or not you are a Journeyman:
    //
    // journey zones <class> - show skill table for specified class
    // journey find <class> <skill> - show where to find specified skill
    // journey find all <skill> - show where to find skill for all classes

    String[] params = parameters.trim().split("\\s+");
    if (params.length < 1 || params[0].equals("")) {
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
    return switch (params[1].toLowerCase().trim()) {
      case "sc" -> AscensionClass.SEAL_CLUBBER;
      case "tt" -> AscensionClass.TURTLE_TAMER;
      case "pm" -> AscensionClass.PASTAMANCER;
      case "s" -> AscensionClass.SAUCEROR;
      case "db" -> AscensionClass.DISCO_BANDIT;
      case "at" -> AscensionClass.ACCORDION_THIEF;
      default -> null;
    };
  }

  private boolean unreachableZone(KoLAdventure zone) {
    return switch (zone.getZone()) {
      case "MoxSign" -> KoLCharacter.getSignZone() != ZodiacZone.GNOMADS;
      case "MusSign" -> KoLCharacter.getSignZone() != ZodiacZone.KNOLL;
      case "Little Canadia" -> KoLCharacter.getSignZone() != ZodiacZone.CANADIA;
      default -> false;
    };
  }

  private void zonesCommand(String[] params) {
    // Parse command
    boolean me = false;
    AscensionClass aclass = null;

    if (params.length < 2) {
      if (KoLCharacter.getPath() != Path.JOURNEYMAN) {
        RequestLogger.printLine("Specify a class: SC, TT, PM, S, DB, AT.");
        return;
      }
      me = true;
      aclass = KoLCharacter.getAscensionClass();
    } else {
      aclass = parseClass(params);
      if (aclass == null) {
        RequestLogger.printLine("I don't know what '" + params[1] + "' is.");
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
        // Don't show zones zodiac sign doesn't have
        continue;
      }

      output.append("<tr>");

      boolean accessible = !me || zone.canAdventure();

      output.append("<td rowspan=2>");
      if (!accessible) {
        output.append("<s>");
      }
      output.append(zone.getAdventureName());
      if (!accessible) {
        output.append("</s>");
      }
      output.append("</td>");

      String[] skills = JourneyManager.journeymanData.get(zone).get(aclass);
      for (int i = 0; i < 6; ++i) {
        String skillName = skills[i];
        int skillId = SkillDatabase.getSkillId(skillName);
        boolean known = me && KoLCharacter.hasSkill(skillId);
        output.append("<td>");
        // output.append("<a href=\"desc_skill.php?whichskill=");
        // output.append(skillId);
        // output.append("&self=true\">");
        if (known) {
          output.append("<s>");
        }
        output.append(skillName);
        if (known) {
          output.append("</s>");
        }
        // output.append("</a>");
        output.append("</td>");
        if (i == 2) {
          output.append("</tr><tr>");
        }
      }

      output.append("</tr>");
    }

    output.append("</table>");
    RequestLogger.printHtml(output.toString());
    RequestLogger.printLine("");
  }

  private void findCommand(String[] params) {
    // Parse command
    boolean journeyman = KoLCharacter.getPath() == Path.JOURNEYMAN;
    boolean me = false;
    boolean all = false;
    AscensionClass aclass = null;

    if (params.length < 3) {
      if (!journeyman) {
        RequestLogger.printLine("Specify a class: SC, TT, PM, S, DB, AT.");
        return;
      }
      me = true;
    } else if (params[1].equals("all")) {
      all = true;
    } else {
      aclass = parseClass(params);
      if (aclass == null) {
        if (journeyman) {
          me = true;
        } else {
          RequestLogger.printLine("I don't know what '" + params[1] + "' is.");
          return;
        }
      }
    }

    if (me) {
      aclass = KoLCharacter.getAscensionClass();
    }

    // Put the words back together
    String[] skillWords = Arrays.copyOfRange(params, me ? 1 : 2, params.length);
    String skillName = Arrays.stream(skillWords).map(String::trim).collect(Collectors.joining(" "));

    // Look up the actual skill name
    int skillId = SkillDatabase.getSkillId(skillName, false);
    if (skillId == -1) {
      RequestLogger.printLine("I don't know a skill named \"" + skillName + "\"");
      return;
    }

    // Normalize the skillname
    skillName = SkillDatabase.getSkillName(skillId);

    // Look it up in JourneyManager!
    Map<AscensionClass, Integer> skills = JourneyManager.journeymanSkills.get(skillName);

    if (skills == null) {
      RequestLogger.printLine("The \"" + skillName + "\" skill is not available to Journeymen.");
      return;
    }

    if (all) {
      for (AscensionClass sclass : AscensionClass.standardClasses) {
        printSkillLocation(skillName, skills.get(sclass), sclass, me);
      }
    } else {
      printSkillLocation(skillName, skills.get(aclass), aclass, me);
    }
  }

  void printSkillLocation(String skillName, int zoneindex, AscensionClass aclass, boolean me) {
    StringBuilder output = new StringBuilder();
    if (me) {
      output.append("You");
    } else {
      output.append("A Journeyman ");
      output.append(aclass.getName());
    }
    if (me && KoLCharacter.hasSkill(skillName)) {
      output.append(" already learned \"");
    } else {
      output.append(" can learn \"");
    }
    output.append(skillName);
    output.append("\" after ");
    int index = (zoneindex % 6) + 1;
    output.append(String.valueOf(index * 4));
    output.append(" turns in ");
    int adventureId = zoneindex / 6;
    KoLAdventure zone = JourneyManager.journeymanZones.get(adventureId);
    if (zone == null) {
      // This should not be possible.
      output.append("an unknown zone.");
      RequestLogger.printLine(output.toString());
      return;
    }
    output.append(zone.getAdventureName());
    if (me) {
      if (unreachableZone(zone)) {
        output.append(" (which is permanently inaccessible to you)");
      } else if (!zone.canAdventure()) {
        output.append(" (which is not currently accessible to you)");
      }
    }
    output.append(".");
    RequestLogger.printLine(output.toString());
  }
}
