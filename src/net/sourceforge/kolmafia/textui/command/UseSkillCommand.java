package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class UseSkillCommand extends AbstractCommand {
  public UseSkillCommand() {
    this.usage = "[?] [ [<count>] <skill> [^ effect] [on <player>] ] - list spells, or use one.";
  }

  @Override
  public void run(final String command, final String parameters) {
    if (parameters.length() > 0) {
      try (Checkpoint checkpoint = new Checkpoint()) {
        UseSkillCommand.cast(parameters);
      }
      return;
    }
    ShowDataCommand.show("skills" + (command.equals("cast") ? " cast" : ""));
  }

  /**
   * For some very commonly used skills, we can safely expand certain shorthand forms for better
   * ease of use. For example "cast ode" is safely expanded to "cast the ode to booze" rather than
   * failing because it clashes the "CHEAT CODE: " skills.
   *
   * @param skillNameString Skill name provided as a parameter
   * @return An expanded skill name if the parameter is a recognized shorthand, else the original
   *     parameter
   */
  private static String expandRecognizedShorthand(final String skillNameString) {
    return switch (skillNameString) {
      case "ode" -> "The Ode to Booze";
      default -> skillNameString;
    };
  }

  private static void cast(final String parameters) {
    UseSkillCommand.cast(parameters, false);
  }

  public static boolean cast(final String parameters, boolean sim) {
    String[] buffs = parameters.split("\\s*,\\s*");

    for (String buff : buffs) {
      String skill, effect = null, player = null;
      String[] splitParameters = buff.replaceFirst(" [oO][nN] ", " => ").split(" => ");
      if (splitParameters.length > 1) {
        player = splitParameters[1];
      }
      var skillEffect = splitParameters[0].split(" \\^ ");
      if (skillEffect.length > 1) {
        effect = skillEffect[1];
      }
      skill = skillEffect[0];

      String[] buffParameters = AbstractCommand.splitCountAndName(skill);
      String buffCountString = buffParameters[0];
      String skillNameString = buffParameters[1];

      String skillName =
          SkillDatabase.getUsableKnownSkillName(expandRecognizedShorthand(skillNameString));
      if (skillName == null) {
        if (sim) {
          return false;
        }
        KoLmafia.updateDisplay(
            MafiaState.ERROR,
            "Could not find a known, usable skill of yours uniquely matching \""
                + parameters
                + "\"");
        return false;
      }

      int buffCount = 1;

      if (buffCountString != null && buffCountString.equals("*")) {
        buffCount = 0;
      } else if (buffCountString != null) {
        buffCount = StringUtilities.parseInt(buffCountString);
      }

      if (KoLmafiaCLI.isExecutingCheckOnlyCommand) {
        RequestLogger.printLine(skillName + " (x" + buffCount + ")");
        return true;
      }

      var desiredEffect = -1;
      if (effect != null) {
        desiredEffect = EffectDatabase.getEffectId(effect);
      }
      var skillId = SkillDatabase.getSkillId(skillName);

      UseSkillRequest request =
          UseSkillRequest.getInstance(skillId, player, buffCount, desiredEffect);

      if (sim) {
        return request.getMaximumCast() > 0;
      }

      RequestThread.postRequest(request);
    }
    return true;
  }
}
