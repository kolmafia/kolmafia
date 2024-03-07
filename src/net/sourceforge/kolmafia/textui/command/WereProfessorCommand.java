package net.sourceforge.kolmafia.textui.command;

import java.util.Set;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ResearchBenchRequest;
import net.sourceforge.kolmafia.request.ResearchBenchRequest.Research;

public class WereProfessorCommand extends AbstractCommand {
  public WereProfessorCommand() {
    this.usage = " research [skill]";
  }

  @Override
  public void run(final String cmd, String parameters) {
    String[] split = parameters.split(" +");
    String command = split[0];

    if (command.equals("research")) {
      researchCommand(split);
      return;
    }

    KoLmafia.updateDisplay(MafiaState.ERROR, "Do what?");
  }

  private void researchCommand(String[] split) {
    if (split.length == 1) {
      printSkillTrees(false);
      return;
    }

    if (split.length > 1) {
      String option = split[1];

      if (option.equals("verbose")) {
        printSkillTrees(true);
        return;
      }

      Research research = ResearchBenchRequest.findResearch(option);
      if (research == null) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "'" + option + "' is not known research");
        return;
      }

      if (!KoLCharacter.inWereProfessor()) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "Only WereProfessors can use their Research Bench.");
        return;
      }

      if (KoLCharacter.isSavageBeast()) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "You are locked out of your Humble Cottage.");
        return;
      }

      // Visit your Research Bench to update available research and rp.
      new ResearchBenchRequest().run();

      // Load known and available skills and Research Points from properties
      Set<Research> known = ResearchBenchRequest.loadResearch("beastSkillsKnown");
      Set<Research> available = ResearchBenchRequest.loadResearch("beastSkillsAvailable");
      int rp = Preferences.getInteger("wereProfessorResearchPoints");

      if (known.contains(research)) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "You've already researched '" + research.field() + "'.");
        return;
      }

      if (!available.contains(research)) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "'" + research.field() + "' is not currently available to research.");
        return;
      }

      int cost = research.cost();
      if (rp < cost) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR,
            "'" + research.field() + "' requires " + cost + " rp, but you only have " + rp + ".");
        return;
      }

      new ResearchBenchRequest(research.field()).run();
    }
  }

  record Row(String field, int prefix, int data, int suffix) {}

  private static Row[] rows = {
    new Row("mus1", 0, 3, 3),
    new Row("rend1", 1, 4, 1),
    new Row("hp1", 1, 3, 2),
    new Row("skin1", 2, 4, 0),
    new Row("stomach1", 2, 4, 0),
    new Row("myst1", 0, 3, 3),
    new Row("bite1", 1, 4, 1),
    new Row("res1", 1, 3, 2),
    new Row("items1", 2, 4, 0),
    new Row("ml1", 2, 4, 0),
    new Row("mox1", 0, 3, 3),
    new Row("kick1", 1, 4, 1),
    new Row("init1", 1, 3, 2),
    new Row("meat1", 2, 4, 0),
    new Row("liver1", 2, 4, 0)
  };

  private void printSkillTrees(boolean verbose) {
    // If we are not a WereProfessor, simply print the skill trees
    if (!KoLCharacter.inWereProfessor()) {
      // If we are not a WereProfessor, just dump the skill trees
      dumpSkills(verbose);
      RequestLogger.printLine();
      return;
    }

    // If we are a Mild-Mannered Professor, visit the Research Bench to
    // update available skills and available rp.
    if (KoLCharacter.isMildManneredProfessor()) {
      new ResearchBenchRequest().run();
    }

    // Load known and available skills and Research Points from properties
    Set<Research> known = ResearchBenchRequest.loadResearch("beastSkillsKnown");
    Set<Research> available = ResearchBenchRequest.loadResearch("beastSkillsAvailable");
    int rp = Preferences.getInteger("wereProfessorResearchPoints");

    // Dump the skill trees and annotate with known/available
    dumpSkills(known, available, rp, verbose);
    // Print available rp
    RequestLogger.printLine("You have " + rp + " Research Points available.");
    RequestLogger.printLine();
  }

  private void dumpSkills(final boolean verbose) {
    dumpSkills(null, null, 0, verbose);
  }

  private void dumpSkills(Set<Research> known, Set<Research> available, int rp, boolean verbose) {
    boolean annotate = known != null;
    StringBuilder output = new StringBuilder();
    output.append("<table border=2 cols=6>");

    // The Set is sorted
    Research[] allResearch = ResearchBenchRequest.allResearch().toArray(new Research[0]);

    int researchIndex = 0;
    for (Row row : rows) {
      output.append("<tr>");

      int prefix = row.prefix();
      if (prefix > 0) {
        output.append("<td colspan=");
        output.append(String.valueOf(prefix));
        output.append(">&nbsp;</td>");
      }

      for (int i = 0; i < row.data(); ++i) {
        Research research = allResearch[researchIndex++];
        output.append("<td>");
        if (annotate) {
          if (known.contains(research)) {
            output.append("<span style=\"color:black font-weight:bold\">");
          } else if (available.contains(research)) {
            output.append("<span style=\"color:red\">");
          } else {
            output.append("<span style=\"color:gray\">");
          }
        }
        output.append(research.field());
        if (!annotate || !known.contains(research)) {
          output.append(" (");
          output.append(String.valueOf(research.cost()));
          output.append(" rp)");
        }
        if (annotate) {
          output.append("</span>");
        }
        if (verbose) {
          output.append("<div>");
          output.append(research.effect());
          output.append("</div>");
        }
        output.append("</td>");
      }

      int suffix = row.suffix();
      if (suffix > 0) {
        output.append("<td colspan=");
        output.append(String.valueOf(suffix));
        output.append(">&nbsp;</td>");
      }

      output.append("</tr>");
    }

    output.append("</table>");

    RequestLogger.printHtml(output.toString());
  }
}
